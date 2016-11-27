
# DeepaMehta 4 Notifications

The DeepaMehta 4 Notifications plugin makes it easy for developers to create persistent notifications across sessions and screens. It furthermore takes care that the notifications end up in the recipients _Private Workspace_.

The use case is:

A user subscribes a topic.
The user is notified when the topic is changed.  

Actually the plugin provides:
*   A *Notification* model (a Topic Type for the _message_)
*   A *Subscription* model (an Association Type for the _configurations_)
*   A service to
    *    Manage subscriptions (subscribe/unsubscribe)
    *    Create notifications (persistent notifcations for all users who subscribed given item)

A subscription is created (using DeepaMehta's Webclient) through drawing an association of type `Notification Subscription` between a `Username` topic and a topic of type `Workspace`, `Topicmap` or `Note`. For example see the following screenshot.

![Notification Subscription Example: "admin" subscribed to three items](https://github.com/mukil/dm4-notifications/raw/master/docs/screen_a_notification_subscription_edge_860.png)

When logged in you can find all notifications for you after clicking on your username topic in the upper right corner of the DeepaMehta Webclient. Then, reveal the very first topic, the one which represents your actual _Username_ and you will see all _Notifications_ related to your username. Sorry for this inconvenience. Currently we have no special display which delivers you notifications in a more convenient way. But of course, you're very welcome to encourage us or help us to design or create one :)

Your _Notifications_ are accessible and readable to you only (unless you share them). For that reason you can find your notifications also in the related section after  revealing your `Private Workspace`.

## Requirements

DeepaMehta 4 is a platform for collaboration and knowledge management.
https://github.com/jri/deepamehta

To be able to install this module you first and additionally have to install the following DeepaMehta 4 Plugins.

* `dm48-websockets-0.3`-Bundle - [Source Code](https://github.com/jri/dm4-websockets), [Nightly Build](http://download.deepamehta.de/nightly/)

## Download & Installation

You can download the latest `dm48-notifications-X.Y.Z.jar-`-Bundle from [here](http://download.deepamehta.de/nightly/).

After downloading the bundle-files, place them (websockets, tags, subscriptions) in the `bundles` folder of your DeepaMehta installation and restart DeepaMehta.

## Usage 

Installation of this plugin on its own is (yet) of limited use for end-users.

The main features of this plugin is a service to be used by other application developers.

## Application Model

![Screenshot of Notification Model in DM, Selected TopicType Notification](/notification_model_doc.png)

## GNU Public License

This sofware is released under the terms of the GNU General Public License in Version 3.0, 2007.

## Version History

**1.1**, Upcoming, 2016

* Renamed plugin to dm4-notifications as the new better expresses what this plugin is all about
* Removed dependency to dm4.tags module
* Renamed all type URIs
* Allows to create subscriptions around any type of topic
* Associates subscriptions to (public) username instead of (private) user account topics
* Stores all notifications in the "Private Workspace of the subscribed user
* Per default: Support (In-App) subscriptions on "Workspace" (New Topicmaps) and "Topicmap" (Topics added) by default
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
Author: Malte Reißig, 2014

