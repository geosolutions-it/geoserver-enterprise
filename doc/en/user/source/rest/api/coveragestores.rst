.. _rest_api_coveragestores:

Coverage stores
===============

A ``coverage store`` contains raster format spatial data.

``/workspaces/<ws>/coveragestores[.<format>]``
----------------------------------------------

Controls all coverage stores in a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all coverage stores in workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - Create a new coverage store
     - 201 with ``Location`` header 
     - XML, JSON
     - 
   * - PUT
     -
     - 405
     -
     -
   * - DELETE
     -
     - 405
     -
     -


``/workspaces/<ws>/coveragestores/<cs>[.<format>]``
---------------------------------------------------

Controls a particular coverage store in a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return coverage store ``cs``
     - 200
     - HTML, XML, JSON
     - HTML
     -
   * - POST
     - 
     - 405
     - 
     -
     - 
   * - PUT
     - Modify coverage store ``cs``
     -
     -
     -
     -
   * - DELETE
     - Delete coverage store ``cs``
     -
     -
     -
     - :ref:`recurse <rest_api_coveragestores_recurse>`

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a coverage store that does not exist
     - 404
   * - PUT that changes name of coverage store
     - 403
   * - PUT that changes workspace of coverage store
     - 403
   * - DELETE against a coverage store that contains configured coverage
     - 403

Parameters
~~~~~~~~~~

.. _rest_api_coveragestores_recurse:

``recurse``
^^^^^^^^^^^

The ``recurse`` parameter recursively deletes all layers referenced by the coverage store. Allowed values for this parameter are "true" or "false". The default value is "false".


``/workspaces/<ws>/coveragestores/<cs>/file[.<extension>]``
-----------------------------------------------------------

This end point allows a file containing spatial data to be added (via a POST or PUT) into an existing coverage store, or will create a new coverage store if it doesn't already exist.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - *Deprecated*. Get the underlying files for the coverage store as a zip file with MIME type ``application/zip``.
     - 200
     - 
     - 
     - 
   * - POST
     - 
     - 405
     - 
     - 
     - :ref:`recalculate <rest_api_coveragestores_recalculate>`
   * - PUT
     - Creates or overwrites the files for coverage store ``cs``
     - 200
     - :ref:`See note below <rest_api_coveragestores_file_put>`
     - 
     - :ref:`configure <rest_api_coveragestores_configure>`, :ref:`coverageName <rest_api_coveragestores_coveragename>`
   * - DELETE
     -
     - 405
     -
     -
     -

.. _rest_api_coveragestores_file_put:

.. note::

   A file can be PUT to a coverage store as a standalone or zipped archive file. Standalone files are only suitable for coverage stores that work with a single file such as GeoTIFF store. Coverage stores that work with multiple files, such as the ImageMosaic store, must be sent as a zip archive.

   When uploading a standalone file, set the ``Content-type`` appropriately based on the file type. If you are loading a zip archive, set the ``Content-type`` to ``application\zip``.

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a data store that does not exist
     - 404
   * - GET for a data store that is not file based
     - 404

Parameters
~~~~~~~~~~

``extension``
^^^^^^^^^^^^^

The ``extension`` parameter specifies the type of coverage store. The
following extensions are supported:

.. list-table::
   :header-rows: 1

   * - Extension
     - Coverage store
   * - geotiff
     - GeoTIFF
   * - worldimage
     - Georeferenced image (JPEG, PNG, TIFF)
   * - imagemosaic
     - Image mosaic

.. _rest_api_coveragestores_configure:

``configure``
^^^^^^^^^^^^^

The ``configure`` parameter controls how the coverage store is configured upon file upload. It can take one of the three values:

* ``first``—(*Default*) Only setup the first feature type available in the coverage store.
* ``none``—Do not configure any feature types.
* ``all``—Configure all feature types.

.. _rest_api_coveragestores_coveragename:

``coverageName``
^^^^^^^^^^^^^^^^

The ``coverageName`` parameter specifies the name of the coverage within the coverage store. This parameter is only relevant if the ``configure`` parameter is not equal to "none". If not specified the resulting coverage will receive the same name as its containing coverage store.

.. note:: At present a one-to-one relationship exists between a coverage store and a coverage. However, there are plans to support multidimensional coverages, so this parameter may change.

.. _rest_api_coveragestores_recalculate:

``recalculate``
^^^^^^^^^^^^^^^

The ``recalculate`` parameter specifies whether to recalculate any bounding boxes for a coverage. Some properties of coverages are automatically recalculated when necessary. In particular, the native bounding box is recalculated when the projection or projection policy is changed. The lat/long bounding box is recalculated when the native bounding box is recalculated or when a new native bounding box is explicitly provided in the request. (The native and lat/long bounding boxes are not automatically recalculated when they are explicitly included in the request.) In addition, the client may explicitly request a fixed set of fields to calculate, by including a comma-separated list of their names in the ``recalculate`` parameter. For example:

* ``recalculate=`` (empty parameter)—Do not calculate any fields, regardless of the projection, projection policy, etc. This might be useful to avoid slow recalculation when operating against large datasets.
* ``recalculate=nativebbox``—Recalculate the native bounding box, but do not recalculate the lat/long bounding box.
* ``recalculate=nativebbox,latlonbbox``—Recalculate both the native bounding box and the lat/long bounding box.

