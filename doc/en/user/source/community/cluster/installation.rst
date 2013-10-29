.. module:: jms.installation

.. _jms.installation:

Installation of the JMS Cluster modules
=======================================

To install the JMS Cluster modules you have to add some jars into the GeoServer's library folder (WEB-INF/libs) following the steps below.

GeoServer JMS Commons module
----------------------------

You need to add the following dependencies:

.. code-block:: xml

  jms-commons-2.2-CIOK-SNAPSHOT.jar
  jms-api-1.1-rev-1.jar
  xbean-spring-3.5.jar
  spring-jms-3.1.4.RELEASE.jar

Spring also need to be updated to 3.1.4.RELEASE :

.. code-block:: xml

  spring-aop-3.1.4.RELEASE.jar
  spring-tx-3.1.4.RELEASE.jar
  spring-beans-3.1.4.RELEASE.jar
  spring-core-3.1.4.RELEASE.jar
  spring-asm-3.1.4.RELEASE.jar
  spring-context-3.1.4.RELEASE.jar
  spring-context-support-3.1.4.RELEASE.jar
  spring-expression-3.1.4.RELEASE.jar
  spring-webmvc-3.1.4.RELEASE.jar
  spring-web-3.1.4.RELEASE.jar
  spring-tx-3.1.4.RELEASE.jar
  spring-jdbc-3.1.4.RELEASE.jar
  
**NOTE:**
  Once you have added the above list of jars remember to remove the corresponding old jars (version 3.1.1.RELEASE)

While spring security can still remain the same:

.. code-block:: xml

  spring-security-core-3.1.0.RELEASE.jar
  spring-security-crypto-3.1.0.RELEASE.jar
  spring-security-web-3.1.0.RELEASE.jar
  spring-security-config-3.1.0.RELEASE.jar


GeoServer JMS module
--------------------

The Geoserver JMS module adds the following deps

.. code-block:: xml

  jms-geoserver-2.2-CIOK-SNAPSHOT.jar

GeoServer JMS ActiveMQ module
-----------------------------

The Geoserver JMS ActiveMQ module adds the following deps:

.. code-block:: xml

  jms-activeMQ-2.2-CIOK-SNAPSHOT.jar

plus ActiveMQ specific dependencies:

.. code-block:: xml

  activemq-all-5.8.0.jar
  activemq-amqp-5.8.0.jar
  activemq-broker-5.8.0.jar
  activemq-client-5.8.0.jar
  specs:geronimo-jms_1.1_spec-1.1.1.jar
  specs:geronimo-j2ee-management_1.1_spec-1.0.1.jar
  activemq-openwire-legacy-5.8.0.jar
  proton-jms-0.3.0-fuse-2.jar
  proton-0.3.0-fuse-2.jar
  proton-api-0.3.0-fuse-2.jar
  bcpkix-jdk15on-1.47.jar
  bcprov-jdk15on-1.47.jar
  hawtbuf-1.9.jar

  