.. _gwc_config:

Configuration
=============

GeoWebCache is automatically configured for use with GeoServer using the most common options, with no setup required. All communication between GeoServer and GeoWebCache happens by passing messages inside the JVM.

By default, all layers served by GeoServer will be known to GeoWebCache. See the :ref:`webadmin_tilecaching_layers` page to test the configuration.

.. note:: Version 2.2.0 of GeoServer introduced changes to the configuration of the integrated GeoWebCache.

Integrated user interface
-------------------------

GeoWebCache has a full integrated web-based configuration. See the :ref:`webadmin_tilecaching` section in the :ref:`web_admin`.

Determining tiled layers
------------------------

In versions of GeoServer prior to 2.2.0, the GeoWebCache integration was done in a such way that every GeoServer layer and layer group was forced to have an associated GeoWebCache tile layer. In addition, every such tile layer was forcedly published in the EPSG:900913 and EPSG:4326 gridsets with PNG and JPEG output formats.

It is possible to selectively turn caching on or off for any layer served through GeoServer. This setting can be configured in the :ref:`webadmin_tilecaching_layers` section of the :ref:`web_admin`.

Configuration files
-------------------

It is possible to configure most aspects of cached layers through the :ref:`webadmin_tilecaching` section in the :ref:`web_admin` or the :ref:`gwc_rest`. 

GeoWebCache keeps the configuration for each GeoServer tiled layer separately, inside the :file:`<data_dir>/gwc-layers/` directory. There is one XML file for each tile layer. These files contain a different syntax from the ``<wmsLayer>`` syntax in the standalone version and are *not* meant to be edited by hand. Instead you can configure tile layers on the :ref:`webadmin_tilecaching_layers` page or through the :ref:`gwc_rest`.

Configuration for the defined gridsets is saved in :file:`<data_dir>/gwc/geowebcache.xml`` so that the integrated GeoWebCache can continue to serve externally-defined tile layers from WMS services outside GeoServer.

If upgrading from a version prior to 2.2.0, a migration process is run which creates a tile layer configuration for all the available layers and layer groups in GeoServer with the old defaults. From that point on, you should configure the tile layers on the :ref:`webadmin_tilecaching_layers` page.


Changing the cache directory
----------------------------

GeoWebCache will automatically store cached tiles in a ``gwc`` directory inside your GeoServer data directory. To set a different directory, stop GeoServer (if it is running) and add the following code to your GeoServer :file:`web.xml` file (located in the :file:`WEB-INF` directory):

.. code-block:: xml 

   <context-param>
      <param-name>GEOWEBCACHE_CACHE_DIR</param-name>
      <param-value>C:\temp</param-value>
   </context-param>

Change the path inside ``<param-value>`` to the desired cache path (such as :file:`C:\\temp` or :file:`/tmp`). Restart GeoServer when done.

.. note:: Make sure GeoServer has write access in this directory.

GeoWebCache with multiple GeoServer instances
---------------------------------------------

For stability reasons, it is not recommended to use the embedded GeoWebCache with multiple GeoServer instances. If you want to configure GeoWebCache as a front-end for multiple instances of GeoServer, we recommend using the `standalone GeoWebCache <http://geowebcache.org>`_.


