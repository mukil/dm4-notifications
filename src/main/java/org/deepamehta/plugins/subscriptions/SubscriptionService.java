package org.deepamehta.plugins.subscriptions;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import java.util.List;

public interface SubscriptionService {

    static final String NOTIFICATION = "org.deepamehta.subscriptions.notification";
    static final String TITLE_TYPE = "org.deepamehta.subscriptions.notification_title";
    static final String BODY_TYPE = "org.deepamehta.subscriptions.notification_body";
    static final String INVOLVED_ITEM_ID_TYPE = "org.deepamehta.subscriptions.involved_item_id";
    static final String SUBSCRIBED_ITEM_ID_TYPE = "org.deepamehta.subscriptions.subscribed_item_id";
    static final String SEEN_TYPE = "org.deepamehta.subscriptions.notification_seen";
    static final String SUBSCRIPTION_EDGE_TYPE = "org.deepamehta.subscriptions.subscription_edge";
    static final String RECIPIENT_EDGE_TYPE = "org.deepamehta.subscriptions.notification_recipient_edge";

    /**
     * Create a subscription edge for useraccount to item.
     *
     * @param   accountId   topic-id of user account
     * @param   itemId      topic-id of user account
     */
    public void subscribe(long accountId, long itemId);

    /**
     * Remove subscription (edge) for given user account and item.
     *
     * @param  accountId        topic-id of user account
     * @param  itemId           topic-id of subscribed item
     */
    public void unsubscribe(long accountId, long itemId);

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
    public void createNotifications(String title, String message, long actionAccountId, DeepaMehtaObject item);

    /** Gets all subscribed topics for the logged-in user. */
    public List<RelatedTopic> getSubscriptions();

    /** Gets all notifications for the logged-in user. */
    public List<RelatedTopic> getNotifications();

    /** Gets all unread notifications for the logged-in user. */
    public List<RelatedTopic> getUnseenNotifications();

}
