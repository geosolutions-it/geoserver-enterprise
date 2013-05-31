.. _wps_install:

Installing the WPS extension
============================

The WPS module is not a part of GeoServer core, but instead must be installed as an extension.  To install WPS:

#. Navigate to the `GeoServer download page <http://geoserver.org/display/GEOS/Download>`_

#. Find the page that matches the version of the running GeoServer.

   .. warning::  Be sure to match the version of the extension with that of GeoServer, otherwise errors will occur.

#. Download the WPS extension.  The download link for :guilabel:`WPS` will be in the :guilabel:`Extensions` section under :guilabel:`Other`.

#. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

#. Restart GeoServer.

After restarting, load the :ref:`web_admin`.  If the extension loaded properly, you should see an extra entry for WPS in the :guilabel:`Service Capabilities` column.  If you don't see this entry, check the logs for errors.

.. figure:: images/wpscapslink.png
   :align: center

   *A link for the WPS capabilities document will display if installed properly*