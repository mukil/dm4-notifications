package org.deepamehta.plugins.subscriptions;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.websockets.WebSocketConnection;
import de.deepamehta.websockets.event.WebsocketTextMessageListener;
import de.deepamehta.websockets.WebSocketsService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 *
 * A DeepaMehta 4 Plugin introducing notifications on subscribed topics based on dm4-websockets.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/dm4-subscriptions
 * @version 1.0.5-SNAPSHOT
 *
 */

@Path("/subscriptions")
public class SubscriptionsPlugin extends PluginActivator implements SubscriptionService,
                                                                    WebsocketTextMessageListener {

    private static Logger log = Logger.getLogger(SubscriptionsPlugin.class.getName());

    // These two types of information can currently be subscribed (with their special semantics)
    private static final String USER_ACCOUNT_TYPE = "dm4.accesscontrol.user_account";
    private static final String DEEPAMEHTA_TAG_TYPE = "dm4.tags.tag";

    private static final String DEFAULT_ROLE_TYPE = "dm4.core.default";

    @Inject
    private AccessControlService aclService = null;
    @Inject
    private WebSocketsService webSocketsService = null;

    

    @GET
    @Path("/subscribe/{itemId}")
    @Transactional
    public Response subscribeUser(@PathParam("itemId") long itemId) {
        // 0) Check for any session
        String logged_in_username = aclService.getUsername();
        if (!logged_in_username.isEmpty()) {
            Topic account = getUserAccountTopic(logged_in_username);
            // 1) Users can just manage their own subscriptions
            subscribe(account.getId(), itemId);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/unsubscribe/{itemId}")
    @Transactional
    public Response unsubscribeUser(@PathParam("itemId") long itemId) {
        // 0) Check for any session
        String logged_in_username = aclService.getUsername();
        if (!logged_in_username.isEmpty()) {
            Topic account = getUserAccountTopic(logged_in_username);
            // 1) Users can just manage their own subscriptions
            unsubscribe(account.getId(), itemId);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/")
    public List<RelatedTopic> getSubscriptions() {
        // 0) Check for any session
        String logged_in_username = aclService.getUsername();
        if (logged_in_username == null || logged_in_username.isEmpty()) return null;
        Topic account = getUserAccountTopic(logged_in_username);
        // 1) Return results
        log.fine("Listing all subscriptions of user " + account.getSimpleValue());
        return account.getRelatedTopics(SUBSCRIPTION_EDGE_TYPE);
    }

    @GET
    @Path("/notification")
    public List<RelatedTopic> getNotificationsForUser() {
        return getNotifications();
    }

    @GET
    @Path("/notification/unseen")
    public ArrayList<RelatedTopic> getUnseenNotificationsForUser() {
        return getUnseenNotifications();
    }

    @GET
    @Path("/notification/seen/{newsId}")
    @Transactional
    public boolean setNotificationSeen(@PathParam("newsId") long newsId) {
        try {
            // 0) Check for any session
            String logged_in_username = aclService.getUsername();
            if (logged_in_username == null || logged_in_username.isEmpty()) {
                log.warning("Nobody logged in for whom we could set the notification as seen.");
            }
            Topic notification = dm4.getTopic(newsId).loadChildTopics();
            notification.getChildTopics().set(SEEN_TYPE, true);
            // 1) Do operation
            log.fine("Set notification " + newsId + " as seen!");
            return true;
        } catch (Exception e) {
            log.warning("Could NOT set notification " + newsId + " as seen! Caused by: " 
                    + e.getCause().toString() + ", " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void subscribe(long accountId, long itemId) {
        try {
            // 1) Check sanity of subscription
            Topic itemToSubscribe = dm4.getTopic(itemId);
            if (!itemToSubscribe.getTypeUri().equals(DEEPAMEHTA_TAG_TYPE)
                    && !itemToSubscribe.getTypeUri().equals(USER_ACCOUNT_TYPE)) {
                log.warning("Subscription are only supported for topics of type "
                        + "\"User Account\" or \"Tag\" - Skipping creation of subscription");
            }
            // 2) Create subscriptions (if not alreay existent)
            if (!associationExists(SUBSCRIPTION_EDGE_TYPE, itemId, accountId)) {
                AssociationModel model = mf.newAssociationModel(SUBSCRIPTION_EDGE_TYPE,
                    mf.newTopicRoleModel(accountId, DEFAULT_ROLE_TYPE),
                    mf.newTopicRoleModel(itemId, DEFAULT_ROLE_TYPE),
                    mf.newChildTopicsModel().addRef("org.deepamehta.subscriptions.subscription_type",
                    "org.deepamehta.subscriptions.in_app_subscription"));
                dm4.createAssociation(model);
                log.info("New subscription for user:" + accountId + " to item:" + itemId);
            } else {
                log.info("Subscription already exists between " + accountId + " and " + itemId);
            }
        } catch (Exception e) {
            log.warning("ROLLBACK!");
            log.warning("Subscription between " +accountId+ " and " +itemId+ " not created.");
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public void unsubscribe(long accountId, long itemId) {
        List<Association> assocs = dm4.getAssociations(accountId, itemId, SUBSCRIPTION_EDGE_TYPE);
        Iterator<Association> iterator = assocs.iterator();
        while (iterator.hasNext()) {
            Association assoc = iterator.next();
            dm4.deleteAssociation(assoc.getId());
        }
    }

    @Override
    @Transactional
    public void createNotifications(String title, String message, long actionAccountId, DeepaMehtaObject item) {
        if (item.getTypeUri().equals(USER_ACCOUNT_TYPE)) {
            // 1) create notifications for all direct subscribers of this user topic
            log.fine("Notifying subscribers of user account \"" + item.getSimpleValue() + "\"");
            createNotifications(title, "", actionAccountId, item);
        } else {
            // 2) create notifications for all subscribers of all tags this topic is tagged with
            if (item.getChildTopics().getTopicsOrNull(DEEPAMEHTA_TAG_TYPE) != null) {
                // 2.1) go trough all tags of this topic
                List<RelatedTopic> tags = item.getChildTopics().getTopics(DEEPAMEHTA_TAG_TYPE);
                for (RelatedTopic tag : tags) {
                    Topic tag_node = dm4.getTopic(tag.getId()).loadChildTopics();
                    log.fine("Notifying subscribers of tag \"" + tag_node.getSimpleValue() + "\"");
                    // for all subscribers of this tag
                    createNotificationTopics(title, "", actionAccountId, item, tag_node);
                }
            }
            webSocketsService.broadcast("org.deepamehta.subscriptions", "Check notifications for your logged-in user.");
        }
    }

    @Override
    public List<RelatedTopic> getNotifications() {
        String logged_in_username = aclService.getUsername();
        if (logged_in_username == null || logged_in_username.isEmpty()) return null;
        Topic account = getUserAccountTopic(logged_in_username);
        //
        List<RelatedTopic> results = account.getRelatedTopics(RECIPIENT_EDGE_TYPE,
            "dm4.core.default", "dm4.core.default", NOTIFICATION);
        log.fine("Fetching " +results.size()+ " notifications for user " + account.getSimpleValue());
        DeepaMehtaUtils.loadChildTopics(results);
        return results;
    }

    @Override
    public ArrayList<RelatedTopic> getUnseenNotifications() {
        String logged_in_username = aclService.getUsername();
        if (logged_in_username == null || logged_in_username.isEmpty()) return null;
        Topic account = getUserAccountTopic(logged_in_username);
        //
        ArrayList<RelatedTopic> unseen = new ArrayList<RelatedTopic>();
        List<RelatedTopic> results = account.getRelatedTopics(RECIPIENT_EDGE_TYPE,
            "dm4.core.default", "dm4.core.default", NOTIFICATION);
        for (RelatedTopic notification : results) {
            boolean seen_child = notification.getChildTopics().getBoolean(SEEN_TYPE);
            if (!seen_child) {
                unseen.add(notification);
            }
        }
        log.info("Fetching " +unseen.size() + " unseen notifications for user " + account.getSimpleValue());
        return unseen;
    }

    @Override
    public void websocketTextMessage(String string, WebSocketConnection wsc) {
        log.info("### Received Websocket Text Message: " + string);
    }

    // ---------------------------------------------------------------------------------------------- Private Methods

    private void createNotificationTopics(String title, String text, long accountId, DeepaMehtaObject involvedItem) {
        createNotificationTopics(title, text, accountId, involvedItem, null);
    }

    private void createNotificationTopics(String title, String text, long accountId, DeepaMehtaObject involvedItem,
            DeepaMehtaObject subscribedItem) {
        // 0) Fetch all subscribers of item X
        List<RelatedTopic> subscribers = null;
        long subscribedItemId = 0;
        if (subscribedItem != null) { // fetch subscribers of subscribedItem
            subscribers = subscribedItem.getRelatedTopics(SUBSCRIPTION_EDGE_TYPE,
                DEFAULT_ROLE_TYPE,  DEFAULT_ROLE_TYPE,  "dm4.accesscontrol.user_account");
            subscribedItemId = subscribedItem.getId();
        } else { // fetch subscribers of involvedItem
            subscribers = involvedItem.getRelatedTopics(SUBSCRIPTION_EDGE_TYPE,
                DEFAULT_ROLE_TYPE,  DEFAULT_ROLE_TYPE,  "dm4.accesscontrol.user_account");
        }
        for (RelatedTopic subscriber : subscribers) {
            if (subscriber.getId() != accountId) {
                log.fine("> subscription is valid, notifying user " + subscriber.getSimpleValue());
                // 1) Create notification instance
                ChildTopicsModel message = mf.newChildTopicsModel()
                        .put(SEEN_TYPE, false)
                        .put(TITLE_TYPE, title)
                        .put(BODY_TYPE, text)
                        .put(SUBSCRIBED_ITEM_ID_TYPE, subscribedItemId)
                        .putRef(USER_ACCOUNT_TYPE, accountId)
                        .put(INVOLVED_ITEM_ID_TYPE, involvedItem.getId());
                TopicModel model = mf.newTopicModel(NOTIFICATION, message);
                dm4.createTopic(model); // check: is system the creator?
                // 2) Hook up notification with subscriber
                AssociationModel recipient_model = mf.newAssociationModel(RECIPIENT_EDGE_TYPE,
                        model.createRoleModel(DEFAULT_ROLE_TYPE),
                        mf.newTopicRoleModel(subscriber.getId(), DEFAULT_ROLE_TYPE));
                dm4.createAssociation(recipient_model); // check: is system the creator?
            }
        }
    }

    private boolean associationExists(String edge_type, long itemId, long accountId) {
        List<Association> results = dm4.getAssociations(itemId, accountId, edge_type);
        return (results.size() > 0);
    }

    private Topic getUserAccountTopic(String username) {
        Topic user = dm4.getTopicByValue("dm4.accesscontrol.username", new SimpleValue(username));
        return user.getRelatedTopic("dm4.core.composition", "dm4.core.child",
                "dm4.core.parent", "dm4.accesscontrol.user_account");
    }

}
