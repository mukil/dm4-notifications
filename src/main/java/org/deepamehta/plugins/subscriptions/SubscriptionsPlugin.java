package org.deepamehta.plugins.subscriptions;

import de.deepamehta.core.Association;
import de.deepamehta.core.DeepaMehtaObject;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.*;
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

    private static final String NOTIFICATION_TYPE = "org.deepamehta.subscriptions.notification";
    private static final String NOTIFICATION_TITLE_TYPE = "org.deepamehta.subscriptions.notification_title";
    private static final String NOTIFICATION_BODY_TYPE = "org.deepamehta.subscriptions.notification_body";
    private static final String NOTIFICATION_CREATOR_ID_TYPE = "org.deepamehta.subscriptions.involved_account_id";
    private static final String NOTIFICATION_ITEM_ID_TYPE = "org.deepamehta.subscriptions.involved_item_id";
    private static final String NOTIFICATION_RECIPIENT_EDGE_TYPE =
            "org.deepamehta.subscriptions.notification_recipient_edge";
    private static final String SUBSCRIPTION_EDGE_TYPE = "org.deepamehta.subscriptions.subscription_edge";
    private static final String NOTIFICATION_SEEN_TYPE = "org.deepamehta.subscriptions.notification_seen";

    private static final String DEFAULT_ROLE_TYPE = "dm4.core.default";

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
            AssociationModel model = new AssociationModel("org.deepamehta.subscriptions.subscription_edge",
                    new TopicRoleModel(accountId, DEFAULT_ROLE_TYPE),
                    new TopicRoleModel(itemId, DEFAULT_ROLE_TYPE),
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
        List<Association> assocs = dms.getAssociations(accountId, itemId,
                "org.deepamehta.subscriptions.subscription_edge");
        Iterator<Association> iterator = assocs.iterator();
        while (iterator.hasNext()) {
            Association assoc = iterator.next();
            dms.deleteAssociation(assoc.getId());
        }
    }

    @Override
    public void notify(String title, String message, long idOfActionista, DeepaMehtaObject item) {
        createNotifications(title, "", idOfActionista, item);
    }

    @Override
    public void receiveAllNotifications() {
        // per requesting user
    }

    @Override
    public void receiveNewNotifications() {
        // per requesting user
    }

    @Override
    public void websocketTextMessage(String message) {
        log.info("### Receiving message from WebSocket client: \"" + message + "\"");

    }

    private void createNotifications(String title, String text, long accountId, DeepaMehtaObject item) {
        // 0) Fetch all subscribers of item X
        ResultList<RelatedTopic> subscribers = item.getRelatedTopics(SUBSCRIPTION_EDGE_TYPE,
                DEFAULT_ROLE_TYPE,  DEFAULT_ROLE_TYPE,  "dm4.accesscontrol.user_account", true, false, 0);
        log.info("Creating notification for all \""+ subscribers.getSize()
                +"\" subcribers of item \"" + item.getSimpleValue() + "\"");
        for (RelatedTopic subscriber : subscribers) {
            log.info("> for " + subscriber.getSimpleValue());
            // 1) Create notification instance
            CompositeValueModel message = new CompositeValueModel()
                    .put(NOTIFICATION_SEEN_TYPE, false)
                    .put(NOTIFICATION_TITLE_TYPE, title)
                    .put(NOTIFICATION_BODY_TYPE, text)
                    .put(NOTIFICATION_CREATOR_ID_TYPE, accountId)
                    .put(NOTIFICATION_ITEM_ID_TYPE, item.getId());
            TopicModel model = new TopicModel(NOTIFICATION_TYPE, message);
            dms.createTopic(model, null); // check: is system the creator?
            // 2) Hook up notification with subscriber
            AssociationModel recipient_model = new AssociationModel(NOTIFICATION_RECIPIENT_EDGE_TYPE,
                    model.createRoleModel(DEFAULT_ROLE_TYPE),
                    new TopicRoleModel(subscriber.getId(), DEFAULT_ROLE_TYPE));
            dms.createAssociation(recipient_model, null); // check: is system the creator?
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
