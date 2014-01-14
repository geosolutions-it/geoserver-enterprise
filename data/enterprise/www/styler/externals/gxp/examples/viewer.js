var app;
Ext.onReady(function() {
    app = new gxp.Viewer({
        portalConfig: {
            renderTo: document.body,
            width: 500,
            height: 300
        },
        defaultSourceType: "gx_wmssource",
        sources: {
            local: {
                url: "/geoserver/wms"
            },
            google: {
                ptype: "gx_googlesource"
            }
        },
        map: {
            projection: "EPSG:900913",
            units: "m",
            maxResolution: 156543.0339,
            center: [-10764594.758211, 4523072.3184791],
            zoom: 3,
            layers: [{
                source: "google",
                name: "G_PHYSICAL_MAP",
                group: "background"
            }, {
                source: "local",
                name: "usa:states"
            }]
        }
    });
});
