
export default ({dm5, Vue, axios:http}) => ({

  state: {
    notifications: [],
    dialogVisible: false
  },

  actions: {
    _loadUnseenNotifications({state}, args) {
      http.get('/notifications/notification').then(function(response) {
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
      } // 
    }, */
    markNotificationAsSeen({state}, {topic}) {
      console.log("[Notifications] Mark notification seen", topic)
      http.put('/notifications/notification/seen/' + topic.id).then(response => {
        console.log("Marked "+ topic + " notification as seen - This will not be shown again.")
        // update state.notifications
        topic = response.data
      }).catch(response => {
        console.error(response)
      })
    },
    toggleNotificationsDrawer({state}) {
      state.dialogVisible = !state.dialogVisible
    }
  }
})
