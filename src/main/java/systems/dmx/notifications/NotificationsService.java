package systems.dmx.notifications;

import java.util.List;
import systems.dmx.core.DMXObject;
import systems.dmx.core.RelatedTopic;

public interface NotificationsService {

    public static final String NOTIFICATION = "dmx.notification";
    public static final String NOTIFICATION_TITLE = "dmx.notification_title";
    public static final String NOTIFICATION_BODY = "dmx.notification_body";
    public static final String INVOLVED_ITEM_ID = "dmx.involved_item_id";
    public static final String SUBSCRIBED_ITEM_ID = "dmx.subscribed_item_id";
    public static final String NOTIFICATION_SEEN = "dmx.notification_seen";
    //
    public static final String SUBSCRIPTION_EDGE = "dmx.notification_subscription_edge";
    public static final String NOTIFICATION_RECIPIENT_EDGE = "dmx.notification_recipient_edge";
    //
    public static final String SUBSCRIPTION_TYPE = "dmx.subscription_type";
    public static final String IN_APP_SUBSCRIPTION = "dmx.in_app_subscription";
    public static final String EMAIL_SUBSCRIPTION = "dmx.mail_subscription";

    /**
     * Create a subscription edge to an item.
     *
     * @param   itemId      topic-id of user account
     */
    public void subscribeInApp(long itemId);

    /**
     * Remove subscription (edge) for given user account and item.
     *
     * @param  itemId           topic-id of subscribed item
     */
    public void unsubscribe(long itemId);

    /**
     * Creates new notifications for all users with a direct or indirect (tags) subscription to the given item.
     * Notifications are created if the (given) item is either of TopicType "User Account" or of any other TopicType
     * which has the TopicType=Tag as a child-type. In the latter case, all subscribers of the _Tag_ are notified.
     *
     * @param   title           title of the notification
     * @param   message         text part of the notification
     * @param   actionAccountId topic-id of user account performing the action which leads to the notification of all others
     * @param   item            item users can have subscribed (either "User Account" or any TopicType with "Tags" as child)
     */
    public void notifySubscribers(String title, String message, long actionAccountId, DMXObject item);

    /** Gets all subscribed topics for the logged-in user. */
    public List<RelatedTopic> getSubscriptions();

    /** Gets all notifications for the logged-in user. */
    public List<RelatedTopic> getNotifications();

    /** Gets all unread notifications for the logged-in user. */
    public List<RelatedTopic> getUnseenNotifications();

}
