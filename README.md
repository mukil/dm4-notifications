
# DeepaMehta 4 Subscriptions

The DeepaMehta 4 Subscriptions plugin provides developers the base for realizing a thematic publish/subscribe function.

The publish/subscribe use case is:

A user subscribes a tag.  
Items (topics) are tagged.  
The user is notified when a such tagged item is changed.  

Actually the plugin provides:
*   A *Notification* model (a Topic Type for the _message_)
*   A *Subscription* model (an Association Type for the _configurations_)
*   A service to
    *    Manage subscriptions (subscribe/unsubscribe)
    *    Create notifications (persistent notifcations for all users who subscribed given item)

A subscription-edge (associating a user with a subscribed item) can have many subscription types (as child topics). The plugin developers conceived two basic subscription types in advance: `In-App` (delivery via dm4-websockets) and `Mail` (not yet implemented).

## Requirements

DeepaMehta 4 is a platform for collaboration and knowledge management.
https://github.com/jri/deepamehta

To be able to install this module you first and additionally have to install the following DeepaMehta 4 Plugins.

* `dm44-websockets-0.2.2`-Bundle - [Source Code](https://github.com/jri/dm4-websockets), [Nightly Build](http://download.deepamehta.de/nightly/)
* `dm44-deepamehta-tags-1.3.8`-Bundle - [Source Code](https://github.com/mukil/dm4.tags), [Nightly Build](http://download.deepamehta.de/nightly/)

## Download & Installation

You can download the latest `dm44-subscriptions-X.Y.Z.jar-`-Bundle from [here](http://download.deepamehta.de/nightly/).

After downloading the bundle-files, place them (websockets, tags, subscriptions) in the `bundles` folder of your DeepaMehta installation and restart DeepaMehta.

## Usage 

Installation of this plugin on its own is of limited use for end-users.

The main features of this plugin is a service to be used by other application developers.

For example, the `dm4-notizen-app` makes use of this plugin and provides an examplary user-interface for this service.

## Application Model

![Screenshot of Subscription Application Model in DM, Selected TopicType Notification](/subscription_model_doc.png)

## GNU Public License

This sofware is released under the terms of the GNU General Public License in Version 3.0, 2007.

## Version History

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

