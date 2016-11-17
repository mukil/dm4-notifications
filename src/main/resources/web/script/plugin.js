
dm4c.add_plugin('org.deepamehta.notifications', function() {

    var websocket = undefined
    var aclPlugin = undefined

    var WEBSOCKET_HOST = document.location.hostname // Fixme: Use dm4.host.url?
    var WEBSOCKET_PORT = 8081   // Fixme: Use dm4.websocket.port
    var BUNDLE_URI = "org.deepamehta.notifications"

    function create_websocket_listener() {

        if (!websocket) {
            websocket = new WebSocket("ws://" + WEBSOCKET_HOST + ":" + WEBSOCKET_PORT, BUNDLE_URI)

            websocket.onopen = function(e) {
                console.log("Opening Notification WebSocket connection to " + e.target.url, e)
                websocket.send("Hello Notifications -WebSockets server! I am a "  + window.navigator.userAgent)
            }
            websocket.onmessage = function(e) {
                console.log("Received message, we should fetch unseen notifications and re-render the users toolbar ..")
            }

            websocket.onclose = function(e) {
                console.log("Closing Notification WebSocket connection to " + e.target.url + " (" + e.reason + ")", e)
            }
        }
    }

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

})
