package org.deepamehta.notifications;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.core.Association;
import de.deepamehta.core.Role;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.event.PostCreateAssociationListener;
import de.deepamehta.core.service.event.PostCreateTopicListener;
import de.deepamehta.core.service.event.PostDeleteTopicListener;
import de.deepamehta.core.service.event.PostUpdateTopicListener;
import de.deepamehta.core.util.DeepaMehtaUtils;
import de.deepamehta.websockets.WebSocketConnection;
import de.deepamehta.websockets.WebSocketsService;
import de.deepamehta.websockets.event.WebsocketTextMessageListener;
import de.deepamehta.workspaces.WorkspacesService;
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
 * @author Malte Reißig
 * @version 1.1
 *
 */

@Path("/notifications")
public class NotificationsPlugin extends PluginActivator implements NotificationsService,
                                                                    WebsocketTextMessageListener,
                                                                    PostUpdateTopicListener,
                                                                    PostCreateTopicListener,
                                                                    PostDeleteTopicListener,
                                                                    PostCreateAssociationListener {

    private static Logger log = Logger.getLogger(NotificationsPlugin.class.getName());

    private static final String NOTIFICATON_BUNDLE_URI  = "org.deepamehta.notifications";

    private static final String WORKSPACE               = "dm4.workspaces.workspace";
    private static final String TOPICMAP                = "dm4.topicmaps.topicmap";
    private static final String TOPICMAP_MAPCONTEXT     = "dm4.topicmaps.topic_mapcontext";
    private static final String NOTE                    = "dm4.notes.note";
    private static final String USERNAME                = "dm4.accesscontrol.username";
    private static final String TAG                     = "dm4.tags.tag";

    private static final String AGGREGATION             = "dm4.core.aggregation";
    private static final String DEFAULT_ROLE            = "dm4.core.default";

    @Inject
    private AccessControlService aclService = null;
    @Inject
    private WebSocketsService webSocketsService = null;
    @Inject
    private WorkspacesService workspacesService = null;



    @GET
    @Path("/subscribe/{itemId}")
    @Transactional
    public Response subscribeUser(@PathParam("itemId") long itemId) {
        if (isAuthenticatedUser()) {
            subscribeInApp(itemId);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/unsubscribe/{itemId}")
    @Transactional
    public Response unsubscribeUser(@PathParam("itemId") long itemId) {
        if (isAuthenticatedUser()) {
            unsubscribe(itemId);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/subscription")
    public List<RelatedTopic> getSubscriptions() {
        if (isAuthenticatedUser()) {
            Topic account = aclService.getUsernameTopic();
            log.fine("Listing subscriptions for user \"" + account.getSimpleValue() + "\"");
            return account.getRelatedTopics(SUBSCRIPTION_EDGE);
        }
        return null;
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
    public Response setNotificationSeen(@PathParam("newsId") long newsId) {
        try {
            if (isAuthenticatedUser()) {
                log.warning("Nobody logged in for whom we could set the notification as seen.");
                return Response.ok(false).build();
            }
            Topic notification = dm4.getTopic(newsId).loadChildTopics();
            notification.getChildTopics().set(NOTIFICATION_SEEN, true);
            log.fine("Set notification " + newsId + " SEEN");
            return Response.ok(true).build();
        } catch (Exception e) {
            log.warning("Could NOT set notification " + newsId + " SEEN, Caused by: "
                    + e.getCause().toString() + ", " + e.getMessage());
            return Response.ok(false).build();
        }
    }

    @Override
    public void subscribeInApp(long itemId) {
        if (!isAuthenticatedUser()) {
            throw new RuntimeException("For users to manage their subscriptions they must be authenticated.");
        }
        Topic account = aclService.getUsernameTopic(aclService.getUsername());
        subscribeInApp(account.getId(), itemId);
    }

    @Override
    public void unsubscribe(long itemId) {
        if (!isAuthenticatedUser()) {
            throw new RuntimeException("For users to manage their subscriptions they must be authenticated.");
        }
        Topic account = aclService.getUsernameTopic(aclService.getUsername());
        unsubscribe(account.getId(), itemId);
    }

    @Transactional
    private void subscribeInApp(long accountId, long itemId) {
        try {
            // 2) Create an "In App" subscription (if not already existent)
            if (!associationExists(SUBSCRIPTION_EDGE, itemId, accountId)) {
                AssociationModel model = mf.newAssociationModel(SUBSCRIPTION_EDGE,
                    mf.newTopicRoleModel(accountId, DEFAULT_ROLE),
                    mf.newTopicRoleModel(itemId, DEFAULT_ROLE),
                    mf.newChildTopicsModel().addRef(SUBSCRIPTION_TYPE, IN_APP_SUBSCRIPTION));
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

    @Transactional
    private void unsubscribe(long accountId, long itemId) {
        List<Association> assocs = dm4.getAssociations(accountId, itemId, SUBSCRIPTION_EDGE);
        Iterator<Association> iterator = assocs.iterator();
        while (iterator.hasNext()) {
            Association assoc = iterator.next();
            dm4.deleteAssociation(assoc.getId());
        }
    }

    @Override
    @Transactional
    public void notifySubscribers(String title, String message, long actingUsername, DeepaMehtaObject involvedItem) {
        // ### Investigate: In which workspace dor should do this notifications end up?
        // ### Fixme: I guess we want to have all those created and assigned solely to the "Private Workspace"
        // ...
        // 1) create notifications for all direct subscribers of this user topic
        log.fine("Notifying subscribers of user account \"" + involvedItem.getSimpleValue() + "\"");
        createNotificationTopics(title, "", actingUsername, involvedItem);
        // 2) If involvedItem is tagged, create also notifications for all subscribers of these tag topics
        if (involvedItem.getChildTopics().getTopicsOrNull(TAG) != null) {
            // 2.1) go trough all tags of this topic
            List<RelatedTopic> tags = involvedItem.getChildTopics().getTopics(TAG);
            for (RelatedTopic tag : tags) {
                Topic tag_node = dm4.getTopic(tag.getId()).loadChildTopics();
                log.fine("Notifying subscribers of tag \"" + tag_node.getSimpleValue() + "\"");
                // 2.2) for all subscribers of this tag
                createNotificationTopics(title, "", actingUsername, involvedItem, tag_node);
            }
        }
        // 3) Notifiy plugin developers to reload notifications for users
        webSocketsService.broadcast(NOTIFICATON_BUNDLE_URI, "Please reload notifications area for the user.");
    }

    @Override
    public List<RelatedTopic> getNotifications() {
        String logged_in_username = aclService.getUsername();
        if (logged_in_username == null || logged_in_username.isEmpty()) return null;
        Topic account = aclService.getUsernameTopic(logged_in_username);
        //
        List<RelatedTopic> results = account.getRelatedTopics(NOTIFICATION_RECIPIENT_EDGE,
                DEFAULT_ROLE, DEFAULT_ROLE, NOTIFICATION);
        log.fine("Fetching " +results.size()+ " notifications for user " + account.getSimpleValue());
        DeepaMehtaUtils.loadChildTopics(results);
        return results;
    }

    @Override
    public ArrayList<RelatedTopic> getUnseenNotifications() {
        String logged_in_username = aclService.getUsername();
        if (logged_in_username == null || logged_in_username.isEmpty()) return null;
        Topic account = aclService.getUsernameTopic(logged_in_username);
        //
        ArrayList<RelatedTopic> unseen = new ArrayList<RelatedTopic>();
        List<RelatedTopic> results = account.getRelatedTopics(NOTIFICATION_RECIPIENT_EDGE,
                DEFAULT_ROLE, DEFAULT_ROLE, NOTIFICATION);
        for (RelatedTopic notification : results) {
            boolean seen_child = notification.getChildTopics().getBoolean(NOTIFICATION_SEEN);
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

    // ------------------------------------------------------------------------------------------- Listening to Hooks

    @Override
    public void postCreateTopic(Topic topic) {
        log.info("Created Topic " + topic.getSimpleValue());
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel tm, TopicModel tm1) {
        log.info("Updated Topic " + topic.getSimpleValue());
    }

    @Override
    public void postDeleteTopic(TopicModel tm) {
        log.info("Deleted Topic " + tm);
    }

    @Override
    public void postCreateAssociation(Association association) {
        if (association.getTypeUri().equals(TOPICMAP_MAPCONTEXT)) {
            DeepaMehtaObject player1 = association.getPlayer1();
            DeepaMehtaObject player2 = association.getPlayer2();
            if (player1.getTypeUri().equals(TOPICMAP)) {
                log.info("Added Topic of type \""+ player2.getTypeUri()
                        + "\" to Topicmap \"" + player1.getSimpleValue() + "\"");
            } else {
                log.info("Added Topic of type \"" + player1.getTypeUri()
                        + "\" to Topicmap \"" + player2.getSimpleValue() + "\"");
            }
        } else if (association.getTypeUri().equals(AGGREGATION)) {
            log.info("Created Association " + association);
            // Topic workspace = association.getTopic(WORKSPACE);
            // When adding a "Date of Birth" or "Phone Entry" to a "Person" entry, or creating an "Event" with a "From" and "To" topic this throws ...
            DeepaMehtaObject player1 = association.getPlayer1(); // graphdb.NotFoundException: 'value' property not found for NodeImpl#6581
            if (association.getPlayer1().getTypeUri().equals(WORKSPACE)
             || association.getPlayer2().getTypeUri().equals(WORKSPACE)) {
                DeepaMehtaObject otherElement = (association.getPlayer1().getTypeUri().equals(WORKSPACE)) ? association.getPlayer2() : association.getPlayer1();
                DeepaMehtaObject workspaceElement = (association.getPlayer1().getTypeUri().equals(WORKSPACE)) ? association.getPlayer1() : association.getPlayer2();
                log.info("Workspace Assignment of a \"" + otherElement.getTypeUri() + "\" to \"" + workspaceElement.getSimpleValue() + "\"");
            }
        }
    }

    // ---------------------------------------------------------------------------------------------- Private Methods

    private boolean isAuthenticatedUser() {
        String logged_in_username = aclService.getUsername();
        return !logged_in_username.isEmpty();
    }

    private void createNotificationTopics(String title, String text, long actingUsername,
            DeepaMehtaObject involvedItem) {
        createNotificationTopics(title, text, actingUsername, involvedItem, null);
    }

    private void createNotificationTopics(String title, String text, long actingUsername,
            DeepaMehtaObject involvedItem, DeepaMehtaObject subscribedItem) {
        // 0) Fetch all subscribers of item X
        List<RelatedTopic> subscribers = null;
        // 1) Handle indirect subscriptions (where subscribedItem != involvedItem)
        if (subscribedItem != null) { // fetch subscribers of subscribedItem
            subscribers = subscribedItem.getRelatedTopics(SUBSCRIPTION_EDGE,
                DEFAULT_ROLE, DEFAULT_ROLE, USERNAME);
        } else { // 2) Handle direct subscriptions to involvedItem
            subscribers = involvedItem.getRelatedTopics(SUBSCRIPTION_EDGE,
                DEFAULT_ROLE, DEFAULT_ROLE, USERNAME);
        }
        // 3) For all subscribers create the following notification
        for (RelatedTopic subscriber : subscribers) {
            if (subscriber.getId() != actingUsername) {
                log.fine("Identified subscription, notifying user " + subscriber.getSimpleValue());
                createNotificationTopic(subscriber, title, text, actingUsername, involvedItem, subscribedItem);
            }
        }
    }
    
    private void createNotificationTopic(Topic subscriber, String title, String text, long actingUsername,
            DeepaMehtaObject involvedItem, DeepaMehtaObject subscribedItem) {
        // ### Fixme: Take care that topic and assoc end up in the users "Private Workspace"
        // 1) Create notification instance
        ChildTopicsModel message = mf.newChildTopicsModel()
                .put(NOTIFICATION_SEEN, false)
                .put(NOTIFICATION_TITLE, title)
                .put(NOTIFICATION_BODY, text)
                .put(SUBSCRIBED_ITEM_ID, subscribedItem.getId())
                .putRef(USERNAME, actingUsername)
                .put(INVOLVED_ITEM_ID, involvedItem.getId());
        TopicModel model = mf.newTopicModel(NOTIFICATION, message);
        dm4.createTopic(model);
        // 2) Hook up notification with subscriber
        AssociationModel recipient_model = mf.newAssociationModel(NOTIFICATION_RECIPIENT_EDGE,
                model.createRoleModel(DEFAULT_ROLE),
                mf.newTopicRoleModel(subscriber.getId(), DEFAULT_ROLE));
        dm4.createAssociation(recipient_model); // check: is system the creator?
    }

    private boolean associationExists(String edge_type, long itemId, long accountId) {
        List<Association> results = dm4.getAssociations(itemId, accountId, edge_type);
        return (results.size() > 0);
    }

}
