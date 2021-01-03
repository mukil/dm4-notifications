
# DMX Notifications

The DMX Notifications plugin makes it easy for developers to create persistent notifications across sessions and screens. It furthermore takes care that the notifications end up in the recipients _Private Workspace_.

The use case is:

A user subscribes a topic.
The user is notified when the topic is changed.  

Actually the plugin provides:
*   A *Notification* model (a Topic Type representing a _message_)
*   A *Subscription* model (an Association Type for the _configurations_)
*   A service to
    *    Manage subscriptions (subscribe/unsubscribe)
    *    Create notifications (persistent notifcations for all users who subscribed given item)


## Notifications Model

The following figure you find an overview of the type definitionst dmx-notifications installs in DMX. 

![Screenshot of Notification Model in DMX, Selected TopicType Notification](/notification_model_doc.png)

## Usage: Subscribe items to receive notifications

A subscription is created (using the DMX Webclient) through creating a `Notification Subscription` association between your `Username` topic and a topic of type `Workspace`, `Topicmap` or `Note`. For example see the following screenshot.

![Notification Subscription Example: "admin" subscribed to three items](https://github.com/mukil/dmx-notifications/raw/master/docs/notification_subscription_configuration.png)

For the mentioned subscriptions the following actions will notify subscribers:
*   Workspace: Whenever a new *Topicmap*, *Note* or *Event* is created in the workspace
*   Topicmap: Whenever another user with write permission `adds a topic` to it
*   Note: Whenever another user with WRITE permissions `Edits` the contents of the note<br/>
    (In this case, the notificiation includes the old and the new contents of the topic, [example](https://github.com/mukil/dmx-notifications/blob/master/docs/screen_c_notification_topics_75perc.png))

### Accessing notifications (In-App) 

When logged in you find your notifications related to your username. You can reveal your username in a topicmap using the Search/Create Dialog. Related to your username you will find all your _Notifications_. You can navigate to your notifications also using the *Related* tab after revealing your `Private Workspace`.

![Accessing your notification in the Webclient](https://github.com/mukil/dmx-notifications/raw/master/docs/screen_b_notification_topics_75perc.png)

As of DMX there is no special display (yet) which delivers you your notifications in a more convenient way. Of course, you're very welcome to encourage us or help us to design or create one :)

Your _Notifications_ are accessible and readable to you only (unless you move them to another workspace). 

## Requirements

DMX 5.0 is a platform for collaboration and knowledge management.
https://github.com/jri/deepamehta

## Download & Installation

You can download the latest `dmx-notifications`-Bundle [here](http://download.dmx.systems/).

## GNU AGPL Public License

DMX Notifications is available freely under the GNU Affero General Public License, version 3 or later (see [License](https://git.dmx.systems/dmx-plugins/dmx-sign-up/-/blob/master/LICENSE)).

## Version History

**2.0**, UPCOMING

* Data model and backend compatible with DMX 5.0

**1.2**, Aug 11, 2017

Maintenance release:
* Adapted to be compatible with DeepaMehta 4.8.6 and DM 4 Websockets 0.4

**1.1**, Jan 04, 2017

* Renamed plugin to dm4-notifications as the new better expresses what this plugin is all about
* Removed dependency to dm4.tags module
* Introduced `Subscribe` topic command
* Renamed all type URIs
* Allows to create subscriptions around any type of topic
* Adapted client side plugin.js to receive websocket notifications (broadcasts)
* Associates subscriptions to (public) username instead of (private) user account topics
* Stores all notifications in the "Private Workspace of the subscribed user
* Per default: Support subscriptions of changes in a `Workspace` (New Topicmaps), in a `Topicmap` (Topics added) or to a `Note`s (Content updated)
* Compatible with DeepaMehta 4.8

Note: This release is not compatible with previous releases and there is no migration provided.

**1.0.3**, Dec 25, 2014

- compatible with DeepaMehta 4.4
- code revision

**1.0.2**, Nov 17, 2014
- revised service method signatures
- introducing some javadoc
- compatible with DM 4.3

**1.0.0**, May 13 2014
- Introducing persistent Notifications and a Subscription edge
- compatible with DM4.2

--------------------------
Author: Malte Reißig, 2014 - 2017

