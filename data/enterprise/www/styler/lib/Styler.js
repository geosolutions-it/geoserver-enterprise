/**
 * Copyright (c) 2008 The Open Planning Project
 *
 * @include Styler/dispatch.js
 * @include Styler/ColorManager.js
 * @include Styler/SchemaManager.js
 * @include Styler/SLDManager.js
 */

/**
 * Constructor: Styler
 * Create a new styler application.
 *
 * Extends: Ext.util.Observable
 */
var Styler = Ext.extend(Ext.util.Observable, {
    
    map: null,
    wmsLayerList: null,
    layerList: null,
    currentLayer: null,
    sldManager: null,
    schemaManager: null,
    symbolTypes: null,
    ruleDlg: null,
    featureDlg: null,
    getFeatureControl: null,
    saving: null,
    windowPositions: {featureDlg: {}, ruleDlg: {}},

    /**
     * APIProperty: fonts
     * {Array(String)} List of fonts for the font combo.  If not set, defaults
     *     to the list provided by the <Styler.FontComboBox>.
     */
    fonts: undefined,
    
    constructor: function(config) {
        config = config || {};

        this.addEvents(
            /**
             * Event: layerchanged
             * Fires when the active layer is changed.
             *
             * Listener arguments:
             * layer - {OpenLayers.Layer} The newly active layer.
             */
            "layerchanged",

            /**
             * Event: ruleadded
             * Fires when a rule is added.
             *
             * Listener arguments:
             * rule - {OpenLayers.Rule} The rule added.
             */
            "ruleadded",

            /**
             * Event: ruleremoved
             * Fires when a rule is removed.
             *
             * Listener arguments:
             * rule - {OpenLayers.Rule} The rule removed.
             */
            "ruleremoved",

            /**
             * Event: ruleupdated
             * Fires when a rule is modified.
             *
             * Listener arguments:
             * rule - {OpenLayers.Rule} The rule modified.
             */
            "ruleupdated"

        );
        
        this.initialConfig = Ext.apply({}, config);
        Ext.apply(this, config);
        
        var baseLayers = [new OpenLayers.Layer("None", {isBaseLayer: true})];
        this.baseLayers = baseLayers.concat(config.baseLayers || []);
        
        Styler.superclass.constructor.call(this);

        Styler.dispatch(
            [
                function(done) {
                    Ext.onReady(function() {
                        this.createLayout();
                        done();
                    }, this);
                },
                function(done) {
                    this.getLayerList(done);
                }
            ],
            function() {
                this.createLayers();
                this.getSchemas(this.initEditor.createDelegate(this));
            },
            this
        );

    },
    
    getLayerList: function(callback) {
        this.layerList = [];
        this.getWMSCapabilities((function() {
            this.describeLayers(callback);
        }).createDelegate(this))
    },
    
    describeLayers: function(callback) {
        var config;
        var candidates = [];
        for (var i=0, ii=this.wmsLayerList.length; i<ii; ++i) {
            config = this.wmsLayerList[i];
            if (config.styles && config.styles.length) {
                candidates.push(config.name);
            }
        }
        var params = {
            SERVICE: "WMS",
            VERSION: "1.1.1",
            REQUEST: "DescribeLayer",
            LAYERS: candidates
        };
        var store = new GeoExt.data.WMSDescribeLayerStore({
            url: "/geoserver/wms?" + OpenLayers.Util.getParameterString(params),
            autoLoad: true,
            listeners: {
                load: function() {
                    var config, index;
                    for (var i=0, ii=this.wmsLayerList.length; i<ii; ++i) {
                        config = this.wmsLayerList[i];
                        index = store.findExact("layerName", config.name);
                        if (index > -1) {
                            if (store.getAt(index).get("owsType") === "WFS") {
                                this.layerList.push(config);
                            }
                        }
                    }
                    callback();
                },
                scope: this
            }
        });
    },
    
    /**
     * Method: getWMSCapabilities
     */
    getWMSCapabilities: function(callback) {
        var namespace = OpenLayers.Util.upperCaseObject(
            OpenLayers.Util.getParameters(window.location.href)
        ).NAMESPACE;
        Ext.Ajax.request({
            url: "/geoserver/ows",
            method: "GET",
            disableCaching: false,
            success: this.parseWMSCapabilities,
            failure: function() {
                throw("Unable to read capabilities from WMS");
            },
            params: Ext.apply(namespace ? {NAMESPACE: namespace} : {}, {
                VERSION: "1.1.1",
                REQUEST: "GetCapabilities",
                SERVICE: "WMS"
            }),
            options: {callback: callback},
            scope: this
        });
    },
    
    /**
     * Method: parseWMSCapabilities
     */
    parseWMSCapabilities: function(response, request) {
        var capabilities = new OpenLayers.Format.WMSCapabilities().read(
            response.responseXML && response.responseXML.documentElement ?
            response.responseXML : response.responseText);
        //this.wmsLayerList = capabilities.capability.layers;
        this.wmsLayerList = capabilities.capability.layers;
        request.options.callback();
    },

    /**
     * Method: createLayout
     * Create the layout with a map panel, a layers panel, and a legend panel.
     */
    createLayout: function() {
        
        // register the color manager with every color field
        Ext.util.Observable.observeClass(gxp.form.ColorField);
        gxp.form.ColorField.on({
            render: function(field) {
                var manager = new Styler.ColorManager();
                manager.register(field);
            }
        });

        this.getFeatureControl = new OpenLayers.Control.GetFeature({});
        this.getFeatureControl.events.on({
            "featureselected": function(e) {
                this.showFeatureInfo(this.currentLayer, e.feature);
            },
            scope: this
        });

        this.mapPanel = new GeoExt.MapPanel({
            border: true,
            region: "center",
            map: {
                allOverlays: false,
                controls: [
                    new OpenLayers.Control.Navigation({zoomWheelEnabled: false}),
                    new OpenLayers.Control.PanPanel(),
                    new OpenLayers.Control.ZoomPanel(),
                    this.getFeatureControl
                ],
                projection: new OpenLayers.Projection("EPSG:900913"),
                units: "m",
                theme: null,
                maxResolution: 156543.0339,
                maxExtent: new OpenLayers.Bounds(-20037508.34, -20037508.34,
                                                 20037508.34, 20037508.34),
                numZoomLevels: this.numZoomLevels || 20
            },
            items: [{
                xtype: "gx_zoomslider",
                vertical: true,
                height: 100,
                plugins: new GeoExt.ZoomSliderTip({
                    template: "<div>Zoom Level: {zoom}</div><div>Scale: 1 : {scale}</div>"
                })
            }]
        });

        this.legendContainer = new Ext.Panel({
            title: "Legend",
            height: 200,
            autoScroll: true,
            items: [{html: ""}],
            bbar: [{
                text: "Add new",
                iconCls: "add",
                disabled: true,
                handler: function() {
                    var panel = this.getLegend();
                    var Type = OpenLayers.Symbolizer[panel.symbolType];
                    var rule = new OpenLayers.Rule({
                        symbolizers: [new Type()]
                    });
                    panel.rules.push(rule);
                    this.fireEvent("ruleadded", rule);
                    this.showRule(this.currentLayer, rule, panel.symbolType, function() {
                        if(!this.saving) {
                            panel.rules.remove(rule);
                            this.fireEvent("ruleremoved", rule);
                        }
                    });
                },
                scope: this
            }, {
                text: "Delete selected",
                iconCls: "delete",
                disabled: true,
                handler: function() {
                    var panel = this.getLegend();
                    var rule = panel.selectedRule;
                    var message = "Are you sure you want to delete the " +
                        panel.getRuleTitle(rule) + " rule?";
                    Ext.Msg.confirm("Delete rule", message, function(yesno) {
                        if(yesno == "yes") {
                            // TODO: fix this in GeoExt
                            // http://trac.geoext.org/ticket/347
                            panel.rules.remove(rule);
                            panel.selectedRule = null;
                            this.fireEvent("ruleremoved", rule);
                            sldMgr = this.sldManager;
                            sldMgr.saveSld(this.currentLayer, function() {
                                this.ruleDlg.close();
                                this.repaint();
                            }, this);
                        }
                    }, this);
                },
                scope: this
            }]
        });
        
        this.layersContainer = new Ext.Panel({
            autoScroll: true,
            title: "Layers",
            anchor: "100%, -200"
        });

        var westPanel = new Ext.Panel({
            border: true,
            layout: "anchor",
            region: "west",
            width: 250,
            split: true,
            collapsible: true,
            hideCollapseTool: true,
            collapseMode: "mini",
            items: [
                this.layersContainer, this.legendContainer
            ]
        });

        var viewport = new Ext.Viewport({
            layout: "fit",
            hideBorders: true,
            items: {
                layout: "border",
                deferredRender: false,
                items: [this.mapPanel, westPanel]
            }
        });
        this.map = this.mapPanel.map;
        this.map.events.on({
            zoomend: this.setLegendScale,
            scope: this
        });
    },
    
    /**
     * Method: createLayers
     * Given the merged layer list, create WMS layers and add them to the map.
     */
    createLayers: function() {
        var layers = this.baseLayers.slice();
        var selected = -1;
        var selectedName = OpenLayers.Util.getParameters(window.location.href).layer;
        var alpha = OpenLayers.Util.alphaHack();
        var config;
        for (var i=0, ii=this.layerList.length; i<ii; ++i) {
            config = this.layerList[i]
            if (config.styles && config.styles.length > 0) {
                if (config.name === selectedName) {
                    selected = layers.length;
                }
                var llbbox = config.llbbox;
                // make sure we don't get infinity values for bottom and top
                llbbox[1] = Math.max(-85.0511, llbbox[1]);
                llbbox[3] = Math.min(85.0511, llbbox[3]);
                var maxExtent = OpenLayers.Bounds.fromArray(llbbox).transform(
                    new OpenLayers.Projection("EPSG:4326"),
                    new OpenLayers.Projection("EPSG:900913")
                );

                layers.push(new OpenLayers.Layer.WMS(
                    config.title, "/geoserver/wms", {
                        layers: config.name,
                        styles: config.styles[0].name,
                        transparent: true,
                        format: "image/png"
                    }, {
                        isBaseLayer: false,
                        buffer: 0,
                        tileSize: new OpenLayers.Size(512, 512),
                        displayOutsideMaxExtent: true,
                        visibility: false,
                        alpha: alpha, 
                        maxExtent: maxExtent
                    }
                ));
            }
        }
        if (selected === -1) {
            selected = layers.length - 1;
        }
        
        this.layerTree = new Ext.tree.TreePanel({
            border: false,
            animate: false,
            plugins: [
                new GeoExt.plugins.TreeNodeRadioButton({
                    listeners: {
                        radiochange: function(node) {
                            this.changeLayer(node);
                        },
                        scope: this
                    }
                })
            ],
            loader: new Ext.tree.TreeLoader({
                applyLoader: false,
                uiProviders: {
                    layerNodeUI: Ext.extend(
                        GeoExt.tree.LayerNodeUI, 
                        new GeoExt.tree.TreeNodeUIEventMixin()
                    )
                }
            }),
            root: {
                nodeType: "async",
                allowDrop: false,
                children: [{
                    nodeType: "gx_overlaylayercontainer",
                    allowDrag: false,
                    expanded: true,
                    leaf: false,
                    loader: {
                        baseAttrs: {
                            radioGroup: "active",
                            uiProvider: "layerNodeUI"
                        }
                    }
                }, {
                    nodeType: "gx_baselayercontainer",
                    leaf: false,
                    allowDrag: false,
                    allowDrop: false,
                    loader: {
                        baseAttrs: {
                            allowDrag: false
                        }
                    }
                }]
            },
            enableDD: true,
            listeners: {
                /**
                 * TODO: This dragdrop listener should not be necessary. Make
                 * sure that the radio state does not get changed for a layer
                 * node in GeoExt on dragdrop.
                 * http://trac.geoext.org/ticket/346
                 */
                dragdrop: function(panel, node) {
                    window.setTimeout(
                        this.checkCurrentLayerNode.createDelegate(this)
                    );
                },
                scope: this
            },
            rootVisible: false,
            lines: false
        });
        this.layersContainer.add(this.layerTree);
        this.layersContainer.doLayout();

        this.map.addLayers(layers);
        
        this.setCurrentLayer(this.map.layers[selected]);
    },
    
    /**
     * This is unsavory, but we need a way to check the radio of the current
     * layer - when it hasn't been checked by the user.
     */
    checkCurrentLayerNode: function() {
        this.layerTree.getRootNode().firstChild.cascade(function(node) {
            var el = node.ui.anchor.previousSibling;
            if (el && el.type === "radio") {
                if (node.layer === this.currentLayer && !el.checked) {
                    el.checked = true;
                }
            }
        }, this);
    },
    
    /**
     * Method: getSchemas
     * Request schemas for all layers.  Record the geometry attribute name and
     *     symbol type for each layer.
     */
    getSchemas: function(callback) {
        this.schemaManager = new Styler.SchemaManager(this.map);
        this.schemaManager.loadAll(callback);
    },
    
    /**
     * Method: getStyles
     * Create a new sld manager and initiate loading of all styles.  Call the
     *     callback provided when loading is complete.
     *
     * Parameters:
     * callback - {Function} Function to be called when SLD fetching & parsing
     *     is done.
     */
    getStyles: function(callback) {
        this.sldManager = new Styler.SLDManager(this.map);
        this.sldManager.loadAll(callback);
    },
    
    /**
     * Method: initEditor
     */
    initEditor: function() {
        this.symbolTypes = {};
        this.sldManager = new Styler.SLDManager(this.map);
        this.getFeatureControl.activate();
        this.setCurrentLayer(this.currentLayer);
        this.on({
            "ruleadded": function() {
                this.refreshLegend();
                this.refreshFeatureDlg();
            },
            "ruleremoved": function() {
                this.refreshLegend();
                this.refreshFeatureDlg();
            },
            "ruleupdated": function() {
                this.refreshLegend();
                this.refreshFeatureDlg();
            },
            "layerchanged": function(layer) {
                this.showLegend(layer);
            },
            scope: this
        });
        
        this.showLegend(this.currentLayer);

    },
    
    changeLayer: function(node) {
        if (this.currentLayer != node.layer) {
            this.setCurrentLayer(node.layer);
        }
    },
    
    setCurrentLayer: function(layer) {
        if(layer != this.currentLayer) {
            var extent = this.map.getExtent();
            if (!extent || !layer.maxExtent.containsLonLat(extent.getCenterLonLat())) {
                this.map.zoomToExtent(layer.maxExtent);
            }
            this.currentLayer = layer;
            if(this.ruleDlg) {
                this.ruleDlg.destroy();
                delete this.ruleDlg;
            }
            if(this.featureDlg) {
                this.featureDlg.destroy();
                delete this.featureDlg;
            }
            this.checkCurrentLayerNode();
            this.fireEvent("layerchanged", this.currentLayer);

        }
        if(layer.getVisibility() === false) {
            layer.setVisibility(true);
        }
        // this is getting a bit sloppy - the remainder only works after initEditor
        // and require that setCurrentLayer be called again in initEditor
        if(this.getFeatureControl.active) {
            this.getFeatureControl.protocol = OpenLayers.Protocol.WFS.fromWMSLayer(layer, {
                url: "/geoserver/ows",
                geometryName: this.schemaManager.getGeometryName(layer)
            });
        }
    },
    
    /**
     * Method: getRules
     */
    getRules: function(layer, callback) {
        var rules;
        var style = this.sldManager.getStyle(layer);
        if(style) {
            callback.call(this, style.rules);
        } else {
            this.sldManager.loadSld(
                layer,
                layer.params["STYLES"],
                function(result) {
                    callback.call(this, result.style.rules);
                }.createDelegate(this)
            );
        }
    },
    
    /**
     * Method: showLegend
     * Initiate the sequence to show the legend for a layer.  Because the layer
     *     geometry type may not be known, the legend will not actually be shown
     *     until the geometry type is determined.  If the active layer changes
     *     before he legend is actually displayed, the sequence will be aborted.
     */
    showLegend: function(layer) {
        this.removeLegend();
        this.legendContainer.setTitle("Legend: " + layer.name);
        var mask = new Ext.LoadMask(this.legendContainer.getEl(), {
            msg: "Loading ...",
            removeMask: true
        });
        mask.show();
        Styler.dispatch(
            [
                function(done, context) {
                    this.getSymbolType(layer, function(type) {
                        context.symbolType = type;
                        done();
                    });
                },
                function(done, context) {
                    this.getRules(layer, function(rules) {
                        context.rules = rules;
                        done();
                    });
                }
            ],
            function(context) {
                if(layer === this.currentLayer) {
                    mask.hide();
                    this.addLegend(layer, context.rules, context.symbolType);
                }
            },
            this
        );
    },
    
    /**
     * Method: addLegend
     * Only called from <showLegend> if the active layer was not called while
     *     the layer symbol type or rules were being determined.
     */
    addLegend: function(layer, rules, type) {
        var deleteButton = this.getDeleteButton();
        var legend = new GeoExt.VectorLegend({
            rules: rules,
            symbolType: type,
            enableDD: false,
            style: {padding: "10px"},
            selectOnClick: true,
            currentScaleDenominator: this.map.getScale(),
            listeners: {
                "ruleselected": function(panel, rule) {
                    this.showRule(this.currentLayer, rule, panel.symbolType);
                    deleteButton.enable();
                },
                "ruleunselected": function(panel, rule) {
                    deleteButton.disable();
                },
                "rulemoved": function(panel, rule) {
                    legend.disable();
                    this.sldManager.saveSld(this.currentLayer, function() {
                        legend.enable();
                        this.repaint();
                    }, this);
                },
                scope: this
            }
        });
        this.legendContainer.add(legend);
        this.legendContainer.doLayout();
        this.getAddButton().enable();
    },
    
    /**
     * Method: removeLegend
     * Undo what is done in addLegend.
     */
    removeLegend: function() {
        var old = this.getLegend();
        if (old) {
            this.getAddButton().disable();
            this.legendContainer.remove(old);
        }
    },
    
    /**
     * Method: setLegendScale
     * Called when map scale changes.
     */
    setLegendScale: function() {
        var legend = this.getLegend();
        if (legend && legend.setCurrentScaleDenominator) {
            legend.setCurrentScaleDenominator(this.map.getScale());
        }
    },

    /**
     * Method: refreshLegend
     * Redraw the legend if shown.
     */
    refreshLegend: function() {
        var legend = this.getLegend();
        if (legend) {
            legend.update();
        }
    },
    
    /**
     * Method: refreshFeatureDlg
     * Refresh the feature info shown in any feature dialog.
     */
    refreshFeatureDlg: function() {
        if(this.featureDlg && !this.featureDlg.hidden) {
            var feature = this.featureDlg.getFeature();
            this.showFeatureInfo(this.currentLayer, feature);
        }
    },
    
    /**
     * Method: setSymbolType
     * Set the symbol type for a layer given a feature.
     */
    setSymbolType: function(layer, type) {
        this.symbolTypes[layer.id] = type;
        return type;
    },
    
    /**
     * Method: getSymbolTypeFromFeature
     * Determine the symbol type given a feature.
     *
     * Parameters:
     * feature - {OpenLayers.Feature.Vector}
     *
     * Returns:
     * {String} The symbol type.
     */
    getSymbolTypeFromFeature: function(feature) {
        return feature.geometry.CLASS_NAME.replace(/OpenLayers\.Geometry\.(Multi)?|String/g, "");
    },
    
    /**
     * Method: getSymbolType
     * Get the symbol type for a layer.
     *
     * Parameters:
     * layer - {OpenLayers.Layer.WMS}
     * callback - {Function} Function to call when symbol type is determined.
     *     The callback will be called with the type as an argument.
     */
    getSymbolType: function(layer, callback) {
        var type = this.symbolTypes[layer.id];
        if(type) {
            callback.call(this, type);
        } else {
            type = this.schemaManager.getSymbolType(layer);
            if(type) {
                this.setSymbolType(layer, type);
                callback.call(this, type);
            } else {
                this.getOneFeature(layer, function(features) {
                    type = this.setSymbolType(layer, this.getSymbolTypeFromFeature(features[0]));
                    callback.call(this, type);
                });
            }
        }
    },
    
    showFeatureInfo: function(layer, feature) {
        if(this.featureDlg) {
            this.featureDlg.destroy();
        }
        
        this.getRules(layer, function(rules) {
            this.displayFeatureDlg(layer, feature, rules);
        });
    },
    
    displayFeatureDlg: function(layer, feature, rules) {
        
        // feature needs a layer to evaluate scale constraints
        feature.layer = layer;        
        var matchingRules = [];
        var rule;
        for(var i=0; i<rules.length; ++i) {
            rule = rules[i];
            if(rule.evaluate(feature)) {
                matchingRules.push(rule);
            }
        }
        
        this.featureDlg = new Ext.Window({
            title: "Feature: " + feature.fid || feature.id,
            layout: "fit",
            resizable: false,
            width: 220,
            x: this.windowPositions.featureDlg.x,
            y: this.windowPositions.featureDlg.y,
            items: [{
                hideBorders: true,
                border: false,
                autoHeight: true,
                items: [{
                    xtype: "gx_vectorlegend",
                    title: "Rules used to render this feature:",
                    bodyStyle: {paddingLeft: "5px"},
                    symbolType: this.getSymbolTypeFromFeature(feature),
                    rules: matchingRules,
                    clickableSymbol: true,
                    listeners: {
                        "symbolclick": function(panel, rule) {
                            this.showRule(this.currentLayer,
                                rule, panel.symbolType);
                        },
                        scope: this
                    }
                }, {
                    xtype: "propertygrid",
                    title: "Attributes of this feature:",
                    height: 120,
                    source: feature.attributes,
                    autoScroll: true,
                    listeners: {
                        "beforepropertychange": function() {
                            return false;
                        }
                    }
                }]
            }],
            listeners: {
                "move": function(cp, x, y) {
                    this.windowPositions["featureDlg"] = {x: x, y: y};
                },
                scope: this
            },
            getFeature: function() { return feature }
        });
        
        this.featureDlg.show();
    },

    /**
     * Method: showRule
     * Show the rule dialog for a particular layer/rule combo.
     */
    showRule: function(layer, rule, symbolType, closeCallback) {
        var newRule = rule.clone();
        if(this.ruleDlg) {
            this.ruleDlg.destroy();
        }
        this.ruleDlg = new Ext.Window({
            title: "Style: " + (rule.title || rule.name || "Untitled"),
            layout: "fit",
            x: this.windowPositions.ruleDlg.x,
            y: this.windowPositions.ruleDlg.y,
            width: 315,
            constrainHeader: true,
            items: [{
                xtype: "gx_rulepanel",
                autoHeight: false,
                autoScroll: true,
                rule: newRule,
                symbolType: symbolType, // TODO: decide whether we want to guess this from the rule
                fonts: this.fonts,
                nestedFilters: false,
                scaleLevels: this.map.baseLayer.numZoomLevels,
                minScaleDenominatorLimit: OpenLayers.Util.getScaleFromResolution(
                    this.map.baseLayer.resolutions[this.map.baseLayer.numZoomLevels-1],
                    this.map.units
                ),
                maxScaleDenominatorLimit: OpenLayers.Util.getScaleFromResolution(
                    this.map.baseLayer.resolutions[0],
                    this.map.units
                ),
                scaleSliderTemplate: "<div>{scaleType} Zoom Level: {zoom}</div>" + 
                    "<div>Current Map Zoom: {mapZoom}</div>",
                modifyScaleTipContext: (function(panel, data) {
                    data.mapZoom = this.map.getZoom();
                }).createDelegate(this),
                attributes: new GeoExt.data.AttributeStore({
                    url: "/geoserver/wfs?",
                    baseParams: {
                        version: "1.1.1",
                        request: "DescribeFeatureType",
                        typename: layer.params["LAYERS"]
                    },
                    ignore: {name: this.schemaManager.getGeometryName(layer)}
                }),
                pointGraphics: [
                    {display: "circle", value: "circle", mark: true, preview: "theme/img/circle.gif"},
                    {display: "square", value: "square", mark: true, preview: "theme/img/square.gif"},
                    {display: "triangle", value: "triangle", mark: true, preview: "theme/img/triangle.gif"},
                    {display: "star", value: "star", mark: true, preview: "theme/img/star.gif"},
                    {display: "cross", value: "cross", mark: true, preview: "theme/img/cross.gif"},
                    {display: "x", value: "x", mark: true, preview: "theme/img/x.gif"},
                    {display: "custom..."}
                ]
            }],
            bbar: ["->", {
                text: "cancel",
                iconCls: "cancel",
                handler: function() {
                    this.ruleDlg.close();
                },
                scope: this
            }, {
                text: "save",
                iconCls: "save",
                handler: function() {
                    this.saving = true;
                    this.ruleDlg.disable();
                    this.updateRule(rule, newRule);
                    this.sldManager.saveSld(layer, function() {
                        this.ruleDlg.close();
                        this.repaint();
                        this.saving = false;
                    }, this);
                },
                scope: this
            }],
            listeners: {
                close: function() {
                    this.getLegend().unselect();
                    if(closeCallback) {
                        closeCallback.call(this);
                    }
                },
                move: function(cp, x, y) {
                    this.windowPositions["ruleDlg"] = {x: x, y: y};
                },
                scope: this
            }
        });
        this.ruleDlg.show();
    },
    
    /**
     * Method: updateRule
     * Update the title, symbolizer, filter, and scale constraints of an
     *     existing rule with properties from another rule.
     */
    updateRule: function(rule, newRule) {
        rule.title = newRule.title;
        rule.symbolizers = newRule.symbolizers;
        rule.filter = newRule.filter;
        rule.minScaleDenominator = newRule.minScaleDenominator;
        rule.maxScaleDenominator = newRule.maxScaleDenominator;
        this.fireEvent("ruleupdated", rule);
    },
    
    repaint: function () {
        this.currentLayer.redraw(true);
    },

    getOneFeature: function(layer, callback) {
        Ext.Ajax.request({
            url: "/geoserver/wfs?",
            method: "GET",
            disableCaching: false,
            params: {
                version: "1.0.0",
                request: "GetFeature",
                typeName: layer.params["LAYERS"],
                maxFeatures: "1"
            },
            success: function(response) {
                var features = new OpenLayers.Format.GML().read(
                    response.responseXML || response.responseText);
                if(features.length) {
                    callback.call(this, features);
                } else {
                    throw("Could not load features from the WFS");
                }
            },
            failure: function(response) {
                throw("Could not load features from the WFS");
            },
            scope: this
        });
    },
        
    /**
     * Method: getLegend
     */
    getLegend: function() {
        return !!this.legendContainer.items.length && this.legendContainer.getComponent(0);
    },

    /**
     * Method: getAddButton
     */
    getAddButton: function() {
        return this.legendContainer.getBottomToolbar().items.get(0);
    },
    
    /**
     * Method: getDeleteButton
     */
    getDeleteButton: function() {
        return this.legendContainer.getBottomToolbar().items.get(1);
    }
});

// Global settings
OpenLayers.DOTS_PER_INCH = 25.4 / 0.28;
