export default function ({dmx, store, axios: http, Vue}) {

  // reference to selected topic used in contextCommand handlers
  let topic = undefined

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

  function determineCommands(selectedTopic) {
    // keep reference for command handlers
    topic = selectedTopic
    const isLoggedIn = (store.state.accesscontrol.username)
    const isSupportedType = (
        selectedTopic.typeUri === 'dmx.topicmaps.topicmap' ||
        selectedTopic.typeUri === 'dmx.notes.note' ||
        selectedTopic.typeUri === 'dmx.workspaces.workspace' ||
        selectedTopic.typeUri === 'dmx.events.event')
    // 1) topic is unsupported
    if (!isLoggedIn || !isSupportedType) return []
    // 2) topic is supported
    return [http.get('/notifications/subscription/' + selectedTopic.id).then(result => {
      return (result.data) ? unsubscribeCmd : subscribeCmd
    })]
  }

  return {
    // ### Fixme: How to react to login! and logout! in dmx-webclient?
    init () {
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
      topic: topic => determineCommands(topic)
    }
  }

}
