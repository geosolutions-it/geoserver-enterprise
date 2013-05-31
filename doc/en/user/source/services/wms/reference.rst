.. _wms_reference: 

WMS reference
============= 

Introduction
------------ 

The OGC `Web Map Service <http://www.opengeospatial.org/standards/wms>`_ (WMS) specification 
defines an HTTP interface for requesting georeferenced map images from a server.  
GeoServer supports WMS 1.1.1, the most widely used version of WMS, as well as WMS 1.3.0.

The relevant OGC WMS specifications are:

- `OGC Web Map Service Implementation Specification, Version 1.1.1 <http://portal.opengeospatial.org/files/?artifact_id=1081&version=1&format=pdf>`_
- `OGC Web Map Service Implementation Specification, Version 1.3.0 <http://portal.opengeospatial.org/files/?artifact_id=14416>`_
 
GeoServer also supports some extensions to the WMS specification made by the Styled Layer Descriptor (SLD) standard to control the styling of the map output.

Benefits of WMS
--------------- 

WMS provides a standard interface for requesting a geospatial map image.  The benefit of this is that WMS clients can request images from multiple WMS servers, and then combine them into a single view for the user.  The standard guarantees that these images can all be overlaid on one another as they actually would be in reality.  Numerous servers and clients support WMS.

Operations
---------- 

WMS requests can perform the following operations: 

.. list-table::
   :widths: 20 80

   * - **Operation**
     - **Description**
   * - ``Exceptions``
     - If an exception occur
   * - ``GetCapabilities``
     - Retrieves metadata about the service, including supported operations and parameters, and a list of the available layers
   * - ``GetMap``
     - Retrieves a map image for a specified area and content
   * - ``GetFeatureInfo`` (optional)
     - Retrieves the underlying data, including geometry and attribute values, for a pixel location on a map
   * - ``DescribeLayer`` (optional)
     - Indicates the WFS or WCS to retrieve additional information about the layer.
   * - ``GetLegendGraphic`` (optional)
     - Retrieves a generated legend for a map 

Exceptions
----------

Formats in which WMS can report exceptions. The supported values for exceptions are:

.. list-table::
   :widths: 15 35 50
   
   * - **Format**
     - **Syntax**
     - **Notes**
   * - XML
     - ``EXCEPTIONS=application/vnd.ogc.se_xml``
     - Xml output. (The default format)
   * - PNG
     - ``EXCEPTIONS=application/vnd.ogc.inimage``
     - Generates an image
   * - Blank
     - ``EXCEPTIONS=application/vnd.ogc.se_blank``
     - Generates a blank image
   * - JSON
     - ``EXCEPTIONS=application/json``
     - Simple Json representation.
   * - JSONP
     - ``EXCEPTIONS=text/javascript``
     - Return a JsonP in the form: paddingOutput(...jsonp...). See :ref:`wms_vendor_parameters` to change the callback name. Note that this format is disabled by default (See :ref:`wms_global_variables`).

.. _wms_getcap:

GetCapabilities
---------------

The **GetCapabilities** operation requests metadata about the operations, services, and data ("capabilities") that are offered by a WMS server. 

The parameters for the GetCapabilities operation are:

.. list-table::
   :widths: 20 10 70
   
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``service``
     - Yes
     - Service name. Value is ``WMS``.
   * - ``version``
     - Yes
     - Service version. Value is one of ``1.0.0``, ``1.1.0``, ``1.1.1``, ``1.3``.
   * - ``request``
     - Yes
     - Operation name. Value is ``GetCapabilities``.

A example GetCapabilities request is:

.. code-block:: xml
 
   http://localhost:8080/geoserver/wms?
   service=wms&
   version=1.1.1&
   request=GetCapabilities
	  
There are three parameters being passed to the WMS server, ``service=wms``, ``version=1.1.1``, and ``request=GetCapabilities``.  
The standard requires that a WMS request have these three parameters (``service``, ``version``, and ``request``).  
GeoServer relaxes these requirements (setting the default version if omitted), but "officially" they are mandatory, so they should always be included.  
The *service* key tells the WMS server that a WMS request is forthcoming.  
The *version* key refers to which version of WMS is being requested.  
The *request* key specifies the GetCapabilities operation.

The response is a Capabilities XML document that is a detailed description of the WMS service.  
It contains three main sections:

.. list-table::
   :widths: 20 80
   
   * - **Service**
     - Contains service metadata such as the service name, keywords, and contact information for the organization operating the server.
   * - **Request**
     - Describes the operations the WMS service provides and the parameters and output formats for each operation.  
       If desired GeoServer can be configured to disable support for certain WMS operations.
   * - **Layer**
     - Lists the available coordinate systems and layers.  
       In GeoServer layers are named in the form "namespace:layer".  
       Each layer provides service metadata such as title, abstract and keywords.

.. _wms_getmap:

GetMap
------

The **GetMap** operation requests that the server generate a map.  
The core parameters specify one or more layers and styles to appear on the map,
a bounding box for the map extent,
a target spatial reference system,
and a width, height, and format for the output, 
The response is a map image, or other map output artifact, depending on the format requested.
GeoServer provides a wide variety of output formats, described in :ref:`wms_output_formats`.

The information needed to specify values for parameters such as ``layers``, ``styles`` and ``srs`` are supplied by the Capabilities document.  

A good way to get to know the GetMap parameters is to experiment with the :ref:`tutorials_wmsreflector`.  

The standard parameters for the GetMap operation are:

.. list-table::
   :widths: 20 10 70
   
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``service``
     - Yes
     - Service name. Value is ``WMS``.
   * - ``version``
     - Yes
     - Service version. Value is one of ``1.0.0``, ``1.1.0``, ``1.1.1``, ``1.3``.
   * - ``request``
     - Yes
     - Operation name. Value is ``GetMap``.
   * - ``layers``
     - Yes
     - Layers to display on map.  
       Value is a comma-separated list of layer names.
   * - ``styles``
     - Yes
     - Styles in which layers are to be rendered.  
       Value is a comma-separated list of style names,
       or empty if default styling is required.
       Style names may be empty in the list.
   * - ``srs`` *or* ``crs``
     - Yes
     - Spatial Reference System for map output.
       Value is in form ``EPSG:nnn``.
       ``crs`` is the parameter key used in WMS 1.3.0. 
   * - ``bbox``
     - Yes
     - Bounding box for map extent.
       Value is ``minx,miny,maxx,maxy`` in units of the SRS.
   * - ``width``
     - Yes
     - Width of map output, in pixels.
   * - ``height``
     - Yes
     - Height of map output, in pixels.
   * - ``format``
     - Yes
     - Format for the map output.  
       See :ref:`wms_output_formats` for supported values.
   * - ``transparent``
     - No
     - Whether the map background should be transparent.
       Values are ``true`` or ``false``.
       Default is ``false``
   * - ``bgcolor``
     - No
     - Background color for the map image.
       Value is in the form ``RRGGBB``.
       Default is ``FFFFFF`` (white).
   * - ``exceptions``
     - No
     - Format in which to report exceptions.
       Default value is ``application/vnd.ogc.se_xml``. 
       Other valid values are ``application/vnd.ogc.inimage`` and ``application/vnd.ogc.se_blank``.
   * - ``sld``
     - No
     - A URL referencing a :ref:`StyledLayerDescriptor <styling>` XML file
       which controls or enhances map layers and styling
   * - ``sld_body``
     - No
     - A URL-encoded :ref:`StyledLayerDescriptor <styling>` XML document
       which controls or enhances map layers and styling     

       
GeoServer provides a number of useful vendor-specific parameters, which are documented in the :ref:`wms_vendor_parameters` section.

An example request for a PNG map image using default styling is:

.. code-block:: xml

   http://localhost:8080/geoserver/wms?
   request=GetMap
   &service=WMS
   &version=1.1.1
   &layers=topp%3Astates
   &styles=
   &srs=EPSG%3A4326
   &bbox=-145.15104058007,21.731919794922,-57.154894212888,58.961058642578&
   &width=780
   &height=330
   &format=image%2Fpng


.. _wms_getfeatureinfo:

GetFeatureInfo
--------------

The **GetFeatureInfo** operation requests the spatial and attribute data for the features
at a given location on a map.  
It is similar to the WFS **GetFeature** operation, but that operation provides more flexibility in both input and output.
Since GeoServer provides a WFS we recommend using it instead of ``GetFeatureInfo`` whenever possible.  
 
The one advantage of ``GetFeatureInfo`` is that the request uses an (x,y) pixel value from a returned WMS image.  
This is easier to use for a naive client that is not able to perform the geographic referencing otherwise needed.

The standard parameters for the GetFeatureInfo operation are:

.. list-table::
   :widths: 20 10 70
   
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``service``
     - Yes
     - Service name. Value is ``WMS``.
   * - ``version``
     - Yes
     - Service version. Value is one of ``1.0.0``, ``1.1.0``, ``1.1.1``, ``1.3``.
   * - ``request``
     - Yes
     - Operation name. Value is ``GetFeatureInfo``.
   * - ``layers``
     - Yes
     - See :ref:`wms_getmap`
   * - ``styles``
     - Yes
     - See :ref:`wms_getmap`
   * - ``srs`` *or* ``crs``
     - Yes
     - See :ref:`wms_getmap`
   * - ``bbox``
     - Yes
     - See :ref:`wms_getmap`
   * - ``width``
     - Yes
     - See :ref:`wms_getmap`
   * - ``height``
     - Yes
     - See :ref:`wms_getmap`
   * - ``query_layers``
     - Yes
     - Comma-separated list of one or more layers to query.
   * - ``info_format``
     - No
     - Format for the feature information response.  See below for values.
   * - ``feature_count``
     - No
     - Maximum number of features to return.
       Default is 1.
   * - ``x`` or ``i``
     - Yes
     - X ordinate of query point on map, in pixels. 0 is left side.
       ``i`` is the parameter key used in WMS 1.3.0.
   * - ``y`` or ``j``
     - Yes
     - Y ordinate of query point on map, in pixels. 0 is the top.
       ``j`` is the parameter key used in WMS 1.3.0.
   * - ``exceptions``
     - No
     - Format in which to report exceptions.
       The default value is ``application/vnd.ogc.se_xml``.

Geoserver supports a number of output formats for the ``GetFeatureInfo`` response.
Server-styled HTML is the most commonly-used format. 
For maximum control and customisation the client should use GML3 and style the raw data itself.
The supported formats are:

.. list-table::
   :widths: 15 35 50
   
   * - **Format**
     - **Syntax**
     - **Notes**
   * - TEXT
     - ``info_format=text/plain``
     - Simple text output. (The default format)
   * - GML 2
     - ``info_format=application/vnd.ogc.wms`` 
     - Works only for Simple Features (see :ref:`app-schema.complex-features`)
   * - GML 3
     - ``info_format=application/vnd.ogc.wms/3.1.1``
     - Works for both Simple and Complex Features (see :ref:`app-schema.complex-features`)
   * - HTML
     - ``info_format=text/html``
     - Uses HTML templates that are defined on the server. See :ref:`tutorials_getfeatureinfo` for information on how to template HTML output. 
   * - JSON
     - ``info_format=application/json``
     - Simple Json representation.
   * - JSONP
     - ``info_format=text/javascript``
     - Returns a JsonP in the form: ``parseResponse(...json...)``. See :ref:`wms_vendor_parameters` to change the callback name. Note that this format is disabled by default (See :ref:`wms_global_variables`).

GeoServer provides the following vendor-specific parameters
for the GetFeatureInfo operation.
They are fully documented in the :ref:`wms_vendor_parameters` section.

.. list-table::
   :widths: 20 10 70
   
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``buffer``
     - No
     - width of search radius around query point.
   * - ``cql_filter``
     - No
     - Filter for returned data, in ECQL format
   * - ``filter``
     - No
     - Filter for returned data, in OGC Filter format
   * - ``propertyName``
     - No
     - Feature properties to be returned

An example request for feature information from the ``topp:states`` layer in HTML format is:

.. code-block:: xml

   http://localhost:8080/geoserver/wms?
   request=GetFeatureInfo
   &service=WMS
   &version=1.1.1
   &layers=topp%3Astates
   &styles=
   &srs=EPSG%3A4326
   &format=image%2Fpng
   &bbox=-145.151041%2C21.73192%2C-57.154894%2C58.961059
   &width=780
   &height=330
   &query_layers=topp%3Astates
   &info_format=text%2Fhtml
   &feature_count=50
   &x=353
   &y=145
   &exceptions=application%2Fvnd.ogc.se_xml

An example request for feature information in GeoJSON format is:

.. code-block:: xml

   http://localhost:8080/geoserver/wms?
   &INFO_FORMAT=application/json
   &REQUEST=GetFeatureInfo
   &EXCEPTIONS=application/vnd.ogc.se_xml
   &SERVICE=WMS
   &VERSION=1.1.1
   &WIDTH=970&HEIGHT=485&X=486&Y=165&BBOX=-180,-90,180,90
   &LAYERS=COUNTRYPROFILES:grp_administrative_map
   &QUERY_LAYERS=COUNTRYPROFILES:grp_administrative_map
   &TYPENAME=COUNTRYPROFILES:grp_administrative_map

The result will be:

.. code-block:: xml
   
   {
   "type":"FeatureCollection",
   "features":[
      {
         "type":"Feature",
         "id":"dt_gaul_geom.fid-138e3070879",
         "geometry":{
            "type":"MultiPolygon",
            "coordinates":[
               [
                  [
                     [
                        XXXXXXXXXX,
                        XXXXXXXXXX
                     ],
                     ...
                     [
                        XXXXXXXXXX,
                        XXXXXXXXXX
                     ]
                  ]
               ]
            ]
         },
         "geometry_name":"at_geom",
         "properties":{
            "bk_gaul":X,
            "at_admlevel":0,
            "at_iso3":"XXX",
            "ia_name":"XXXX",
            "at_gaul_l0":X,
            "bbox":[
               XXXX,
               XXXX,
               XXXX,
               XXXX
            ]
         }
      }
   ],
   "crs":{
      "type":"EPSG",
      "properties":{
         "code":"4326"
      }
   },
   "bbox":[
      XXXX,
      XXXX,
      XXXX,
      XXXX
   ]
   }


.. _wms_describelayer:

DescribeLayer
-------------

The **DescribeLayer** operation is used primarily by clients that understand SLD-based WMS.  
In order to make an SLD one needs to know the structure of the data.  
WMS and WFS both have operations to do this, so the **DescribeLayer** operation just routes the client to the appropriate service.

The standard parameters for the DescribeLayer operation are:

.. list-table::
   :widths: 20 10 70
   
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``service``
     - Yes
     - Service name. Value is ``WMS``.
   * - ``version``
     - Yes
     - Service version. Value is ``1.1.1``.
   * - ``request``
     - Yes
     - Operation name. Value is ``DescribeLayer``.
   * - ``layers``
     - Yes
     - See :ref:`wms_getmap`
   * - ``exceptions``
     - No
     - Format in which to report exceptions.
       The default value is ``application/vnd.ogc.se_xml``.

Geoserver supports a number of output formats for the ``DescribeLayer`` response.
Server-styled HTML is the most commonly-used format. 
The supported formats are:

.. list-table::
   :widths: 15 35 50
   
   * - **Format**
     - **Syntax**
     - **Notes**
   * - TEXT
     - ``output_format=text/xml``
     - Same as default.
   * - GML 2
     - ``output_format=application/vnd.ogc.wms_xml``
     - The default format.
   * - JSON
     - ``output_format=application/json``
     - Simple Json representation.
   * - JSONP
     - ``output_format=text/javascript``
     - Return a JsonP in the form: paddingOutput(...jsonp...). See :ref:`wms_vendor_parameters` to change the callback name.  Note that this format is disabled by default (See :ref:`wms_global_variables`).
     

An example request in XML (default) format on a layer is:

.. code-block:: xml

   http://localhost:8080/geoserver/topp/wms?service=WMS
   &version=1.1.1
   &request=DescribeLayer
   &layers=topp:coverage

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <!DOCTYPE WMS_DescribeLayerResponse SYSTEM "http://localhost:8080/geoserver/schemas/wms/1.1.1/WMS_DescribeLayerResponse.dtd">
   <WMS_DescribeLayerResponse version="1.1.1">
      <LayerDescription name="topp:coverage" owsURL="http://localhost:8080/geoserver/topp/wcs?" owsType="WCS">
         <Query typeName="topp:coverage"/>
      </LayerDescription>
   </WMS_DescribeLayerResponse>

An example request for feature description in JSON format on a layer group is:

.. code-block:: xml

   http://localhost:8080/geoserver/wms?service=WMS
   &version=1.1.1
   &request=DescribeLayer
   &layers=sf:roads,topp:tasmania_roads,nurc:mosaic
   &outputFormat=application/json
   

The result will be:

.. code-block:: xml

   {
   version: "1.1.1",
   layerDescriptions: [
   {
      layerName: "sf:roads",
      owsURL: "http://localhost:8080/geoserver/wfs/WfsDispatcher?",
      owsType: "WFS",
      typeName: "sf:roads"
   },
   {
      layerName: "topp:tasmania_roads",
      owsURL: "http://localhost:8080/geoserver/wfs/WfsDispatcher?",
      owsType: "WFS",
      typeName: "topp:tasmania_roads"
   },
   {
      layerName: "nurc:mosaic",
      owsURL: "http://localhost:8080/geoserver/wcs?",
      owsType: "WCS",
      typeName: "nurc:mosaic"
   }
   ]
   }


.. _wms_getlegendgraphic:

GetLegendGraphic
----------------

The **GetLegendGraphic** operation provides a mechanism for generating legend graphics as images, beyond the LegendURL reference of WMS Capabilities.  
It generates a legend based on the style defined on the server, or alternatively based on a user-supplied SLD.  
For more information on this operation and the various options that GeoServer supports see :ref:`get_legend_graphic`.
