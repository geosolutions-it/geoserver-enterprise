Clustering
========== 

Introduction
------------

There exists various approaches with GeoServer to implement a clustered deployment, based on different mixes of data directory sharing plus configuration reload. However, these techniques have intrinsic limitations in terms of scalability therefore we decided to create a specific GeoServer Clustering Extension in order to overcome them. It is worth to point out that what we are going to describe is designed to work with GeoServer 2.1 stable series as well as with the 2.2.x. Our approach is shown in Illustration .

.. figure:: images/Schema.png
   :align: center
   :alt: Illustration  Clustering Solution for GeoServer

We have implemented a robust Master/Slave approach which leverages on a Message Oriented Middleware (MOM) where:
The Masters (yes, we can have more than one, read on...) accept changes to the internal configuration, persist them on their own data directory but also forward them to the Slaves via the MOM
The Slaves should not be used to change  their configuration from either REST or the User Interface, since are configured to inject configuration changes disseminated by the Master(s) via the MOM
The MOM is used to make the Master and the Slave exchange messages in a durable fashion
Each Slave has its own data directory and it is responsible for keeping it aligned with the Master's one. In case a Slave goes down when it goes up again he might receive a bunch of JMS messages to align its configuration to the Master's one.
A Node can be both Master and Slave at the same time, this means that we don't have a single point of failure, i.e. the Master itself

Summarizing, the Master as well as each Slave use a private data directory, Slaves receive changes from the Master, which is the only one where configuration changes are allowed, via JMS messages. Such messages transport GeoServer configuration objects that the Slaves inject directly in their own in-memory configuration and then persist on disk on their data directory, completely removing the need for a configuration reload for each configuration change.

Description
-----------

The GeoServer Master/slave integration is implemented using JMS, Spring and a MOM (Message Oriented Middleware), in particular ActiveMQ.
The schema in Illustration  represents a complete high level design of Master/Slave platform.
It is composed by 3 distinct actors:

1. GeoServer Masters
2. GeoServer Slaves
3. The MOM (ActiveMQ)

This structure allows to have:
1. Queue fail-over components (using MOM).
2. Slaves down are automatically handled using durable topic (which will keep missed message to re-synch changes happens during the slave down).
3. Master down will not affect any slave synchronization process.

This deployment is composed by:
A pure Master GeoServer(s), this instance can only send events to the topic.It cannot act as a slave
A set of Geoserver which can work as both Master and Slave. These instances can send and receive messages to/from the topic. They can work as Masters (sending message to other subscribers) as well as Slaves (these instances are also subscribers of the topic).
A set of pure Slaves GeoServer instances whic can only receive messages from the topic.
A set of MOM brokers so that each GeoServer instance is configured with a set of available brokers (failover). Each broker use the shared database as persistence. Doing so if a broker fails for some reason, messages can still be written and read from the shared database.

All the produced code is based on spring-jms to ensure portability amongs different MOM, but if you look at the schema, we are also leveraging ActiveMQ VirtualTopics to get dinamic routing (you can dinamically attach masters and slaves).

The VirtualTopics feature has also other advantages explained here http://activemq.apache.org/virtual-destinations.html


.. figure:: images/Arch.png
   :align: center
   :alt: Illustration  Component Diagram for the MOM based clustering

Implementation
--------------
The comunity plugin is composed by 3 different modules (plus one which can be used as external broker):

1. jms-commons
2. jms-geoserver
3. jms-activeMQ
4. activemqBroker

jms-commons
^^^^^^^^^^^

Contains only the low level definition of all the used interface.
Depends from Spring JMS.

jms-geoserver
^^^^^^^^^^^^^

Is the geoserver cluster core plugin, it implements the Master and the Slave and all the needed GeoServer event listeners (currently this is done for configuration and catalog).
Define a set of classes and methods to serialize or wrap those events to produce valid JMS messages.
For each incoming event type the module will apply the assigned method (from the above set) obtaining a valid JMS message which are sent to a durable topic queue.
The messages from the topic can be consumed by a set of classes and methods to perform message de-serialization accordingly to the incoming message type and properties.
For each incoming message the module will apply the assigned method (from the above set) obtaining a valid Object (f.e.: CatalogEvent).
The object will be used to apply changes to the target component (for instance the configuration or the catalog).
It also ships an GUI interface to handle the various components and to check the status
Depends from geoserver and the jms-commons module.

jms-activemq
^^^^^^^^^^^^

Is a the activeMQ implementation of a factory used to instantiate the needed JMS components (essentially 2 Destination and 1 Connection).
Depends from ActiveMQ and jms-commons.

It is however possible as indicated above that an instance of  GeoServer would work both as master as well as Slave (looking at events coming from other GeoServer(s)). This is useful to setup a multimaster enviroment that allows the modification to keep flowing even in face of failure of one of the masters (using a failover approach).

Installation
^^^^^^^^^^^^

To install the jms cluster modules into an existing geoserver refer to the :ref:`jms.installation` page 

Building
^^^^^^^^

To build geoserver with cluster support you only need to add the **cluster** profile to the maven command line:

.. code-block:: xml
  
  mvn clean install -Pcluster


activemqBroker
--------------

Very small web application based on ActiveMQ which is preconfigured to be used with the geoserver jms cluster module plugin.

.. toctree:: 
   :maxdepth: 1

   activemq/activemqBroker
   activemq/JDBC
   activemq/SharedFolder

Building
^^^^^^^^

To build the standalone broker you only need to add the **activemq** profile to the maven command line:

.. code-block:: xml
  
  mvn clean install -Pactivemq
  
You'll get two distinct war:

The standalone ActiveMQ broker:

.. code-block:: xml
  
  ./community/cluster/activemqBroker/activemq_webapp/target/activemq.war

The geoserver including the cluster plugin:

.. code-block:: xml
  
  ./web/app/target/geoserver.war


HOW-TO configure GeoServer Instances
====================================

The configuration for the geoserver is very simple and can be performed using the provided GUI or modifying the cluster.properties file which is stored into the GEOSERVER_DATA_DIR under the cluster folder (${GEOSERVER_DATA_DIR}/cluster). To override the default destination of this configuration file you have to setup the **CLUSTER_CONFIG_DIR** variable defining the destination folder of the **cluster.properties** file. This is useful when you want to share the same GEOSERVER_DATA_DIR (which is not needed with geoserver cluster with JMS extensions).

Instance name
-------------
The instance.name is used to distinguish from which GeoServer instance the message is coming from, so each GeoServer instance should use a different, unique (for a single cluster) name.

Broker URL
----------
The broker.url field is used to instruct the internal JMS machinery where to publish messages to (master GeoServer installation) or where to consume messages from (slave GeoServer installation). Many options are available for configuring the connection between the GeoServer instance and the JMS broker, for a complete list, please, check this link. In case when (recommended) failover set up  is put in place multiple broker URLs can be used: please, check this link for more information about how to configure that.
Note
GeoServer will not complete the start-up phase until the target broker is correctly activated and reachable.

Limitations and future extensions
---------------------------------

Data
^^^^
NO DATA IS SENT THROUGH THE JMS CHANNEL
The clustering solution we have put in place is specific for managing the GeoServer internal configuration, no data is transferred between master and slaves. For that purpose use external mechanisms (ref. [GeoServer REST]). 
In principle this is not a limitation per se since usually in a clustered environment data is stored in shared locations outside the data directory. With our solution this is a requirement since each slave will have its own private data directory.

Events
^^^^^^
NEVER RELOAD THE GEOSERVER CATALOG ON A MASTER
Each master instance should never call the catalog reload since this propagates the creation of all the resources, styles, etc to all the connected slaves.
NEVER CHANGE CONFIGURATION USING A PURE SLAVE
This will make the configuration of the specific slave out of synch with the others.

Refs:
-----

.. toctree:: 
   :maxdepth: 1

   installation
   activemq/activemqBroker
   activemq/JDBC
   activemq/SharedFolder

Bibliography:
-------------

[JMS specs]
Sun microsystens - Java Message Service - Version 1.1 April 12, 2002

[JMS]
Snyder Bosanac Davies - ActiveMQ in action - Manning

[GeoServer]
http://docs.geoserver.org/

[GeoServer REST]
http://docs.geoserver.org/latest/en/user/restconfig/rest-config-api.html

[ActiveMQ]
http://activemq.apache.org/