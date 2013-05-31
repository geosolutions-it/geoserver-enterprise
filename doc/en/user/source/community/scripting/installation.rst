.. _scripting_installation:

Installing the Scripting Extension
==================================

.. note:: The various language runtime libraries increase GeoServer's memory footprint, specifically the "PermGen" (Permanent Generation) space. When installing the scripting extension we recommended that you increase PermGen capacity to 256m. This is done with the option ``-XX:MaxPermSize=256m``. If installing multiple language extensions this size may need to be increased even further.

Python
------

Currently, the only scripting language that is distributed as a package for download is Python. This extension is a community extension, in that it is not included with the list of extensions on the standard `GeoServer download page <http://geoserver.org/display/GEOS/Download>`_. Instead, the community extensions are built each night on the `nightly build server <http://gridlock.opengeo.org/geoserver/>`_.

To access the Python scripting extension:

#. Navigate to the `nightly build server <http://gridlock.opengeo.org/geoserver/>`_.

#. Click the folder that contains the correct branch of GeoServer for your version (for example: for 2.2.2, click on :guilabel:`2.2.x`):

#. Click :guilabel:`community-latest`. This folder contains the most recently built community extensions for the branch.

#. Download the file that contains the string "python". For example: :file:`geoserver-8-ENTERPRISE-SNAPSHOT-python-plugin.zip`.

#. Extract the contents of the archive into the :file:`/WEB-INF/lib/` directory of GeoServer. For example, if GeoServer was installed at :file:`/opt/geoserver-2.2.2/`, extract the archive contents in :file:`/opt/geoserver-2.1.0/webapps/geoserver/WEB-INF/lib/`.

#. Restart GeoServer.

Upon a successful install a new directory named ``scripts`` will be created inside the data directory.