package org.deepamehta.plugins.subscriptions.service;

import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;

public interface SubscriptionService extends PluginService {

    /** Create a subscription (edge) for {useraccount} when something happens with {item} */
    public void subscribe(long accountId, long itemId, ClientState clientState);

    /** Remove subscription (edge) for {useraccount} and {item} */
    public void unsubscribe(long accountId, long itemId);

    /** Send a notification to all involved users that something has happened to {item} */
    public void notify(String title, String message, DeepaMehtaObject item);

}
