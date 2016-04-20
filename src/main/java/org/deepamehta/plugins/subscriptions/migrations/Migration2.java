package org.deepamehta.plugins.subscriptions.migrations;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Inject;
import de.deepamehta.workspaces.WorkspacesService;
import de.deepamehta.core.service.Migration;
import org.deepamehta.plugins.subscriptions.SubscriptionService;


/**
 * This migration assigns all the custom topic types of this plugin to the public "DeepaMehta" Workspace.
 * */
public class Migration2 extends Migration {

    @Inject
    private WorkspacesService workspaceService;

    @Override
    public void run() {

        Topic deepaMehtaWs = workspaceService.getWorkspace(WorkspacesService.DEEPAMEHTA_WORKSPACE_URI);
        //
        TopicType notificationType = dm4.getTopicType(SubscriptionService.NOTIFICATION);
        TopicType titleType = dm4.getTopicType(SubscriptionService.TITLE_TYPE);
        TopicType bodyType = dm4.getTopicType(SubscriptionService.BODY_TYPE);
        TopicType seenType = dm4.getTopicType(SubscriptionService.SEEN_TYPE);
        TopicType subscriptionType = dm4.getTopicType(SubscriptionService.SUBSCRIBED_ITEM_ID_TYPE);
        TopicType involvedType = dm4.getTopicType(SubscriptionService.INVOLVED_ITEM_ID_TYPE);
        AssociationType recipientType = dm4.getAssociationType(SubscriptionService.RECIPIENT_EDGE_TYPE);
        AssociationType edgeType = dm4.getAssociationType(SubscriptionService.SUBSCRIPTION_EDGE_TYPE);
        //
        workspaceService.assignTypeToWorkspace(notificationType, deepaMehtaWs.getId());
        workspaceService.assignTypeToWorkspace(titleType, deepaMehtaWs.getId());
        workspaceService.assignTypeToWorkspace(bodyType, deepaMehtaWs.getId());
        workspaceService.assignTypeToWorkspace(seenType, deepaMehtaWs.getId());
        workspaceService.assignTypeToWorkspace(recipientType, deepaMehtaWs.getId());
        workspaceService.assignTypeToWorkspace(subscriptionType, deepaMehtaWs.getId());
        workspaceService.assignTypeToWorkspace(involvedType, deepaMehtaWs.getId());
        workspaceService.assignTypeToWorkspace(subscriptionType, deepaMehtaWs.getId());
        workspaceService.assignTypeToWorkspace(edgeType, deepaMehtaWs.getId());

    }

}