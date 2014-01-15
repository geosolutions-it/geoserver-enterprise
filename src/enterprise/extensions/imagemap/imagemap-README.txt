GeoServer ImageMap OutputFormat Extension
==========================================

This extension adds a new WMS output format to GeoServer, 
to produce html image maps. The outputFormat mime-type is
text/html.


INSTALLATION

1) Copy all files to the GeoServer library directory.
   In a binary install this is [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a WAR install this is [container]/webapps/geoserver/WEB-INF/lib/
2) Restart GeoServer.

COMPATIBILITY

This jar should work with any version of GeoServer based on GeoTools 10.x.  
Currently this is anything in 2.4.x. 

For more information see on needed libraries and parameters, see:
http://geoserver.org/display/GEOS/HTML+ImageMap+support

Please report any bugs with jira (http://jira.codehaus.org/browse/GEOS). 

Any other issues can be discussed on the geoserver-users mailing list
(http://lists.sourceforge.net/lists/listinfo/geoserver-users).

