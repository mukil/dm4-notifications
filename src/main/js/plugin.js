export default ({dm5, store, axios: http, Vue}) => ({

  init () {
    console.log("[DMX Notifications] Initializing")
    store.dispatch("_loadUnseenNotifications", { username: "me"})
  },

  storeModule: {
    name: 'notifications',
    module: require('./notifications-store').default
  },

  contextCommands: {
    topic: topic => {
      let isLoggedIn = (store.state.accesscontrol.username)
      if ((isLoggedIn && topic.typeUri === 'dmx.topicmaps.topicmap') || (isLoggedIn && topic.typeUri === 'dmx.notes.note') ||
         (isLoggedIn && topic.typeUri === 'dmx.workspaces.workspace') || (isLoggedIn && topic.typeUri === 'dmx.events.event')) { 
        // 1) Check: if alread subscribed
        let hasTargetTypeConfigured = true
        /**  dm5.restClient.getTopicRelatedTopics(topic.id, {
          assocTypeUri: "dmx.csv.file_import", othersTopicTypeUri: "dmx.core.topic_type"})
                .then(response => {
                  hasTargetTypeConfigured = (response.length > 0) ? true : false
                })**/
        // 2) Allow for "Subscrbe" and "Unsubscribe" commands
        /** return [{
          label: 'Import CSV',
          handler: id => {
            // 3.2) Execute if configuration is _potentially_ correct
            if (hasTargetTypeConfigured) {
              http.post(`/csv/import/${id}`)
              .then(function (response) {
                console.log("Import Status", response.data)
                Vue.prototype.$notify({
                  title: "CSV Import Successful",
                  dangerouslyUseHTMLString: true,
                  message: JSON.stringify(response.data.infos),
                  type: "success"
                })
              })
              .catch(function (error) {
                console.error("Import Error", error);
              })
            } else {
              // 3.2) Notify about mis-configuration before executing the import
              Vue.prototype.$notify({
                title: "Import operation needs to know the <i>Topic Type</>",
                dangerouslyUseHTMLString: true, duration: 10000,
                message: "You must relate a <i>Topic Type</i> to <i>"+topic.value+"</i> before importing data. "
                + "Create a <i>File Import</i> association to the <i>Topic Type</i> you want to import data to.",
                type: "error"
              })
            }
          }
        }] **/
        return []
      }
    }
  }

})
