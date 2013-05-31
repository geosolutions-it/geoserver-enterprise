.. _migrating_data_directory:

Migrating a Data Directory between different versions
=====================================================

Minor and major version numbers
-------------------------------

There should generally be no problems or issues migrating data directories between major and minor versions of GeoServer (i.e. from 2.0.0 to 2.0.1 and vice versa, or from 1.6.x to 1.7.x and vice versa).

Migrating between GeoServer 1.7.x and 2.0.x
-------------------------------------------

When using GeoServer 2.0.x with a data directory from the 1.7.x branch, modifications will occur to the directory **immediately** that will **make the data directory incompatible with 1.7.x**!  Below is a list of changes made to the data directory.

Files and directories added
```````````````````````````

::

  wfs.xml
  wcs.xml
  wms.xml
  logging.xml
  global.xml
  workspaces/*
  layergroups/*
  styles/*.xml

Files renamed
`````````````

* ``catalog.xml`` renamed to ``catalog.xml.old``
* ``services.xml`` renamed to ``services.xml.old``

Reverting from GeoServer 2.0.x to 1.7.x
---------------------------------------

In order to revert the directory to be compatible with 1.7.x again:

#. Stop GeoServer.

#. Delete the following files and directories::

      wfs.xml
      wcs.xml
      wms.xml
      logging.xml
      global.xml
      workspaces/*
      layergroups/*
      styles/*.xml

#. Rename ``catalog.xml.old`` to ``catalog.xml``.

#. Rename ``services.xml.old`` to ``services.xml``.

.. _migrating_data_directory_22x:

Migrating between GeoServer 2.1.x and 2.2.x
-------------------------------------------

The security changes that ship with GeoServer 2.2 require modifications to the ``security`` directory of the 
GeoServer data directory.

Files and directories added
```````````````````````````

::

  security/*.xml
  security/masterpw.*
  security/geoserver.jceks
  security/auth/*
  security/filter/*
  security/masterpw/*
  security/pwpolicy/*
  security/role/*
  security/usergroup/*
  
Files renamed
`````````````

  * ``security/users.properties`` renamed to ``security/users.properties.old``


Reverting from GeoServer 2.1.x and 2.1.x
----------------------------------------

In order to restore the GeoServer 2.1 configuration:

#. Stop GeoServer.

#. Rename ``users.properties.old`` to ``users.properties``.

#. Additionally (although not mandatory) delete the following files and directories::

     security/
       config.xml
       geoserver.jceks
       masterpw.xml
       masterpw.digest
       masterpw.info
       auth/
       filter/
       masterpw/
       pwpolicy/
       role/
       usergroup/
