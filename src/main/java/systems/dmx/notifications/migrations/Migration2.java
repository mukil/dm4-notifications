package systems.dmx.notifications.migrations;

import systems.dmx.core.AssocType;
import systems.dmx.core.Topic;
import systems.dmx.core.TopicType;
import systems.dmx.core.service.Inject;
import systems.dmx.core.service.Migration;
import systems.dmx.notifications.NotificationsService;
import systems.dmx.workspaces.WorkspacesService;


/**
 * This migration assigns all the custom topic types of this plugin to the public "DMX" Workspace.
 * */
public class Migration2 extends Migration {

    @Inject
    private WorkspacesService workspaceService;

    @Override
    public void run() {

        Topic standardWs = workspaceService.getWorkspace(WorkspacesService.DMX_WORKSPACE_URI);
        //
        TopicType notificationType = dmx.getTopicType(NotificationsService.NOTIFICATION);
        TopicType titleType = dmx.getTopicType(NotificationsService.NOTIFICATION_TITLE);
        TopicType bodyType = dmx.getTopicType(NotificationsService.NOTIFICATION_BODY);
        TopicType seenType = dmx.getTopicType(NotificationsService.NOTIFICATION_SEEN);
        TopicType subscriptionType = dmx.getTopicType(NotificationsService.SUBSCRIBED_ITEM_ID);
        TopicType involvedType = dmx.getTopicType(NotificationsService.INVOLVED_ITEM_ID);
        //
        AssocType recipientType = dmx.getAssocType(NotificationsService.NOTIFICATION_RECIPIENT_EDGE);
        AssocType edgeType = dmx.getAssocType(NotificationsService.SUBSCRIPTION_EDGE);
        //
        workspaceService.assignTypeToWorkspace(notificationType, standardWs.getId());
        workspaceService.assignTypeToWorkspace(titleType, standardWs.getId());
        workspaceService.assignTypeToWorkspace(bodyType, standardWs.getId());
        workspaceService.assignTypeToWorkspace(seenType, standardWs.getId());
        workspaceService.assignTypeToWorkspace(recipientType, standardWs.getId());
        workspaceService.assignTypeToWorkspace(subscriptionType, standardWs.getId());
        workspaceService.assignTypeToWorkspace(involvedType, standardWs.getId());
        workspaceService.assignTypeToWorkspace(subscriptionType, standardWs.getId());
        workspaceService.assignTypeToWorkspace(edgeType, standardWs.getId());

    }

}