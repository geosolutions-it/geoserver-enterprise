.. _wfs_output_formats:


WFS output formats
==================

WFS returns features and feature information in a number of formats. The syntax for specifying an output format is::

   outputFormat=<format>

where ``<format>`` is one of the following options:

.. list-table::
   :widths: 15 30 55
   :header-rows: 1
   
   * - Format
     - Syntax
     - Notes
   * - GML2
     - ``outputFormat=GML2``
     - Default option for WFS 1.0.0
   * - GML3
     - ``outputFormat=GML3``
     - Default option for WFS 1.1.0 and 2.0.0
   * - Shapefile
     - ``outputFormat=shape-zip``
     - ZIP archive will be generated containing the shapefile (see :ref:`wfs_outputformat_shapezip` below)
   * - JSON
     - ``outputFormat=application/json``
     - Returns a GeoJSON or a JSON output. Note ``outputFormat=json`` is only supported for getFeature (for backward compatibility).
   * - JSONP
     - ``outputFormat=text/javascript``
     - Returns a `JSONP <http://en.wikipedia.org/wiki/JSONP>`_ in the form: ``parseResponse(...json...)``. See :ref:`wms_vendor_parameters` to change the callback name. Note that this format is disabled by default (See :ref:`wms_global_variables`).
   * - CSV
     - ``outputFormat=csv``
     - Returns a CSV (comma-separated values) file

.. note:: Some additional output formats (such as :ref:`Excel <excel_extension>`) are available with the use of an extension. The full list of output formats supported by a particular GeoServer instance can be found by performing a WFS :ref:`wfs_getcap` request.
     
.. _wfs_outputformat_shapezip:

Shapefile output customization
------------------------------

The shapefile output format output can be customized by preparing a :ref:`Freemarker template <tutorial_freemarkertemplate>` which will configure the file name of the archive (ZIP file) and the files it contains. The default template is:

::

  zip=${typename}
  shp=${typename}${geometryType}
  txt=wfsrequest

The ``zip`` property is the name of the archive, the ``shp`` property is the name of the shapefile for a given feature type, and ``txt`` is the dump of the actual WFS request.

The properties available in the template are:
  
  * ``typename``—Feature type name (for the ``zip`` property this will be the first feature type if the request contains many feature types)
  * ``geometryType``—Type of geometry contained in the shapefile. This is only used if the output geometry type is generic and the various geometries are stored in one shapefile per type.
  * ``workspace``—Workspace of the feature type
  * ``timestamp``—Date object with the request timestamp
  * ``iso_timestamp``—String (ISO timestamp of the request at GMT) in ``yyyyMMdd_HHmmss`` format
  
Format options as parameter in WFS requests
-------------------------------------------

GeoServer provides the ``format_options`` vendor-specific parameter to specify parameters that are specific to each format. The syntax is:

::

    format-options=param1:value1;param2:value2;...
	
The currently supported format option in WFS output is:

  * ``filename``—Applies only to the SHAPE-ZIP output format. If a file name is provided, the name is used as the output file name. For example, ``format_options=filename:roads.zip``. If a file name is not specified, the output file name is inferred from the requested feature type name.

