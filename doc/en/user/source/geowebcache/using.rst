.. _gwc_using:

Using GeoWebCache
=================

.. note:: For an more in-depth discussion of using GeoWebCache, please see the `GeoWebCache documentation <http://geowebcache.org/docs/>`_.

.. _gwc_directwms:

Direct integration with GeoServer WMS
-------------------------------------

GeoWebCache can be transparently integrated with the GeoServer WMS, and so requires no special endpoint or custom URL. In this way one can have the simplicity of a standard WMS endpoint with the performance of a tiled client.

Although this direct integration is disabled by default, it can be enabled by going to the :ref:`webadmin_tilecaching_defaults` page in the :ref:`web_admin`.

When this feature is enabled, GeoServer WMS will cache and retrieve tiles from GeoWebCache (via a GetMap request) only if **all of the following criteria are followed**:

* WMS Direct integration is enabled (you can set this on the :ref:`webadmin_tilecaching_defaults` page)
* ``tiled=true`` is included in the request
* The request only references a single layer
* Caching is enabled for that layer
* The image requested is of the same height and width as the size saved in the layer configuration
* The requested CRS matches one of the available tile layer gridsets
* The image requested lines up with the existing grid bounds
* A parameter is included for which there is a corresponding Parameter Filter

In addition, when direct integration is enabled, the WMS capabilities document (via a GetCapabilities request) will only return the WMS-C vendor-specific capabilities elements (such as a ``<TileSet>`` element for each cached layer/CRS/format combination) if ``tiled=true`` is appended to the GetCapabilities request.

.. note:: For more information on WMS-C, please see the `WMS Tiling Client Recommendation <http://wiki.osgeo.org/wiki/WMS_Tiling_Client_Recommendation>`_ from OSGeo.

.. note:: GeoWebCache integration is not compatible with the OpenLayers-based :ref:`layerpreview`, as the preview does not usually align with the GeoWebCache layer gridset. This is because the OpenLayers application calculates the ``tileorigin`` based on the layer's bounding box, which is different from the gridset. It is, possible to create an OpenLayers application that caches tiles; just make sure that the ``tileorigin`` aligns with the gridset.


Virtual services
~~~~~~~~~~~~~~~~

When direct WMS integration is enabled, GeoWebCache will properly handle requests to :ref:`virtual_services` (``/geoserver/<workspace>/wms?tiled=true&...``). 

Virtual services capabilities documents will contain ``<TileSet>`` entries only for the layers that belong to that workspace (and global layer groups), and will be referenced by unqualified layer names (no namespace). For example, the layer ``topp:states`` will be referred to as ``<Layers>states</Layers>`` instead of ``<Layers>topp:states</Layers>``, and GetMap requests to the virtual services endpoint using ``LAYERS=states`` will properly be handled.

Supported parameter filters
~~~~~~~~~~~~~~~~~~~~~~~~~~~

With direct WMS integration, the following parameter filters are supported for GetMap requests: 

* ``ANGLE``
* ``BGCOLOR``
* ``BUFFER``
* ``CQL_FILTER``
* ``ELEVATION``
* ``ENV``
* ``FEATUREID``
* ``FEATUREVERSION``
* ``FILTER``
* ``FORMAT_OPTIONS``
* ``MAXFEATURES``
* ``PALETTE``
* ``STARTINDEX``
* ``TIME``
* ``VIEWPARAMS``

If a request is made using any of the above parameters, the request will be passed to GeoServer, unless a parameter filter has been set up, in which case GeoWebCache will process the request.


.. _gwc_endpoint:

GeoWebCache endpoint URL
------------------------

When not using direct integration, you can point your client directly to GeoWebCache.

.. warning:: GeoWebCache is not a true WMS, and so the following is an oversimplification. If you encounter errors, see the :ref:`gwc_troubleshooting` page for help. 

To direct your client to GeoWebCache (and thus receive cached tiles) you need to change the WMS URL.

If your application requests WMS tiles from GeoServer at this URL::

   http://example.com/geoserver/wms

You can invoke the GeoWebCache WMS instead at this URL::

   http://example.com/geoserver/gwc/service/wms
   
In other words, add ``/gwc/service/wms`` in between the path to your GeoServer instance and the WMS call.

As soon as tiles are requested through GeoWebCache, GeoWebCache automatically starts saving them. This means that initial requests for tiles will not be accelerated since GeoServer will still need to generate the tiles. To automate this process of requesting tiles, you can **seed** the cache. See the section on :ref:`gwc_seeding` for more details.

.. _gwc_diskquota:

Disk quota
----------

GeoWebCache has a built-in disk quota feature to prevent disk space from growing unbounded. You can set the maximum size of the cache directory, poll interval, and what policy of tile removal to use when the quota is exceeded. Tiles can be removed based on usage ("Least Frequently Used" or LFU) or timestamp ("Least Recently Used" or LRU).

Disk quotas are turned off by default, but can be configured on the :ref:`webadmin_tilecaching_diskquotas` page in the :ref:`web_admin`. 

Integration with external mapping sites
---------------------------------------

The documentation on the `GeoWebCache homepage <http://geowebcache.org>`_ contains examples for creating applications that integrate with Google Maps, Google Earth, Bing Maps, and more. 

Support for custom projections
------------------------------

The version of GeoWebCache that comes embedded in GeoServer automatically configures every layer served in GeoServer with the two most common projections:

* **EPSG:4326** (latitude/longitude)
* **EPSG:900913** (Spherical Mercator, the projection used in Google Maps)

You can also set a custom CRS from any that GeoServer recognizes. See the :ref:`webadmin_tilecaching_gridsets` page for details. 

