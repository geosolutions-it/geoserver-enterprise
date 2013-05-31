.. _globalsettings:

Global Settings
================

The Global Setting page configures messaging, logging, character, and proxy settings for the entire server.

.. figure:: ../images/server_globalsettings.png
   :align: center
   
   *Global Settings Page*
   
Verbose Messages
----------------

Verbose Messages, when enabled, will cause GeoServer to return XML with newlines and indents. Because such XML responses contain a larger amount of data, and in turn requires a larger amount of bandwidth, it is recommended to use this option only for testing purposes. 


Verbose Exception Reporting
---------------------------

Verbose Exception Reporting returns service exceptions with full Java stack traces. It writes to the GeoServer log file and offers one of the most useful configuration options for debugging. When disabled, GeoServer returns single-line error messages.

Enable Global Services
----------------------

When enabled, allows access to both global services and :ref:`virtual services <virtual_services>`. When disabled, clients will only be able to access virtual services. Disabling is useful if GeoServer is hosting a large amount of layers and you want to ensure that client always request limited layer lists. Disabling is also useful for security reasons.


Resource Error Handling
-----------------------

This setting determines how GeoServer will respond when a layer becomes inaccessible for some reason. By default, when a layer has an error (for example, when the default style for the layer is deleted), a service exception is printed as part of the capabilities document, making the document invalid. For clients that rely on a valid capabilities document, this can effectively make a GeoServer appear to be "offline". 

An administrator may prefer to configure GeoServer to simply omit the problem layer from the capabilities document, thus retaining the document integrity and allowing clients to connect to other published layers.

There are two options:

**OGC_EXCEPTION_REPORT**: This is the default behavior. Any layer errors will show up as Service Exceptions in the capabilities document, making it invalid.

**SKIP_MISCONFIGURED_LAYERS**: With this setting, GeoServer will elect simply to not describe the problem layer at all, removing it from the capabilities document, and preserving the integrity of the rest of the document. Note that having a layer "disappear" may cause other errors in client functionality.


Number of Decimals
------------------

Refers to the number of decimal places returned in a GetFeature response. Also useful in optimizing bandwidth. Default is **8**.

Character Set
-------------

Specifies the global character encoding that will be used in XML responses. Default is **UTF-8**, which is recommended for most users. A full list of supported character sets is available on the `IANA Charset Registry <http://www.iana.org/assignments/character-sets>`_.

Proxy Base URL
--------------

GeoServer can have the capabilities documents report a proxy properly. The Proxy Base URL field is the base URL seen beyond a reverse proxy.

Logging Profile
---------------

Logging Profile corresponds to a log4j configuration file in the GeoServer data directory. (Apache `log4j <http://logging.apache.org/log4j/1.2/index.html>`_ is a Java-based logging utility.)  By default, there are five logging profiles in GeoServer; additional customized profiles can be added by editing the log4j file. 

There are six logging levels used in the log itself. They range from the least serious TRACE, through DEBUG, INFO, WARN, ERROR and finally the most serious, FATAL. The GeoServer logging profiles combine logging levels with specific server operations. The five pre-built logging profiles available on the global settings page are:
 
#. **Default Logging** (``DEFAULT_LOGGING``)—Provides a good mix of detail without being VERBOSE. Default logging enables INFO on all GeoTools and GeoServer levels, except certain (chatty) GeoTools packages which require WARN. 
#. **GeoServer Developer Logging** (``GEOSERVER_DEVELOPER_LOGGING``)-A verbose logging profile that includes DEBUG information on GeoServer and VFNY. This developer profile is recommended for active debugging of GeoServer.
#. **GeoTools Developer Logging** (``GEOTOOLS_DEVELOPER_LOGGING``)—A verbose logging profile that includes DEBUG information only on GeoTools. This developer profile is recommended for active debugging of GeoTools.
#. **Production Logging** (``PRODUCTION_LOGGING``) is the most minimal logging profile, with only WARN enabled on all GeoTools and GeoServer levels. With such production level logging, only problems are written to the log files.
#. **Verbose Logging**  (``VERBOSE_LOGGING``)—Provides more detail by enabling DEBUG level logging on GeoTools, GeoServer, and VFNY.


Log to StdOut
-------------

Standard output (StdOut) determines where a program writes its output data. In GeoServer, the Log to StdOut setting enables logging to the text terminal that initiated the program. If you are running GeoServer in a large J2EE container, you might not want your container-wide logs filled with GeoServer information. Clearing this option will suppress most GeoServer logging, with only FATAL exceptions still output to the console log.

Log Location
------------

Sets the written output location for the logs. A log location may be a directory or a file, and can be specified as an absolute path (e.g., :file:`C:\\GeoServer\\GeoServer.log`) or a relative one (for example, :file:`GeoServer.log`). Relative paths are relative to the GeoServer data directory. Default is :file:`logs/geoserver.log`.

XML POST request log buffer 
---------------------------

In more verbose logging levels, GeoServer will log the body of XML (and other format) POST requests. It will only log the initial part of the request though, since it has to store (buffer) everything that gets logged for use in the parts of GeoServer that use it normally. This setting sets the size of this buffer, in characters. A setting of **0** will disable the log buffer.


Feature type cache size
-----------------------

GeoServer can cache datastore connections and schemas in memory for performance reasons. The cache size should generally be greater than the number of distinct featuretypes that are expected to be accessed simultaneously. If possible, make this value larger than the total number of featuretypes on the server, but a setting too high may produce out-of-memory errors.