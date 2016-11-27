package org.deepamehta.notifications;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.accesscontrol.AccessControlService;
import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaType;
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
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import static org.deepamehta.notifications.NotificationsService.INVOLVED_ITEM_ID;
import static org.deepamehta.notifications.NotificationsService.NOTIFICATION;
import static org.deepamehta.notifications.NotificationsService.NOTIFICATION_BODY;
import static org.deepamehta.notifications.NotificationsService.NOTIFICATION_RECIPIENT_EDGE;
import static org.deepamehta.notifications.NotificationsService.NOTIFICATION_SEEN;
import static org.deepamehta.notifications.NotificationsService.NOTIFICATION_TITLE;

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
    private static final String PRIVATE_TOPICMAP        = "dm4.topicmaps.private";
    private static final String NOTE                    = "dm4.notes.note";
    private static final String NOTE_TEXT               = "dm4.notes.text";
    private static final String USERNAME                = "dm4.accesscontrol.username";
    private static final String TAG                     = "dm4.tags.tag";

    private static final String AGGREGATION             = "dm4.core.aggregation";
    private static final String AGGREGATION_DEF         = "dm4.core.aggregation_def";
    private static final String COMPOSITION             = "dm4.core.composition";
    private static final String COMPOSITION_DEF         = "dm4.core.composition_def";

    private static final String DEFAULT_ROLE            = "dm4.core.default";
    private static final String CHILD                   = "dm4.core.child";
    private static final String PARENT                  = "dm4.core.parent";

    @Inject
    private AccessControlService aclService = null;
    @Inject
    private WebSocketsService webSocketsService = null;
    @Inject
    private WorkspacesService workspacesService = null;
    /** @Inject
    private SendgridService sendgrid = null; **/


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
        Topic account = aclService.getUsernameTopic();
        subscribeInApp(account.getId(), itemId);
    }

    @Override
    public void unsubscribe(long itemId) {
        if (!isAuthenticatedUser()) {
            throw new RuntimeException("For users to manage their subscriptions they must be authenticated.");
        }
        Topic account = aclService.getUsernameTopic();
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
        // 1) create notifications for all direct subscribers of this user topic
        log.info("Notifying subscribers for action involving \"" + involvedItem.getSimpleValue()
                + "\" (" + involvedItem.getType().getSimpleValue() + ")");
        createNotifications(title, message, actingUsername, involvedItem);
        // 2) If involvedItem is tagged, create also notifications for all subscribers of these tag topics
        List<RelatedTopic> tags = null; // Defensive access check for tags on this topic type (for indirect subscriptions)
        try {
            tags = involvedItem.getChildTopics().getTopicsOrNull(TAG);
        } catch(RuntimeException rex) {
            log.fine("Skipping expected RuntimeException ... " + rex.getMessage());
        }
        if (tags != null) {
            // 2.1) go trough all tags of this topic
            for (RelatedTopic tag : tags) {
                log.info("Notifying subscribers of tag \"" + tag.getSimpleValue() + "\"");
                // 2.2) for all subscribers of this tag
                createNotifications(title, message, actingUsername, involvedItem, tag);
            }
        }
        // 3) Notifiy plugin developers to reload notifications for users
        webSocketsService.broadcast(NOTIFICATON_BUNDLE_URI, "Please reload notifications area for the user.");
    }

    @Override
    public List<RelatedTopic> getNotifications() {
        if (!isAuthenticatedUser()) {
            throw new RuntimeException("For users to read their notifications they must be authenticated.");
        }
        Topic account = aclService.getUsernameTopic();
        //
        List<RelatedTopic> results = account.getRelatedTopics(NOTIFICATION_RECIPIENT_EDGE,
                DEFAULT_ROLE, DEFAULT_ROLE, NOTIFICATION);
        log.fine("Fetching " +results.size()+ " notifications for user " + account.getSimpleValue());
        DeepaMehtaUtils.loadChildTopics(results);
        return results;
    }

    @Override
    public ArrayList<RelatedTopic> getUnseenNotifications() {
        if (!isAuthenticatedUser()) {
            throw new RuntimeException("For users to read their notifications they must be authenticated.");
        }
        Topic account = aclService.getUsernameTopic();
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
        if (isAuthenticatedUser()) { // Prevents notifications created by Migrations or other Mechanics
            if (topic.getTypeUri().equals(TOPICMAP)) {
                long workspaceId = dm4.getAccessControl().getAssignedWorkspaceId(topic.getId());
                notifyWorkspaceSubscribersAboutNewTopicmap(topic, workspaceId);
            } else if (topic.getTypeUri().equals(NOTE)) {
                log.info("Created Note " + topic.getSimpleValue());
            }
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel tm, TopicModel tm1) {
        if (topic.getTypeUri().equals(NOTE_TEXT)) {
            log.info("Note updated " + topic.getSimpleValue() + "Model 1: " + tm + ", Model 2: " + tm1);
            notifyTopicSubscribersAboutChangeset(topic, tm, tm1);
        }
    }

    @Override
    public void postDeleteTopic(TopicModel tm) {
        // log.info("Deleted Topic " + tm);
    }

    @Override
    public void postCreateAssociation(Association association) {
        if (isAuthenticatedUser()) { // Prevents notifications created by Migrations or other Mechanics
            if (association.getTypeUri().equals(TOPICMAP_MAPCONTEXT)) {
                notifyTopicmapSubscribersAboutNewTopic(association);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------- Private Methods

    private void notifyTopicSubscribersAboutChangeset(Topic childValueTopicEdited, TopicModel tm1, TopicModel tm2) {
        Topic actingUsername = aclService.getUsernameTopic();
        List parentAssocTypesUris = new ArrayList();
        parentAssocTypesUris.add(AGGREGATION);
        parentAssocTypesUris.add(COMPOSITION);
        List<RelatedTopic> parentTopics = childValueTopicEdited.getRelatedTopics(parentAssocTypesUris,
                CHILD, PARENT, null);
        // The following limitation is SAFE as long (see Line 282) as we just support subscriptions on "Note" topics
        // TODO: Potentially many topic which subscribers should be notified. Skipping support for this, taking any.
        if (parentTopics.size() > 0) {
            RelatedTopic subscribedItem = parentTopics.get(0);
            notifySubscribers("Topic \"" + subscribedItem.getSimpleValue()
                    + "\" edited by user \""+actingUsername.getSimpleValue()+"\"", "<p>As a subscriber of topic \""
                    + subscribedItem.getSimpleValue() +"\" you receive this automatic "
                    + "notification about the update of " + childValueTopicEdited.getType().getSimpleValue()
                        + " by <em>"+actingUsername.getSimpleValue() +"</em>.</p>"
                    +"<h3>"+ subscribedItem.getType().getSimpleValue() +" Before</h3><p>"+ tm2.getSimpleValue() +"</p>"
                    +"<h3>"+ subscribedItem.getType().getSimpleValue() +" After</h3><p>"+ tm1.getSimpleValue() +"</p>",
                    actingUsername.getId(), subscribedItem);
        }
    }

    private void notifyWorkspaceSubscribersAboutNewTopicmap(Topic topic, long workspaceId) {
        Topic actingUsername = aclService.getUsernameTopic();
        if (topic.getTypeUri().equals(TOPICMAP)) {
            boolean isPrivate = topic.getChildTopics().getBoolean(PRIVATE_TOPICMAP);
            try {
                if (!isPrivate) {
                    Topic workspace = dm4.getTopic(workspaceId);
                    log.fine("Notifying subscribers about new topicmap created by \"" + actingUsername.getSimpleValue()
                            + "\" in workspace \"" + workspace.getSimpleValue() + "\"");
                    notifySubscribers("Topicmap \"" + topic.getSimpleValue() + "\" created in Workspace \""
                            + workspace.getSimpleValue() +"\"", "A new topicmap was created by \""
                            + actingUsername.getSimpleValue() +"\" in workspace \""+ workspace.getSimpleValue() +"\"",
                            actingUsername.getId(), workspace);
                }
            } catch (RuntimeException rex) {
                log.warning("Could not create notifications because user has no permission to "
                    +"access workspaceId=" + workspaceId + ", Exception:" + rex.getMessage());
            }
        }
    }

    private void notifyTopicmapSubscribersAboutNewTopic(Association association) {
        Topic actingUser = aclService.getUsernameTopic();
        DeepaMehtaObject topic = null;
        DeepaMehtaObject topicmap = null;
        if (association.getPlayer1().getTypeUri().equals(TOPICMAP)) {
            topic = association.getPlayer2();
            topicmap = association.getPlayer1();
            log.fine("Added Topic of type \""+ topic.getTypeUri()
                    + "\" to Topicmap \"" + topicmap.getSimpleValue() + "\"");
        } else {
            topic = association.getPlayer1();
            topicmap = association.getPlayer2();
            log.fine("Added Topic of type \"" + topic.getTypeUri()
                    + "\" to Topicmap \"" + topicmap.getSimpleValue() + "\"");
        }
        DeepaMehtaType type = topic.getType();
        if (!topic.getTypeUri().equals(NOTIFICATION)) {
            notifySubscribers(type.getSimpleValue() + " added to Topicmap \"" + topicmap.getSimpleValue() + "\"",
                "An entry on " + ((topic.getSimpleValue().toString().isEmpty()) ? "..." : topic.getSimpleValue())
                        + " was added to Topicmap \""+ topicmap.getSimpleValue() +"\"", actingUser.getId(), topicmap);
        }
    }

    private boolean isAuthenticatedUser() {
        String username = aclService.getUsername();
        return (username != null && !username.isEmpty());
    }

    private void createNotifications(String title, String text, long actingUsername,
            DeepaMehtaObject involvedItem) {
        createNotifications(title, text, actingUsername, involvedItem, null);
    }

    /**
     * Takes care that a notification for each subscriber gets created.
     * ### Todo: Check if subscription is of type "In App" or "Email".
     * @param title
     * @param text
     * @param actingUsername
     * @param involvedItem
     * @param subscribedItem
     */
    private void createNotifications(String title, String text, long actingUsername,
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
            // 3.1) Except for the actor herself, she does not get a notification on her action
            if (subscriber.getId() != actingUsername) {
                log.info("Identified subscription, notifying user " + subscriber.getSimpleValue());
                createNotificationTopic(subscriber, title, text, actingUsername, involvedItem, subscribedItem);
                // ### sendMailNotification(subscriber, title, text, actingUsername, involvedItem, subscribedItem);
            }
        }
    }

    /** private void sendMailNotification(final Topic subscriber, final String title, final String text,
            final long actingUsername, final DeepaMehtaObject involvedItem, final DeepaMehtaObject subscribedItem) {
        log.info("Sending mail notificatino vai sendgrid to \"" + subscriber.getSimpleValue() + "\"");
        sendgrid.doEmailUser(subscriber.getSimpleValue().toString(), title, text);
    } **/

    /**
     * Creates a notification topic in the \"Private workspace\" of the given user represented by the "subscriber"
     * topic and associates it with its "Username" using an association of type "Notification Recipient".
     * @param subscriber
     * @param title
     * @param text
     * @param actingUsername
     * @param involvedItem
     * @param subscribedItem
     */
    private void createNotificationTopic(final Topic subscriber, final String title, final String text,
            final long actingUsername, final DeepaMehtaObject involvedItem, final DeepaMehtaObject subscribedItem) {
        try {
            dm4.getAccessControl().runWithoutWorkspaceAssignment(new Callable<Topic>() {
                @Override
                public Topic call() {
                    // 1) Create instance of notification
                    ChildTopicsModel message = mf.newChildTopicsModel()
                            .put(NOTIFICATION_SEEN, false)
                            .put(NOTIFICATION_TITLE, title)
                            .put(NOTIFICATION_BODY, text)
                            .putRef(USERNAME, actingUsername)
                            .put(INVOLVED_ITEM_ID, involvedItem.getId());
                    if (subscribedItem != null) message.put(SUBSCRIBED_ITEM_ID, subscribedItem.getId());
                    TopicModel model = mf.newTopicModel(NOTIFICATION, message);
                    Topic notification = dm4.createTopic(model);
                    Topic privateWorkspace = dm4.getAccessControl()
                            .getPrivateWorkspace(subscriber.getSimpleValue().toString());
                    dm4.getAccessControl().assignToWorkspace(notification, privateWorkspace.getId());
                    dm4.getAccessControl().assignToWorkspace(notification.getChildTopics()
                            .getTopic(NOTIFICATION_TITLE), privateWorkspace.getId());
                    dm4.getAccessControl().assignToWorkspace(notification.getChildTopics()
                            .getTopic(NOTIFICATION_BODY), privateWorkspace.getId());
                    dm4.getAccessControl().assignToWorkspace(notification.getChildTopics()
                            .getTopic(NOTIFICATION_SEEN), privateWorkspace.getId());
                    dm4.getAccessControl().assignToWorkspace(notification.getChildTopics()
                            .getTopic(INVOLVED_ITEM_ID), privateWorkspace.getId());
                    if (subscribedItem != null) {
                        dm4.getAccessControl().assignToWorkspace(notification.getChildTopics()
                                .getTopic(SUBSCRIBED_ITEM_ID), privateWorkspace.getId());
                    }
                    // Improvement: Try using topicmaps.setViewProperties()... (not having a topicmap) to colorize..
                    // 2) Hook up notification with subscriber
                    AssociationModel recipientModel = mf.newAssociationModel(NOTIFICATION_RECIPIENT_EDGE,
                            model.createRoleModel(DEFAULT_ROLE),
                            mf.newTopicRoleModel(subscriber.getId(), DEFAULT_ROLE));
                    Association recipient = dm4.createAssociation(recipientModel);
                    dm4.getAccessControl().assignToWorkspace(recipient, privateWorkspace.getId());
                    return notification;
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException("Creating notification for user "+subscriber.getSimpleValue()+" failed", ex);
        }
    }

    private boolean associationExists(String edge_type, long itemId, long accountId) {
        List<Association> results = dm4.getAssociations(itemId, accountId, edge_type);
        return (results.size() > 0);
    }

}
