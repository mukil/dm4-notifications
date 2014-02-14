package org.deepamehta.plugins.subscriptions;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.annotation.ConsumesService;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.accesscontrol.service.AccessControlService;
import de.deepamehta.plugins.websockets.event.WebsocketTextMessageListener;
import de.deepamehta.plugins.websockets.service.WebSocketsService;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.deepamehta.plugins.subscriptions.service.SubscriptionService;

@Path("/subscriptions")
public class SubscriptionsPlugin extends PluginActivator implements SubscriptionService,
                                                                    WebsocketTextMessageListener {

    private static Logger log = Logger.getLogger(SubscriptionsPlugin.class.getName());

    private AccessControlService aclService = null;
    private WebSocketsService webSocketsService = null;

    @GET
    @Path("/subscribe/{accountId}/{itemId}")
    public Response subscribeUser(@PathParam("accountId") long accountId, @PathParam("itemId") long itemId,
            @HeaderParam("Cookie") ClientState clientState) {
        // 0) Check for any session
        String logged_in_username = aclService.getUsername();
        if (!logged_in_username.isEmpty()) {
            Topic user = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(logged_in_username), false);
            Topic account = user.getRelatedTopic("dm4.core.composition", "dm4.core.child",
                    "dm4.core.parent", "dm4.accesscontrol.user_account", true, false);
            // 1) Users can just manage their own subscriptions
            if (account.getId() == accountId) {
                subscribe(accountId, itemId, clientState);
                return Response.ok().build();
            }
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/unsubscribe/{accountId}/{itemId}")
    public Response unsubscribeUser(@PathParam("accountId") long accountId, @PathParam("itemId") long itemId) {
        // 0) Check for any session
        String logged_in_username = aclService.getUsername();
        if (!logged_in_username.isEmpty()) {
            Topic user = dms.getTopic("dm4.accesscontrol.username", new SimpleValue(logged_in_username), false);
            Topic account = user.getRelatedTopic("dm4.core.composition", "dm4.core.child",
                    "dm4.core.parent", "dm4.accesscontrol.user_account", true, false);
            // 1) Users can just manage their own subscriptions
            if (account.getId() == accountId) {
                unsubscribe(accountId, itemId);
                return Response.ok().build();
            }
        }
        return Response.noContent().build();
    }

    @Override
    public void subscribe(long accountId, long itemId, ClientState clientState) {
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            //
            AssociationModel model = new AssociationModel("org.deepamehta.subscriptions.subscription",
                    new TopicRoleModel(accountId, "dm4.core.default"),
                    new TopicRoleModel(itemId, "dm4.core.default"),
                    new CompositeValueModel().addRef("org.deepamehta.subscriptions.subscription_type",
                    "org.deepamehta.subscriptions.in_app_subscription"));
            dms.createAssociation(model, clientState);
            log.info("Subscribed " + accountId + " to item " + itemId);
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
        List<Association> assocs = dms.getAssociations(accountId, itemId, "org.deepamehta.subscriptions.subscription");
        Iterator<Association> iterator = assocs.iterator();
        while (iterator.hasNext()) {
            Association assoc = iterator.next();
            dms.deleteAssociation(assoc.getId());
        }
    }

    @Override
    public void notify(String title, String message, DeepaMehtaObject item) {
        /** ResultList<RelatedTopic> subscribers = item.getRelatedTopics("org.deepamehta.subscriptions.subscription",
                "dm4.core.default",  "dm4.core.default",  "dm4.accesscontrol.user_account", true, false, 0);
        for (RelatedTopic subscriber : subscribers) {
            log.info("Notify user with account \"" + subscriber.getSimpleValue() + "\"");
        } */
        // 2) just broadcast it to all connected clients
        broadcastNotification(title, item);
        log.info("Notify All subscribers completed ..");
    }

    @Override
    public void websocketTextMessage(String message) {
        log.info("### Receiving message from WebSocket client: \"" + message + "\"");

    }

    private void broadcastNotification(String message, DeepaMehtaObject topic) {
        if (webSocketsService != null) {
            JSONObject banana = new JSONObject();
            try {
                banana.put("message", message);
                banana.put("topic", topic.toJSON());
                webSocketsService.broadcast(getUri(), banana.toString());
            } catch (JSONException j) {
                log.warning("Problems with sending banana-message to all clients.");
            }
        } else {
            log.warning("MoodleServiceClient.webSocketService is AWAY!");
        }
    }


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

}
