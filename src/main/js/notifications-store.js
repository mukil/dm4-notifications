
export default ({dm5, Vue, axios:http}) => ({

  state: {
    notifications: []
  },

  actions: {
    _loadUnseenNotifications({state}, args) {
      console.log("[Notifications] Loading Unseen Notifications (Actor: \"" + args.username + "\")")
      http.get('/notifications/notification/unseen').then(function(response) {
        state.notifications = response.data
      })
    },
    markNotificationAsSeen(topicId) {
      // TBD.
      console.log("[Notifications] markNotification")
    }
  }
})
