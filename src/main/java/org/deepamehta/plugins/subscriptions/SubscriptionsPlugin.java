package org.deepamehta.plugins.subscriptions;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.websockets.event.WebsocketTextMessageListener;
import de.deepamehta.plugins.websockets.service.WebSocketsService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.deepamehta.plugins.subscriptions.service.SubscriptionService;

/**
 *
 * A DeepaMehta 4 Plugin introducing notifications on subscribed topics based on dm4-websockets.
 *
 * @author Malte Reißig (<malte@mikromedia.de>)
 * @website https://github.com/mukil/org.deepamehta-subscriptions
 * @version 1.0.0
 *
 */

@Path("/subscriptions")
public class SubscriptionsPlugin extends PluginActivator implements SubscriptionService,
                                                                    WebsocketTextMessageListener {

    private static Logger log = Logger.getLogger(SubscriptionsPlugin.class.getName());

    private static final String NOTIFICATION_TYPE = "org.deepamehta.subscriptions.notification";
    private static final String NOTIFICATION_TITLE_TYPE = "org.deepamehta.subscriptions.notification_title";
    private static final String NOTIFICATION_BODY_TYPE = "org.deepamehta.subscriptions.notification_body";
    private static final String NOTIFICATION_INVOLVED_ITEM_ID_TYPE = "org.deepamehta.subscriptions.involved_item_id";
    private static final String NOTIFICATION_SUB_ITEM_ID_TYPE = "org.deepamehta.subscriptions.subscribed_item_id";
    private static final String NOTIFICATION_RECIPIENT_EDGE_TYPE =
            "org.deepamehta.subscriptions.notification_recipient_edge";
    private static final String SUBSCRIPTION_EDGE_TYPE = "org.deepamehta.subscriptions.subscription_edge";
    private static final String NOTIFICATION_SEEN_TYPE = "org.deepamehta.subscriptions.notification_seen";

    // These two types of information can currently be subscribed (with their special semantics)
    private static final String USER_ACCOUNT_TYPE = "dm4.accesscontrol.user_account";
    private static final String DEEPAMEHTA_TAG_TYPE = "dm4.tags.tag";

    private static final String DEFAULT_ROLE_TYPE = "dm4.core.default";

    private AccessControlService aclService = null;
    private WebSocketsService webSocketsService = null;


    // --- Hook Implementations


    @Override
    @ConsumesService({
        "de.deepamehta.plugins.accesscontrol.service.AccessControlService",
        "de.deepamehta.plugins.websockets.service.WebSocketsService"
    })
    public void serviceArrived(PluginService service) {
        if (service instanceof AccessControlService) {
            aclService = (AccessControlService) service;
        } else if (service instanceof WebSocketsService) {
            webSocketsService = (WebSocketsService) service;
        }
    }

    @ConsumesService({
        "de.deepamehta.plugins.accesscontrol.service.AccessControlService",
        "de.deepamehta.plugins.websockets.service.WebSocketsService"
    })
    public void serviceGone(PluginService service) {
        if (service instanceof AccessControlService) {
            aclService = null;
        } else if (service instanceof WebSocketsService) {
            webSocketsService = null;
        }
    }

    @GET
    @Path("/subscribe/{itemId}")
    public Response subscribeUser(@PathParam("itemId") long itemId, @HeaderParam("Cookie") ClientState clientState) {
        // 0) Check for any session
        String logged_in_username = aclService.getUsername();
        if (!logged_in_username.isEmpty()) {
            Topic user = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(logged_in_username), false);
            Topic account = user.getRelatedTopic("dm4.core.composition", "dm4.core.child",
                    "dm4.core.parent", "dm4.accesscontrol.user_account", true, false);
            // 1) Users can just manage their own subscriptions
            subscribe(account.getId(), itemId, clientState);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/unsubscribe/{itemId}")
    public Response unsubscribeUser(@PathParam("itemId") long itemId) {
        // 0) Check for any session
        String logged_in_username = aclService.getUsername();
        if (!logged_in_username.isEmpty()) {
            Topic user = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(logged_in_username), false);
            Topic account = user.getRelatedTopic("dm4.core.composition", "dm4.core.child",
                    "dm4.core.parent", "dm4.accesscontrol.user_account", true, false);
            // 1) Users can just manage their own subscriptions
            unsubscribe(account.getId(), itemId);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/list")
    public ResultList<RelatedTopic> getSubscriptions() {
        // 0) Check for any session
        String logged_in_username = aclService.getUsername();
        if (logged_in_username == null || logged_in_username.isEmpty()) return null;
        Topic user = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(logged_in_username), false);
        Topic account = user.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent",
                "dm4.accesscontrol.user_account", false, false);
        // 1) Return results
        log.info("Listing all subscriptions of user " + account.getSimpleValue());
        return account.getRelatedTopics(SUBSCRIPTION_EDGE_TYPE, 0);
    }

    @GET
    @Path("/notification/seen/{newsId}")
    public void setNotificationSeen(@PathParam("newsId") long newsId) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            // 0) Check for any session
            String logged_in_username = aclService.getUsername();
            if (logged_in_username == null || logged_in_username.isEmpty()) throw new RuntimeException();
            Topic notification = dms.getTopic(newsId, true);
            notification.getCompositeValue().set(NOTIFICATION_SEEN_TYPE, true, null, new Directives());
            // 1) Do operation
            log.info("Set notification " + newsId + " as seen!");
            tx.success();
        } catch (Exception e) {
            tx.failure();
            log.info("Did NOT set notification " + newsId + " as seen!");
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            tx.finish();
        }
    }

    @GET
    @Path("/notifications/all")
    public ResultList<RelatedTopic> getAllNotificationsForUser() {
        return getAllNotifications();
    }

    @GET
    @Path("/notifications/unseen")
    public ArrayList<RelatedTopic> getAllUnseenNotificationsForUser() {
        return getAllUnseenNotifications();
    }

    @Override
    public void subscribe(long accountId, long itemId, ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            if (!associationExists(SUBSCRIPTION_EDGE_TYPE, itemId, accountId)) {
                AssociationModel model = new AssociationModel(SUBSCRIPTION_EDGE_TYPE,
                    new TopicRoleModel(accountId, DEFAULT_ROLE_TYPE),
                    new TopicRoleModel(itemId, DEFAULT_ROLE_TYPE),
                    new CompositeValueModel().addRef("org.deepamehta.subscriptions.subscription_type",
                    "org.deepamehta.subscriptions.in_app_subscription"));
                dms.createAssociation(model, clientState);
                log.info("Subscribed " + accountId + " to item " + itemId);
            } else {
                log.info("Subscription already exists between " + accountId + " and " + itemId);
            }
            tx.success();
        } catch (Exception e) {
            log.warning("Exception " + e.getMessage());
            tx.failure();
        } finally {
            tx.finish();
        }
    }

    @Override
    public void unsubscribe(long accountId, long itemId) {
        List<Association> assocs = dms.getAssociations(accountId, itemId, SUBSCRIPTION_EDGE_TYPE);
        Iterator<Association> iterator = assocs.iterator();
        while (iterator.hasNext()) {
            Association assoc = iterator.next();
            dms.deleteAssociation(assoc.getId());
        }
    }

    @Override
    public void notify(String title, String message, long actionAccountId, DeepaMehtaObject item) {

        if (item.getTypeUri().equals(USER_ACCOUNT_TYPE)) {
            // 1) create notifications for all directl subscribers of creates|edits of this user topic
            createNotifications(title, "", actionAccountId, item);
        } else {
            // 1) create notifications for all subscribers of all the tags this (created|edited) topic is tagged with
            if (item.getModel().getCompositeValueModel().has(DEEPAMEHTA_TAG_TYPE)) {
                // 2) check all tags
                List<TopicModel> tags = item.getModel().getCompositeValueModel().getTopics(DEEPAMEHTA_TAG_TYPE);
                for (TopicModel tag : tags) {
                    Topic tag_node = dms.getTopic(tag.getId(), true);
                    log.info("Notifying subscribers of tag \"" + tag_node.getSimpleValue() + "\"");
                    // for all subscribers of this tag
                    createNotifications(title, "", actionAccountId, item, tag_node);
                }
            }
            webSocketsService.broadcast("org.deepamehta.subscriptions", "Check notifications for the logged-in user.");
        }

    }

    @Override
    public ResultList<RelatedTopic> getAllNotifications() {
        String logged_in_username = aclService.getUsername();
        if (logged_in_username == null || logged_in_username.isEmpty()) return null;
        Topic user = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(logged_in_username), false);
        Topic account = user.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent",
                "dm4.accesscontrol.user_account", false, false);
        //
        ResultList<RelatedTopic> results = account.getRelatedTopics(NOTIFICATION_RECIPIENT_EDGE_TYPE, "dm4.core.default", "dm4.core.default",
                NOTIFICATION_TYPE, true, false, 0);
        log.info("Fetching " +results.getSize()+ " notifications for user " + account.getSimpleValue());
        return results;
    }

    @Override
    public ArrayList<RelatedTopic> getAllUnseenNotifications() {
        String logged_in_username = aclService.getUsername();
        if (logged_in_username == null || logged_in_username.isEmpty()) return null;
        Topic user = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(logged_in_username), false);
        Topic account = user.getRelatedTopic("dm4.core.composition", "dm4.core.child", "dm4.core.parent",
                "dm4.accesscontrol.user_account", false, false);
        //
        ArrayList<RelatedTopic> unseen = new ArrayList<RelatedTopic>();
        ResultList<RelatedTopic> results = account.getRelatedTopics(NOTIFICATION_RECIPIENT_EDGE_TYPE, "dm4.core.default", "dm4.core.default",
                NOTIFICATION_TYPE, true, false, 0);
        for (RelatedTopic notification : results.getItems()) {
            boolean seen_child = notification.getCompositeValue().getBoolean(NOTIFICATION_SEEN_TYPE);
            if (!seen_child) {
                unseen.add(notification);
            }
        }
        log.info("Fetching " +unseen.size() + " unseen notifications for user " + account.getSimpleValue());
        return unseen;
    }

    @Override
    public void websocketTextMessage(String message) {
        log.info("### Receiving message from WebSocket client: \"" + message + "\"");

    }

    private void createNotifications(String title, String text, long accountId, DeepaMehtaObject involvedItem) {
        createNotifications(title, text, accountId, involvedItem, null);
    }

    private void createNotifications(String title, String text, long accountId, DeepaMehtaObject involvedItem,
            DeepaMehtaObject subscribedItem) {
        // 0) Fetch all subscribers of item X
        ResultList<RelatedTopic> subscribers = null;
        long subscribedItemId = 0;
        if (subscribedItem != null) { // fetch subscribers of subscribedItem
            subscribers = subscribedItem.getRelatedTopics(SUBSCRIPTION_EDGE_TYPE,
                DEFAULT_ROLE_TYPE,  DEFAULT_ROLE_TYPE,  "dm4.accesscontrol.user_account", true, false, 0);
            subscribedItemId = subscribedItem.getId();
        } else { // fetch subscribers of involvedItem
            subscribers = involvedItem.getRelatedTopics(SUBSCRIPTION_EDGE_TYPE,
                DEFAULT_ROLE_TYPE,  DEFAULT_ROLE_TYPE,  "dm4.accesscontrol.user_account", true, false, 0);
        }
        for (RelatedTopic subscriber : subscribers) {
            if (subscriber.getId() != accountId) {
                log.fine("> subscription is valid, notifying user " + subscriber.getSimpleValue());
                // 1) Create notification instance
                CompositeValueModel message = new CompositeValueModel()
                        .put(NOTIFICATION_SEEN_TYPE, false)
                        .put(NOTIFICATION_TITLE_TYPE, title)
                        .put(NOTIFICATION_BODY_TYPE, text)
                        .put(NOTIFICATION_SUB_ITEM_ID_TYPE, subscribedItemId)
                        .putRef(USER_ACCOUNT_TYPE, accountId)
                        .put(NOTIFICATION_INVOLVED_ITEM_ID_TYPE, involvedItem.getId());
                TopicModel model = new TopicModel(NOTIFICATION_TYPE, message);
                dms.createTopic(model, null); // check: is system the creator?
                // 2) Hook up notification with subscriber
                AssociationModel recipient_model = new AssociationModel(NOTIFICATION_RECIPIENT_EDGE_TYPE,
                        model.createRoleModel(DEFAULT_ROLE_TYPE),
                        new TopicRoleModel(subscriber.getId(), DEFAULT_ROLE_TYPE));
                dms.createAssociation(recipient_model, null); // check: is system the creator?
            }
        }
    }

    private boolean associationExists(String edge_type, long itemId, long accountId) {
        List<Association> results = dms.getAssociations(itemId, accountId, edge_type);
        return (results.size() > 0) ? true : false;
    }


}
