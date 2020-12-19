export default function ({dmx, store, axios: http, Vue}) {

  const subscribeCmd = {
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
  }

  const unsubscribeCmd = {
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

  function determineCommands(topic) {

    const isLoggedIn = (store.state.accesscontrol.username)
    const isSupportedType = (
        topic.typeUri === 'dmx.topicmaps.topicmap' ||
        topic.typeUri === 'dmx.notes.note' ||
        topic.typeUri === 'dmx.workspaces.workspace' || 
        topic.typeUri === 'dmx.events.event')
    // 1) topic is unsupported
    if (!isLoggedIn || !isSupportedType) return []
    // 2) topic is supported
    return http.get('/notifications/subscription/' + topic.id).then(result => {
      return (result.data) ? [unsubscribeCmd] : [subscribeCmd]
    })

  }
  
  return {
    init () {
      if (store.state.accesscontrol.username) {
        store.dispatch("_loadUnseenNotifications", { username: "me"}) 
      }
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
      topic: topic => determineCommands(topic).then(result => {
        console.log("determinedCommands", result)
        return result
      })
    }
  }

}
