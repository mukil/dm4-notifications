<template>
  <div class="dm5-notifications">
    <el-badge :value="notificationsCount" :max="99" v-if="notificationsButtonVisible" type="info">
      <el-button @click="toggleNotificationsDrawer" type="text" :class="'fa fa-' + buttonIconName" title="Open Notificatons Dialog"></el-button>
    </el-badge>
    <el-drawer :title="'Notifications for ' + username" :modal="false" size="35%" :append-to-body="true"
      :visible.sync="notificationsDialogVisible">
      <el-timeline :reverse="reverse" >
        <el-timeline-item placement="top"
            v-for="(notification, index) in notifications"
            :key="index"
            :timestamp="new Date(notification.children['dmx.timestamps.created'].value).toString()">
            {{notification.value}}<br/>
            <el-button type="text">Reveal</el-button>
            <el-button type="text">Mark as seen</el-button>
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
      return (this.notificationsCount > 0)
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
    }
  }
}
</script>

<style>
.dm5-notifications .el-badge__content.is-fixed {
    top: 8px;
    right: 20px;
}
.dm5-notifications {
    position: relative;
    top: -3px;
</style>
