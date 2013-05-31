Building
------------------------

In order to user Styler you'll first have to build it.
In order to build it:

sudo easy_install jstools
mkdir script
jsbuild build.cfg -o script

Using
-----------------------

Copy (or link) the entire directory in a place that
in the GeoServer $DATA_DIR/www/styler folder, and then
access it from:
http://host:port/geoserver/www/styler/index.html

You can filter the layers displayed in the layer tree by passing a namespace
parameter to the url, e.g.
http://host.port/geoserver/www/styler/index.html?namespace=topp

To start styler with a specific layer activated, pass a layer parameter to
the url, e.g.
http://host.port/geoserver/www/styler/index.html?layer=topp:states

In order for styler to work the GeoSErver RESTConfig extension
will have to be installed as well

