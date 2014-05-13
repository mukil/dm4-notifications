package org.deepamehta.plugins.subscriptions.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;
import java.util.ArrayList;

public interface SubscriptionService extends PluginService {

    /** Create a subscription (edge) for {useraccount} when something happens with {item} */
    public void subscribe(long accountId, long itemId, ClientState clientState);

    /** Remove subscription (edge) for {useraccount} and {item} */
    public void unsubscribe(long accountId, long itemId);

    /** Create a notification for all subscribed users that something has happened to their {item} */
    public void notify(String title, String message, long actionAccountId, DeepaMehtaObject item);

    /** Gets all subscribed topics for the logged-in user. */
    public ResultList<RelatedTopic> getSubscriptions();

    /** Gets all notifications for the logged-in user. */
    public ResultList<RelatedTopic> getAllNotifications();

    /** Gets all unread notifications for the logged-in user. */
    public ArrayList<RelatedTopic> getAllUnseenNotifications();

}
