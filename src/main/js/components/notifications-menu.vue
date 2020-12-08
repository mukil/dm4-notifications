<template>
    <div class="dm5-notifications" v-if="isLoggedIn">
        <el-badge :value="notificationCount" :max="99" class="item">
            <el-button>...</el-button>
        </el-badge>
    </div>
</template>

<script>
export default {

  inject: {
    http: 'axios'
  },

  computed: {
    isLoggedIn: function() {
      return (this.$store.state.accesscontrol.username)
    },
    notificationCount: function() { 
      return this.$store.state.notifications.notifications.length
    },
    notifications: function() { 
      console.log("[Notifications] Computed", this.$store.state.notifications.notifications.length, "notifications")
      return this.$store.state.notifications.notifications
    }
  }/**,

  watch: {
    notifications: function(newVal, oldVal) {
      for (var n in this.notifications) {
        let notification = this.notifications[n]
        console.log("[Notifications] Show Notification", notification)
        this.$notify({
          type: "success",
          message: notification.value
        })
      }
    }
  } **/

}
</script>


<style>
.dm5-notifications button {
    margin-left: .75em;
    /** To align with Login State Menu: Padding and Font-Size copied from .dm5-login-state .el-button.sign-in **/
    font-size: 12px !important;
    padding: 5px 8px !important;
}
</style>
