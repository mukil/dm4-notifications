<template>
  <div class="dmx-notifications">
    <el-badge :value="unseenCount" :max="99" v-if="(unseenCount > 0)" @click="toggleNotificationsDrawer">
      <el-button type="text" class="fa fa-envelope" @click="toggleNotificationsDrawer"></el-button>
    </el-badge>
    <el-button type="text" v-if="(unseenCount == 0)" class="fa fa-envelope-o inbox-zero" @click="toggleNotificationsDrawer"></el-button>
    <el-drawer custom-class="dmx-notifications" :title="'Notifications for ' + username" :modal="false" size="40%" :append-to-body="true"
      :visible.sync="notificationsDialogVisible">
      <el-timeline :reverse="reverse" >
        <el-timeline-item placement="top"
            v-for="(notification, index) in notifications"
            :key="index" type="primary"
            :color="colorType(notification)"
            :timestamp="dateTime(notification)">
            <h3>{{notification.children['dmx.notification_body'].value}}</h3>
            <!--div class="notification-body>
                <p>{{notification.children['dmx.notification_body'].value}}</p>
                <p>Performed by {{notification.children['dmx.accesscontrol.username'].value}}</p>
            </div-->
            <el-button-group name="message-commands">
                <el-button @click="revealTopic(notification)" icon="el-icon-top-left">Reveal</el-button>
                <el-button v-if="!notificationSeen(notification)" @click="markNotificationAsSeen(notification)" icon="el-icon-edit">Mark as seen</el-button>
                <el-button @click="deleteNotification(notification)" icon="el-icon-delete">Delete Notification</el-button>
            </el-button-group>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </div>
</template>

<script>
export default {

  data() {
    return {
        reverse: true,
        notificationsDialogVisible: false
    }
  },

  computed: {
    notificationsButtonVisible: function() {
      return (this.username)
    },
    username: function() {
      return this.$store.state.accesscontrol.username
    },
    unseenCount: function() {
      var count = 0;
      for (var el in this.$store.state.notifications.notifications) {
        var note = this.$store.state.notifications.notifications[el]
        if (!this.notificationSeen(note)) {
            count++;
        }
      }
      return count
    },
    notifications: function() {
      return this.$store.state.notifications.notifications
    }
  },

  methods: {
    toggleNotificationsDrawer() {
      this.notificationsDialogVisible = !this.notificationsDialogVisible
    },
    dateTime(topic) {
      return new Date(topic.children['dmx.timestamps.created'].value).toString()
    },
    notificationSeen(topic) {
      return (topic.children["dmx.notification_seen"].value)
    },
    colorType(topic) {
      return (this.notificationSeen(topic)) ? "#e7e7e7" : "#67C23A"
    },
    revealTopic(topic) {
      let topicId = topic.children['dmx.involved_item_id'].value
      this.$store.dispatch("revealTopicById", topicId)
    },
    markNotificationAsSeen(topic) {
      topic.children['dmx.notification_seen'].value = true
      this.$store.dispatch("markNotificationAsSeen", {topic: topic})
    },
    deleteNotification(topic) {
      this.$store.dispatch("deleteNotification", {topic: topic})
    }
  }
}
</script>

<style>
.dmx-notifications .el-timeline-item__content h3 {
    font-size: var(--heading-font-size);
}
.dmx-notifications .el-badge__content {
    background-color: #67C23A;
}
.dmx-notifications .el-badge__content.is-fixed {
    top: 8px;
    right: 20px;
}
.dmx-notifications .el-timeline-item__wrapper {
    padding-right: 2em;
}
.dmx-notifications .el-timeline-item__timestamp.is-top {
    padding-top: 2px;
}
.dmx-notifications .el-button {
    padding: 4px 8px;
}
.dmx-notifications .el-button.inbox-zero {
    position: relative;
    top: 2px;
}
.dmx-notifications.el-drawer {
    overflow-y: scroll;
}
.dmx-notifications {
    position: relative;
    top: -3px;
}
</style>
