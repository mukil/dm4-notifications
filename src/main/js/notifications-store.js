
export default ({dm5, Vue, axios:http}) => ({

  state: {
    notifications: [],
    dialogVisible: false
  },

  actions: {
    _loadUnseenNotifications({state}, args) {
      http.get('/notifications/notification/unseen').then(function(response) {
        state.notifications = response.data
        console.log("[Notifications] Loaded",response.data,"Notifications")
      })
    },
    /** showUnseenNotifications({state}, args) {
      console.log("[Notifications] Show ", state.notifications.length, "unseen notifications")
      for (var n in state.notifications) {
        let notification = state.notifications[n]
        Vue.$notify({
          type: "success",
          message: notification.value
        })
      }
    }, */
    markNotificationAsSeen(topicId) {
      // TBD.
      console.log("[Notifications] Mark notification seen", topicId)
    },
    toggleNotificationsDrawer({state}) {
      state.dialogVisible = !state.dialogVisible
    }
  }
})
