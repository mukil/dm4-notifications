package systems.dmx.notifications;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import systems.dmx.accesscontrol.AccessControlService;
import static systems.dmx.accesscontrol.Constants.USERNAME;
import systems.dmx.core.Assoc;
import static systems.dmx.core.Constants.*;
import systems.dmx.core.DMXObject;
import systems.dmx.core.DMXType;
import systems.dmx.core.RelatedTopic;
import systems.dmx.core.Topic;
import systems.dmx.core.model.AssocModel;
import systems.dmx.core.model.ChildTopicsModel;
import systems.dmx.core.model.TopicModel;
import systems.dmx.core.osgi.PluginActivator;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Transactional;
import systems.dmx.core.service.event.PostCreateAssoc;
import systems.dmx.core.service.event.PostCreateTopic;
import systems.dmx.core.service.event.PostUpdateTopic;
import systems.dmx.core.util.DMXUtils;
import static systems.dmx.events.Constants.EVENT;
import static systems.dmx.notes.Constants.NOTE;
import static systems.dmx.notes.Constants.NOTE_TEXT;
import static systems.dmx.notifications.NotificationsService.INVOLVED_ITEM_ID;
import static systems.dmx.notifications.NotificationsService.NOTIFICATION;
import static systems.dmx.notifications.NotificationsService.NOTIFICATION_BODY;
import static systems.dmx.notifications.NotificationsService.NOTIFICATION_RECIPIENT_EDGE;
import static systems.dmx.notifications.NotificationsService.NOTIFICATION_SEEN;
import static systems.dmx.notifications.NotificationsService.NOTIFICATION_TITLE;
import static systems.dmx.tags.Constants.TAG;
import static systems.dmx.topicmaps.Constants.TOPICMAP;
import static systems.dmx.topicmaps.Constants.TOPICMAP_CONTEXT;
import systems.dmx.workspaces.WorkspacesService;

/**
 *
 * A DMX Plugin sending notifications to clients based on topic subscription_edges and dmx-websockets.
 *
 * @author Malte Reißig
 * @version 2.x
 *
 */

@Path("/notifications")
public class NotificationsPlugin extends PluginActivator implements NotificationsService,
                                                                    PostUpdateTopic,
                                                                    PostCreateTopic,
                                                                    PostCreateAssoc {

    private static Logger log = Logger.getLogger(NotificationsPlugin.class.getName());

    private static final String NOTIFICATON_BUNDLE_URI  = "systems.dmx.notifications";

    @Inject
    private AccessControlService accesscontrol = null;
    @Inject
    private WorkspacesService workspaces = null;



    // --------------------------------------------------------------------------------------- DMX Event Listeners

    @Override
    public void postCreateTopic(Topic topic) {
        if (isAuthenticatedUser()) { // Prevents notifications created by Migrations or other Mechanics
            if (topic.getTypeUri().equals(TOPICMAP)) {
                long workspaceId = dmx.getPrivilegedAccess().getAssignedWorkspaceId(topic.getId());
                notifyWorkspaceSubscribersAboutNewTopicmap(topic, workspaceId);
            } else if (topic.getTypeUri().equals(EVENT)) {
                long workspaceId = dmx.getPrivilegedAccess().getAssignedWorkspaceId(topic.getId());
                notifyWorkspaceSubscribersAboutNewEvent(topic, workspaceId);
            } else if (topic.getTypeUri().equals(NOTE)) {
                long workspaceId = dmx.getPrivilegedAccess().getAssignedWorkspaceId(topic.getId());
                notifyWorkspaceSubscribersAboutNewNote(topic, workspaceId);
            }
            // Todo: Add notifications for User Accounts, Person, Organizations, Bookmarks creation
        }
    }

    @Override
    public void postUpdateTopic(Topic topic, TopicModel tm, TopicModel tm1) {
        if (topic.getTypeUri().equals(NOTE)) {
            log.info("Note updated " + topic.getSimpleValue() + "Model 1: " + tm + ", Model 2: " + tm1);
            notifyTopicSubscribersAboutChangeset(topic, tm, tm1);
        }
    }

    @Override
    public void postCreateAssoc(Assoc association) {
        if (isAuthenticatedUser()) { // Prevents notifications created by Migrations or other Mechanics
            if (association.getTypeUri().equals(TOPICMAP_CONTEXT)) {
                notifyTopicmapSubscribersAboutNewTopicInMap(association);
            }
        }
    }

    // Dev-Meeting:
    // - Notes, Optional Dependencies for Plugins via OSGi Standard Mechanisms

    // --------------------------------------------------------------------------------------- Web Service Resources
    
    @POST
    @Path("/subscribe/{itemId}")
    @Transactional
    public Response subscribeUser(@PathParam("itemId") long itemId) {
        if (isAuthenticatedUser()) {
            subscribeInApp(itemId);
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    @POST
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
            Topic account = accesscontrol.getUsernameTopic();
            log.fine("Listing subscriptions for user \"" + account.getSimpleValue() + "\"");
            return account.getRelatedTopics(SUBSCRIPTION_EDGE, null, null, null);
        }
        return null;
    }

    @GET
    @Path("/subscription/{itemId}")
    public String getSubscription(@PathParam("itemId") long itemId) {
        if (isAuthenticatedUser()) {
            Topic account = accesscontrol.getUsernameTopic();
            log.info("Checking subscription for user \"" + account.getSimpleValue() + "\" on item " + itemId);
            return "" + associationExists(SUBSCRIPTION_EDGE, itemId, account.getId());
        }
        return "" + false;
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

    @PUT
    @Path("/notification/seen/{newsId}")
    @Transactional
    public Response setNotificationSeen(@PathParam("newsId") long newsId) {
        if (!isAuthenticatedUser()) {
            log.warning("Nobody logged in for whom we could set the notification as seen.");
            return Response.noContent().build();
        }
        Topic notification = dmx.getTopic(newsId).loadChildTopics();
        ChildTopicsModel seen = mf.newChildTopicsModel().set(NOTIFICATION_SEEN, true);
        notification.update(seen);
        // The old call would crash since 5.x: 
        // Clarify why: notification.update(notification.getChildTopics().getModel().set(NOTIFICATION_SEEN, true));
        log.fine("Set notification " + newsId + " SEEN");
        return Response.ok(notification).build();
    }



    // --------------------------------------------------------------------------- Notifications Service Implementation

    @Override
    public void subscribeInApp(long itemId) {
        if (!isAuthenticatedUser()) {
            throw new RuntimeException("For users to manage their subscriptions they must be authenticated.");
        }
        Topic account = accesscontrol.getUsernameTopic();
        subscribeInApp(account.getId(), itemId);
    }

    @Override
    public void unsubscribe(long itemId) {
        if (!isAuthenticatedUser()) {
            throw new RuntimeException("For users to manage their subscriptions they must be authenticated.");
        }
        Topic account = accesscontrol.getUsernameTopic();
        unsubscribe(account.getId(), itemId);
    }

    @Override
    @Transactional
    public void notifySubscribers(String title, String body, long actingUsername, DMXObject involvedItem, DMXObject subscribedItem) {
        // 1) create notifications for all "direct" subscribers of this user topic
        String subscribedItemName = (subscribedItem != null) ? subscribedItem.getSimpleValue().toString() : "Unknown";
        log.info("Notifying subscribers for action involving \"" + involvedItem.getSimpleValue()
                + "\" (" + involvedItem.getType().getSimpleValue() + ", Subscription: " + subscribedItemName);
        createNotifications(title, body, actingUsername, involvedItem, subscribedItem);
        // 2) If involvedItem is tagged, create also notifications for all subscribers of these tag topics
        List<RelatedTopic> tags = null; // Defensive access check for tags on this topic type (for indirect subscriptions)
        try {
            tags = involvedItem.getChildTopics().getTopicsOrNull(TAG);
        } catch(RuntimeException rex) {
            log.fine("Skipping expected RuntimeException ... " + rex.getMessage());
        }
        if (tags != null) {
            // 2.1) for all "indirect subscribers" of this topic via tags
            for (RelatedTopic tag : tags) {
                log.info("Notifying subscribers of tag \"" + tag.getSimpleValue() + "\"");
                createNotifications(title, body, actingUsername, involvedItem, tag);
            }
        }
        // 3) Send a generic "ping" to client developers (push-to-pull) requesting a reload notifications for users
        Topic username = dmx.getTopic(actingUsername);
        JSONObject message = new JSONObject();
        try {
            message = new JSONObject()
                    .put("type", "loadUnseenNotifications")
                    .put("args", new JSONObject().put("username", username.getSimpleValue()) );
        } catch (JSONException ex) {
            Logger.getLogger(NotificationsPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        dmx.getWebSocketService().sendToAllButOrigin(message.toString());
    }

    @Override
    public List<RelatedTopic> getNotifications() {
        if (!isAuthenticatedUser()) {
            // throw new RuntimeException("For users to load notifications they must be authenticated.");
            return new ArrayList<>();
        }
        Topic account = accesscontrol.getUsernameTopic();
        //
        List<RelatedTopic> results = account.getRelatedTopics(NOTIFICATION_RECIPIENT_EDGE, DEFAULT, DEFAULT, NOTIFICATION);
        log.fine("Fetching " +results.size()+ " notifications for user " + account.getSimpleValue());
        DMXUtils.loadChildTopics(results);
        return results;
    }

    @Override
    public ArrayList<RelatedTopic> getUnseenNotifications() {
        ArrayList<RelatedTopic> unseen = new ArrayList<RelatedTopic>();
        if (!isAuthenticatedUser()) {
            // throw new RuntimeException("For users to load notifications they must be authenticated.");
            return new ArrayList<>();
        }
        Topic account = accesscontrol.getUsernameTopic();
        //
        List<RelatedTopic> results = account.getRelatedTopics(NOTIFICATION_RECIPIENT_EDGE, DEFAULT, DEFAULT, NOTIFICATION);
        for (RelatedTopic notification : results) {
            boolean seen_child = notification.getChildTopics().getBoolean(NOTIFICATION_SEEN);
            if (!seen_child) {
                notification.loadChildTopics();
                unseen.add(notification);
            }
        }
        log.info("Fetching " +unseen.size() + " unseen notifications for user " + account.getSimpleValue());
        return unseen;
    }



    // ---------------------------------------------------------------------------------------------- Private Methods

    @Transactional
    private void subscribeInApp(long accountId, long itemId) {
        try {
            // 2) Create an "In App" subscription (if not already existent)
            if (!associationExists(SUBSCRIPTION_EDGE, itemId, accountId)) {
                AssocModel model = mf.newAssocModel(SUBSCRIPTION_EDGE,
                    mf.newTopicPlayerModel(accountId, DEFAULT),
                    mf.newTopicPlayerModel(itemId, DEFAULT),
                    mf.newChildTopicsModel().addRef(SUBSCRIPTION_TYPE, IN_APP_SUBSCRIPTION));
                dmx.createAssoc(model);
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
        List<Assoc> assocs = dmx.getAssocs(accountId, itemId, SUBSCRIPTION_EDGE);
        Iterator<Assoc> iterator = assocs.iterator();
        while (iterator.hasNext()) {
            Assoc assoc = iterator.next();
            dmx.deleteAssoc(assoc.getId());
        }
    }
    
    private void notifyTopicSubscribersAboutChangeset(Topic topicUpdated, TopicModel tm1, TopicModel tm2) {
        Topic actingUsername = accesscontrol.getUsernameTopic();
        // The following limitation is SAFE as long (see Line 282) as we just support subscriptions on "Note" topics
        notifySubscribers(actingUsername.getSimpleValue() + " edited "
                + topicUpdated.getType().getSimpleValue() + " \"" + topicUpdated.getSimpleValue(), "\""+actingUsername.getSimpleValue() +"\" edited "
                + topicUpdated.getType().getSimpleValue() + " \"" + topicUpdated.getSimpleValue(),
            actingUsername.getId(), topicUpdated, null);
    }

    private void notifyWorkspaceSubscribersAboutNewEvent(Topic event, long workspaceId) {
        Topic actingUsername = accesscontrol.getUsernameTopic();
        if (event.getTypeUri().equals(EVENT) && workspaceId != -1) {
            // workspace may be "-1" if someone just created it, and therewith implicitly an "untitled" topicmap gets created
            // which brings us here, if the workspace in which the new workspace is created is watched by someone...
            Topic workspace = dmx.getTopic(workspaceId);
            log.info("Notifying subscribers about new event created by \"" + actingUsername.getSimpleValue()
                    + "\" in workspace \"" + workspace.getSimpleValue() + "\"");
            // String mapName = topicmap.getChildTopics().getString(TOPICMAP_NAME);
            notifySubscribers(actingUsername.getSimpleValue() + " created the event \"" + event.getSimpleValue() + "\" in Workspace \""
                    + workspace.getSimpleValue() +"\"", actingUsername.getSimpleValue() + " created the event \"" + event.getSimpleValue() + "\" in workspace \""+ workspace.getSimpleValue() +"\"",
                    actingUsername.getId(), event, workspace);
        }
    }

    private void notifyWorkspaceSubscribersAboutNewNote(Topic note, long workspaceId) {
        Topic actingUsername = accesscontrol.getUsernameTopic();
        if (note.getTypeUri().equals(NOTE) && workspaceId != -1) {
            // workspace may be "-1" if someone just created it, and therewith implicitly an "untitled" topicmap gets created
            // which brings us here, if the workspace in which the new workspace is created is watched by someone...
            Topic workspace = dmx.getTopic(workspaceId);
            log.info("Notifying subscribers about new note created by \"" + actingUsername.getSimpleValue()
                    + "\" in workspace \"" + workspace.getSimpleValue() + "\"");
            // String mapName = topicmap.getChildTopics().getString(TOPICMAP_NAME);
            notifySubscribers(actingUsername.getSimpleValue() +" noted \"" + note.getSimpleValue() + "\" in workspace \"" + workspace.getSimpleValue() +"\"",
                    actingUsername.getSimpleValue() +" noted \"" + note.getSimpleValue() + "\" in workspace \""+ workspace.getSimpleValue() +"\"",
                    actingUsername.getId(), note, workspace);
        }
    }

    private void notifyWorkspaceSubscribersAboutNewTopicmap(Topic topicmap, long workspaceId) {
        Topic actingUsername = accesscontrol.getUsernameTopic();
        if (topicmap.getTypeUri().equals(TOPICMAP) && workspaceId != -1) {
            // workspace may be "-1" if someone just created it, and therewith implicitly an "untitled" topicmap gets created
            // which brings us here, if the workspace in which the new workspace is created is watched by someone...
            Topic workspace = dmx.getTopic(workspaceId);
            // Fixme: no "Topicmap Name" available here yet (simpleValue is empty, children not associated)
            topicmap.loadChildTopics();
            // String mapName = topicmap.getChildTopics().getString(TOPICMAP_NAME);
            notifySubscribers(actingUsername.getSimpleValue() + " created topicmap \"" + topicmap.getSimpleValue() + "\" "
                            + "in workspace \""+ workspace.getSimpleValue() +"\"",
                    actingUsername.getSimpleValue() + " created topicmap \"" + topicmap.getSimpleValue() + "\" "
                            + "in workspace \""+ workspace.getSimpleValue() +"\"",
                    actingUsername.getId(), topicmap, workspace);
        }
    }

    private void notifyTopicmapSubscribersAboutNewTopicInMap(Assoc association) {
        Topic actingUser = accesscontrol.getUsernameTopic();
        DMXObject topic = null;
        DMXObject topicmap = null;
        if (association.getDMXObject1().getTypeUri().equals(TOPICMAP)) {
            topic = association.getDMXObject2();
            topicmap = association.getDMXObject1();
        } else {
            topic = association.getDMXObject1();
            topicmap = association.getDMXObject2();
        }
        DMXType type = topic.getType();
        if (!topic.getTypeUri().equals(NOTIFICATION)) {
            notifySubscribers(type.getSimpleValue() + " added to Topicmap \"" + topicmap.getSimpleValue() + "\"",
                actingUser.getId() + " added " + type.getSimpleValue() + " \"" + ((topic.getSimpleValue().toString().isEmpty()) ? "..." : topic.getSimpleValue())
                        + "\" to Topicmap \""+ topicmap.getSimpleValue() +"\"", actingUser.getId(), topicmap, null);
        }
    }

    private boolean isAuthenticatedUser() {
        String username = accesscontrol.getUsername();
        return (username != null && !username.isEmpty());
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
            DMXObject involvedItem, DMXObject subscribedItem) {
        // 0) Fetch all subscribers of item X
        List<RelatedTopic> subscribers = null;
        // 1) Handle indirect subscriptions (where subscribedItem != involvedItem)
        if (subscribedItem != null) { // fetch subscribers of subscribedItem
            subscribers = subscribedItem.getRelatedTopics(SUBSCRIPTION_EDGE,
                DEFAULT, DEFAULT, USERNAME);
        } else { // 2) Handle direct subscriptions to involvedItem
            subscribers = involvedItem.getRelatedTopics(SUBSCRIPTION_EDGE,
                DEFAULT, DEFAULT, USERNAME);
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
            final long actingUsername, final DMXObject involvedItem, final DMXObject subscribedItem) {
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
            final long actingUsername, final DMXObject involvedItem, final DMXObject subscribedItem) {
        try {
            dmx.getPrivilegedAccess().runWithoutWorkspaceAssignment(new Callable<Topic>() {
                @Override
                public Topic call() {
                    // 1) Create instance of notification
                    ChildTopicsModel message = mf.newChildTopicsModel()
                            .set(NOTIFICATION_SEEN, false)
                            .set(NOTIFICATION_TITLE, title)
                            .set(NOTIFICATION_BODY, text)
                            .setRef(USERNAME, actingUsername)
                            .set(INVOLVED_ITEM_ID, involvedItem.getId());
                    if (subscribedItem != null) message.set(SUBSCRIBED_ITEM_ID, subscribedItem.getId());
                    TopicModel model = mf.newTopicModel(NOTIFICATION, message);
                    Topic notification = dmx.createTopic(model);
                    Topic privateWorkspace = dmx.getPrivilegedAccess()
                            .getPrivateWorkspace(subscriber.getSimpleValue().toString());
                    dmx.getPrivilegedAccess().assignToWorkspace(notification, privateWorkspace.getId());
                    dmx.getPrivilegedAccess().assignToWorkspace(notification.getChildTopics()
                            .getTopic(NOTIFICATION_TITLE), privateWorkspace.getId());
                    dmx.getPrivilegedAccess().assignToWorkspace(notification.getChildTopics()
                            .getTopic(NOTIFICATION_BODY), privateWorkspace.getId());
                    dmx.getPrivilegedAccess().assignToWorkspace(notification.getChildTopics()
                            .getTopic(NOTIFICATION_SEEN), privateWorkspace.getId());
                    dmx.getPrivilegedAccess().assignToWorkspace(notification.getChildTopics()
                            .getTopic(INVOLVED_ITEM_ID), privateWorkspace.getId());
                    if (subscribedItem != null) {
                        dmx.getPrivilegedAccess().assignToWorkspace(notification.getChildTopics()
                                .getTopic(SUBSCRIBED_ITEM_ID), privateWorkspace.getId());
                    }
                    // Improvement: Try using topicmaps.setViewProperties()... (not having a topicmap) to colorize..
                    // 2) Hook up notification with subscriber
                    AssocModel recipientModel = mf.newAssocModel(NOTIFICATION_RECIPIENT_EDGE,
                            model.createPlayerModel(DEFAULT),
                            mf.newTopicPlayerModel(subscriber.getId(), DEFAULT));
                    Assoc recipient = dmx.createAssoc(recipientModel);
                    dmx.getPrivilegedAccess().assignToWorkspace(recipient, privateWorkspace.getId());
                    return notification;
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException("Creating notification for user "+subscriber.getSimpleValue()+" failed", ex);
        }
    }

    private boolean associationExists(String edge_type, long itemId, long accountId) {
        List<Assoc> results = dmx.getAssocs(itemId, accountId, edge_type);
        return (results.size() > 0);
    }

}
