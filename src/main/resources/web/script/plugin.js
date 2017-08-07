
dm4c.add_plugin('org.deepamehta.notifications', function() {

    var websocket = undefined
    var aclPlugin = undefined
    var url = dm4c.restc.request("GET", "/websockets")["dm4.websockets.url"]

    var BUNDLE_URI = "org.deepamehta.notifications"
    var SUBSCRIPTION_EDGE_URI = "org.deepamehta.notification_subscription_edge"

    function create_websocket_listener() {
        if (!websocket && url) {
            websocket = new WebSocket(url, BUNDLE_URI)
            websocket.onopen = function(e) {
                console.log("Opening Notification WebSocket connection to " + e.target.url)
                websocket.send("Hello Notifications -WebSockets server! I am a "  + window.navigator.userAgent)
            }
            websocket.onmessage = function(e) {
                console.log("Received message, we should fetch unseen notifications and render them ... ("+e+")")
            }

            websocket.onclose = function(e) {
                console.log("Closing Notification WebSocket connection to " + e.target.url + " (" + e.reason + ")", e)
                console.log("Reopening Notification Websocket connection ...")
                setTimeout(create_websocket_listener, 5000)
            }
        } else {
            console.error('could not fetch websocket url configuration to create a websocket connection')
        }
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
            create_websocket_listener()
        }
    })

    dm4c.add_listener("logged_in", function() {
        create_websocket_listener()
    })

    dm4c.add_listener("authority_decreased", function() {
        if (!is_logged_in()) {
            websocket.close()
            websocket = undefined
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
