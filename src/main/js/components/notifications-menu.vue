<template>
  <div class="dm5-notifications">
    <el-badge :value="notificationsCount" :max="99" v-if="notificationsButtonVisible" type="info">
      <el-button @click="toggleNotificationsDrawer" type="text" :class="'fa fa-' + buttonIconName" title="Open Notificatons Dialog"></el-button>
    </el-badge>
    <el-drawer custom-class="dm5-notifications" :title="'Notifications for ' + username" :modal="false" size="40%" :append-to-body="true"
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
                <el-button @click="revealTopic(notification)" icon="el-icon-edit">Reveal</el-button>
                <el-button @click="markNotificationAsSeen(notification)" icon="el-icon-edit">Mark as seen</el-button>
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
      return (this.notificationsCount > 0 && this.username)
    },
    username: function() {
      return this.$store.state.accesscontrol.username
    },
    notificationsCount: function() {
      return this.$store.state.notifications.notifications.length
    },
    notifications: function() {
      return this.$store.state.notifications.notifications
    },
    buttonIconName: function() {
      return (this.$store.state.notifications.notifications.length > 0) ? 'envelope' : 'envelope-o'
    }
  },

  methods: {
    toggleNotificationsDrawer() {
      this.notificationsDialogVisible = !this.notificationsDialogVisible
    },
    dateTime(topic) {
      return new Date(topic.children['dmx.timestamps.created'].value).toString()
    },
    colorType(topic) {
      let seen = topic.children["dmx.notification_seen"].value
      return (seen) ? "e7e7e7" : "#67C23A"
    },
    revealTopic(topic) {
      let topicId = topic.children['dmx.involved_item_id'].value
      this.$store.dispatch("revealTopicById", topicId)
    },
    markNotificationAsSeen(topic) {
      this.$store.dispatch("markNotificationAsSeen", {topic: topic})
    }
  }
}
</script>

<style>
.dm5-notifications .el-badge__content.is-fixed {
    top: 8px;
    right: 20px;
}
.dm5-notifications .el-timeline-item__wrapper {
    padding-right: 2em;
}
.dm5-notifications .el-timeline-item__timestamp.is-top {
    padding-top: 2px;
}
.dm5-notifications .el-button {
    padding: 4px 8px;
}
.dm5-notifications {
    position: relative;
    top: -3px;
}
</style>
