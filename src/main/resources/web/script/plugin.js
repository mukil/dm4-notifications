
dm4c.add_plugin('org.deepamehta.notifications', function() {

    var aclPlugin = undefined
    var websocketPlugin = undefined


    var BUNDLE_URI = "org.deepamehta.notifications"
    var SUBSCRIPTION_EDGE_URI = "org.deepamehta.notification_subscription_edge"

    function on_message(message) {
        console.log("Recieved websocket message", message)
    }

    function subscription_exists() {
        var requestUri = '/notifications/subscription/' + dm4c.selected_object.id
        //
        var check = false
        $.ajax({
            type: "GET", url: requestUri,
            dataType: "text", processData: false,
            async: false,
            success: function(data, text_status, jq_xhr) {
                // console.log("Has user subscribed selected_object?", data) // debug
                check = data
            },
            error: function(jq_xhr, text_status, error_thrown) {
                console.warn("RESTClientError: GET request failed (" + text_status + ": " + error_thrown + ")")
                check = false
            }
        })
        return (check === "true")
    }

    function subscribe_topic() {
        var requestUri = '/notifications/subscribe/' + dm4c.selected_object.id
        //
        $.ajax({
            type: "GET", url: requestUri,
            dataType: "text", processData: false,
            async: true,
            success: function(data, text_status, jq_xhr) {
                try {
                   var username = get_username_topic()
                   if (username) {
                       dm4c.do_reveal_related_topic(username.id, 'none', SUBSCRIPTION_EDGE_URI)
                   }
                } catch (e) {
                    console.warn(e, text_status)
                }
                dm4c.page_panel.refresh()
            },
            error: function(jq_xhr, text_status, error_thrown) {
                console.warn("RESTClientError: GET request failed (" + text_status + ": " + error_thrown + ")")
            }
        })
    }

    // configure menu and type commands
    dm4c.add_listener('topic_commands', function (topic) {
        if (!dm4c.has_create_permission_for_association_type(SUBSCRIPTION_EDGE_URI)) {
            return
        }
        var commands = []
        if (topic.type_uri === 'dm4.workspaces.workspace'
            || topic.type_uri === 'dm4.topicmaps.topicmap'
            || topic.type_uri === 'dm4.notes.note') {
            // check only for these three types if subscription exists
            if (!subscription_exists()) {
                commands.push({is_separator: true, context: 'context-menu'})
                commands.push({
                    label: 'Subscribe',
                    handler: subscribe_topic,
                    context: ['context-menu', 'detail-panel-show']
                })
            }
        }
        return commands
    })

    dm4c.add_listener("init_3", function() {
        if (is_logged_in()) {
            websocketPlugin = dm4c.get_plugin("de.deepamehta.websockets")
            websocketPlugin.create_websocket(BUNDLE_URI, on_message)
        }
    })

    dm4c.add_listener("logged_in", function() {
        websocketPlugin = dm4c.get_plugin("de.deepamehta.websockets")
        websocketPlugin.create_websocket(BUNDLE_URI, on_message)
    })

    dm4c.add_listener("authority_decreased", function() {
        if (!is_logged_in()) {
            // websocketPlugin.close()
            console.log('TOOD: close websocket connection..')
        }
    })

    function is_logged_in() {
        // assert the precondition of our implementation is met
        if (!aclPlugin) {
            aclPlugin = dm4c.get_plugin("de.deepamehta.accesscontrol")
        }
        // do login check through the acl plugin
        return aclPlugin.get_username()
    }

    function get_username_topic() {
        // assert the precondition of our implementation is met
        if (!aclPlugin) {
            aclPlugin = dm4c.get_plugin("de.deepamehta.accesscontrol")
        }
        // do login check through the acl plugin
        return aclPlugin.get_username_topic()
    }

})
