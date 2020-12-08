
export default ({dm5, Vue, axios:http}) => ({

  state: {
    notifications: []
  },

  actions: {
    _loadUnseenNotifications({state, dispatch}, args) {
      console.log("[Notifications] Loading Unseen Notifications (Actor: \"" + args.username + "\")")
      http.get('/notifications/notification/unseen').then(function(response) {
        state.notifications = response.data
        // dispatch("showUnseenNotifications")
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
      console.log("[Notifications] markNotification")
    }
  }
})
