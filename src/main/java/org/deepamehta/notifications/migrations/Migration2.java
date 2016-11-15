package org.deepamehta.notifications.migrations;

import de.deepamehta.core.AssociationType;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.Inject;
import de.deepamehta.workspaces.WorkspacesService;
import de.deepamehta.core.service.Migration;
import org.deepamehta.notifications.NotificationsService;


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
        TopicType notificationType = dm4.getTopicType(NotificationsService.NOTIFICATION);
        TopicType titleType = dm4.getTopicType(NotificationsService.NOTIFICATION_TITLE);
        TopicType bodyType = dm4.getTopicType(NotificationsService.NOTIFICATION_BODY);
        TopicType seenType = dm4.getTopicType(NotificationsService.NOTIFICATION_SEEN);
        TopicType subscriptionType = dm4.getTopicType(NotificationsService.SUBSCRIBED_ITEM_ID);
        TopicType involvedType = dm4.getTopicType(NotificationsService.INVOLVED_ITEM_ID);
        //
        AssociationType recipientType = dm4.getAssociationType(NotificationsService.NOTIFICATION_RECIPIENT_EDGE);
        AssociationType edgeType = dm4.getAssociationType(NotificationsService.SUBSCRIPTION_EDGE);
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