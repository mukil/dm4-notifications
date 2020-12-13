export default ({dmx, store, axios: http, Vue}) => ({

  init () {
    console.log("[DMX Notifications] Initializing")
    store.dispatch("_loadUnseenNotifications", { username: "me"})
    // React to login! and logout!
  },

  storeModule: {
    name: 'notifications',
    module: require('./notifications-store').default
  },

  extraElementUI: true,

  components: [{
    comp: require('./components/notifications-menu').default,
    mount: 'toolbar-right'
  }],

  contextCommands: {
    topic: topic => {
      let isLoggedIn = (store.state.accesscontrol.username)
      if ((isLoggedIn && topic.typeUri === 'dmx.topicmaps.topicmap') || (isLoggedIn && topic.typeUri === 'dmx.notes.note') ||
         (isLoggedIn && topic.typeUri === 'dmx.workspaces.workspace') || (isLoggedIn && topic.typeUri === 'dmx.events.event')) { 
        // Fixme: allow subscription of "Tags" again
        // 1) Check: if alread subscribed
        /** let isSubscribed = false
        dmx.rpc.getTopicRelatedTopics(topic.id, {
          assocTypeUri: "dmx.notification_subscription_edge",
          othersTopicTypeUri: "dmx.accesscontrol.username"
        }).then(response => {
          console.log("[Notifications] is my username subscribed?", response)
        }) **/
        // 2) Add "Subscribe" and "Unsubscribe" commands
        return [{
          label: 'Subscribe',
          handler: id => {
              http.post(`/notifications/subscribe/${id}`).then(function (response) {
                Vue.prototype.$notify({
                  title: "Subscribed to " + dmx.typeCache.getTopicType(topic.typeUri).value + " " + topic.value,
                  type: "success"
                })
              }).catch(function (error) {
                console.log("error", error)
                Vue.prototype.$notify({
                  title: "Subscribing to " + dmx.typeCache.getTopicType(topic.typeUri).value  + " failed",
                  message: error.toString(),
                  type: "error"
                })
              })
            }
          },
          {
            label: 'Unsubscribe',
            handler: id => {
              http.post(`/notifications/unsubscribe/${id}`).then(function (response) {
                Vue.prototype.$notify({
                  title: "Unsubscribed from " + dmx.typeCache.getTopicType(topic.typeUri).value + " " + topic.value,
                  type: "success"
                })
              }).catch(function (error) {
                console.log("error", error)
                Vue.prototype.$notify({
                  title: "Unsubscribing from " + dmx.typeCache.getTopicType(topic.typeUri).value + " " + topic.value + " failed",
                  message: error.toString(),
                  type: "error"
                })
              })
            }
          }
        ]
      }
    }
  }

})
