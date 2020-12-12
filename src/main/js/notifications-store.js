
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
    markNotificationAsSeen(topicId) {
      // TBD.
      console.log("[Notifications] Mark notification seen", topicId)
    },
    toggleNotificationsDrawer({state}) {
      state.dialogVisible = !state.dialogVisible
    }
  }
})
