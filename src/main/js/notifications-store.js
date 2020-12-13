
export default ({dmx, Vue, axios:http}) => ({

    state: {
      notifications: [],
      dialogVisible: false
    },

    actions: {
      _loadUnseenNotifications( {state}, args) {
        http.get('/notifications/notification').then(function (response) {
          state.notifications = response.data
          console.log("[Notifications] Loaded", response.data, "Notifications")
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
      markNotificationAsSeen( {state}, {topic}) {
        http.put('/notifications/notification/seen/' + topic.id).then(response => {
          topic = response.data
        }).catch(response => {
          console.error(response)
        })
      },
      deleteNotification( {state}, {topic}) {
        state.notifications = state.notifications.filter(message => message.id !== topic.id)
        http.delete('/notifications/notification/' + topic.id).then(response => {
          console.log("Deleted " + topic.value + " from storage")
        }).catch(response => {
          console.error(response)
        })
      },
      toggleNotificationsDrawer( {state}) {
        state.dialogVisible = !state.dialogVisible
      }
    }
  })
