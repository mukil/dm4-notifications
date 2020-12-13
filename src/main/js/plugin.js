export default ({dmx, store, axios: http, Vue}) => ({

  init () {
    console.log("[DMX Notifications] Initializing")
    store.dispatch("_loadUnseenNotifications", { username: "me"})
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
                  title: "Topic Subscribed",
                  type: "success"
                })
              }).catch(function (error) {
                Vue.prototype.$notify({
                  title: "Subscribing topic failed",
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
                  title: "Topic Unsubscribed",
                  type: "success"
                })
              }).catch(function (error) {
                Vue.prototype.$notify({
                  title: "Unsubscribing topic failed",
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
