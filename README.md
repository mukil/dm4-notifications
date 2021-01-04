
# DMX Notifications

The DMX Notifications plugin makes it easy for developers to create persistent notifications across sessions and screens.

Important: There are several privacy-related issues unsolved (see https://git.dmx.systems/dmx-plugins/dmx-notifications/-/issues/10). The current version is experimental and released for DEMO purposes only. Furthermore, the type definition should not be used as a template for other plugins (see https://git.dmx.systems/dmx-plugins/dmx-notifications/-/issues/12). The latest version does not resemble the feature of the DM 4 version yet (see https://git.dmx.systems/dmx-plugins/dmx-notifications/-/issues/11).

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

A subscription is created using the "Subscribe" (resp. "Unsubscribe") context command on eligible topics.

### Accessing notifications (In-App) 

When logged in you find your notifications behind the Envelope-icon in the upper right corner of the window. A badge indicates your unread notifications.

## Requirements

DMX 5.1 is a platform for collaboration and knowledge management.
https://github.com/dmx-systems/dmx-platform

## License

DMX Notifications is available freely under the GNU Affero General Public License, version 3 or later (see [License](https://git.dmx.systems/dmx-plugins/dmx-notifications/-/blob/master/LICENSE)).

## Version History

**2.0.0**, Jan 04, 2021

* Data model and backend compatible with DMX 5.1
* Rewritten webclient integration with context commands, envelope icon and a badge indicating unread notifications
* New GUI to list, mark, reveal and delete notifications
* Completely revised notification titles
* "Diffs" upon Note edits are currently not supported (see https://git.dmx.systems/dmx-plugins/dmx-notifications/-/issues/11)
* Your subscriptions and notifications may be READ-able for other users (depending on their workspace assignment)

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
Authors
-------
Copyright (C) 2014-2019 Malte Reißig 

Copyright (C) 2020-2021 DMX Systems

