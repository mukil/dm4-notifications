package org.deepamehta.plugins.subscriptions.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;

public interface SubscriptionService extends PluginService {

    /** Create a subscription (edge) for {useraccount} when something happens with {item} */
    public void subscribe(long accountId, long itemId, ClientState clientState);

    /** Remove subscription (edge) for {useraccount} and {item} */
    public void unsubscribe(long accountId, long itemId);

    /** Send a notification to all involved users that something has happened to {item} */
    public void notify(String title, String message, long actionAccountId, DeepaMehtaObject item);

    public ResultList<RelatedTopic> getSubscriptions();

    public void receiveAllNotifications();

    public void receiveNewNotifications();

}
