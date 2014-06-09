package org.deepamehta.plugins.subscriptions.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;
import java.util.ArrayList;

public interface SubscriptionService extends PluginService {

    /**
     * Create a subscription edge for useraccount to item.
     *
     * @param   accountId   topic-id of user account
     * @param   itemId      topic-id of user account
     */
    public void subscribe(long accountId, long itemId, ClientState clientState);

    /**
     * Remove subscription (edge) for given user account and item.
     *
     * @param  userAccount  topic-id of user account
     * @param  item         topic-id of subscribed item
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
    public ResultList<RelatedTopic> getSubscriptions();

    /** Gets all notifications for the logged-in user. */
    public ResultList<RelatedTopic> getAllNotifications();

    /** Gets all unread notifications for the logged-in user. */
    public ArrayList<RelatedTopic> getAllUnseenNotifications();

}
