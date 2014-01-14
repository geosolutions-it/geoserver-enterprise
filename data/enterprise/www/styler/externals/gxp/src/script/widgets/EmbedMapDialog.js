/**
 * Copyright (c) 2010 OpenPlans
 */

/**
 */

Ext.namespace("gxp");

/** api: (define)
 *  module = gxp
 *  class = EmbedMapDialog
 *  base_link = `Ext.Container <http://extjs.com/deploy/dev/docs/?class=Ext.Container>`_
 */

/** api: example
 *  Show a :class:`gxp.EmbedMapDialog` in a window, using "viewer.html" in the
 *  current path as url:
 * 
 *  .. code-block:: javascript
 *      new Ext.Window({
 *           title: "Export Map",
 *           layout: "fit",
 *           width: 380,
 *           autoHeight: true,
 *           items: [{
 *               xtype: "gx_embedmapdialog",
 *               url: "viewer.html" 
 *           }]
 *       }).show();
 */
 
/** api: constructor
 *  .. class:: EmbedMapDialog(config)
 *   
 *  A dialog for configuring a map iframe to embed on external web pages.
 */
gxp.EmbedMapDialog = Ext.extend(Ext.Container, {
    
    /** api: config[url]
     *  ``String`` the url to use as the iframe's src of the embed snippet. Can
     *  be a url relative to the current href and will be converted to an
     *  absolute one.
     */
    url: null,

    /** api: property[url]
     *  ``String`` the url to use as the iframe's src of the embed snippet. Can
     *  be a url relative to the current href and will be converted to an
     *  absolute one.
     */
    url: null,
    
    /* begin i18n */
    /** api: config[publishMessage] ``String`` i18n */
    publishMessage: "Your map is ready to be published to the web! Simply copy the following HTML to embed the map in your website:",
    /** api: config[heightLabel] ``String`` i18n */
    heightLabel: 'Height',
    /** api: config[widthLabel] ``String`` i18n */
    widthLabel: 'Width',
    /** api: config[mapSizeLabel] ``String`` i18n */
    mapSizeLabel: 'Map Size',
    /** api: config[miniSizeLabel] ``String`` i18n */
    miniSizeLabel: 'Mini',
    /** api: config[smallSizeLabel] ``String`` i18n */
    smallSizeLabel: 'Small',
    /** api: config[premiumSizeLabel] ``String`` i18n */
    premiumSizeLabel: 'Premium',
    /** api: config[largeSizeLabel] ``String`` i18n */
    largeSizeLabel: 'Large',
    /* end i18n */
    
    /** private: property[snippetArea]
     */
    snippetArea: null,
    
    /** private: property[heightField]
     */
    heightField: null,
    
    /** private: property[widthField]
     */
    widthField: null,
    
    /** private: method[initComponent]
     */
    initComponent: function() {
        Ext.apply(this, this.getConfig());
        gxp.EmbedMapDialog.superclass.initComponent.call(this);
    },
    
    /** private: method[updateSnippet]
     */
    updateSnippet: function() {
        this.snippetArea.setValue(
            '<iframe height="' + this.heightField.getValue() +
            '" width="' + this.widthField.getValue() +'" src="' + 
            gxp.util.getAbsoluteUrl(this.url) + '"></iframe>'
        );
        this.snippetArea.focus(true, 100);
    },
    
    /** private: method[getConfig]
     */
    getConfig: function() {
        this.snippetArea = new Ext.form.TextArea({
            height: 70,
            selectOnFocus: true,
            readOnly: true
        });
        
        var numFieldListeners = {
            "change": this.updateSnippet,
            "specialkey": function(f, e) {
                e.getKey() == e.ENTER && this.updateSnippet();
            },
            scope: this
        };

        this.heightField = new Ext.form.NumberField({
            width: 50,
            value: 400,
            listeners: numFieldListeners
        });
        this.widthField = new Ext.form.NumberField({
            width: 50,
            value: 600,
            listeners: numFieldListeners
        });        

        var adjustments = new Ext.Container({
            layout: "column",
            defaults: {
                border: false,
                xtype: "box"
            },
            items: [
                {autoEl: {cls: "gx-field-label", html: this.mapSizeLabel}},
                new Ext.form.ComboBox({
                    editable: false,
                    width: 75,
                    store: new Ext.data.SimpleStore({
                        fields: ["name", "height", "width"],
                        data: [
                            [this.miniSizeLabel, 100, 100],
                            [this.smallSizeLabel, 200, 300],
                            [this.largeSizeLabel, 400, 600],
                            [this.premiumSizeLabel, 600, 800]
                        ]
                    }),
                    triggerAction: 'all',
                    displayField: 'name',
                    value: this.largeSizeLabel,
                    mode: 'local',
                    listeners: {
                        "select": function(combo, record, index) {
                            this.widthField.setValue(record.get("width"));
                            this.heightField.setValue(record.get("height"));
                            this.updateSnippet();
                        },
                        scope: this
                    }
                }),
                {autoEl: {cls: "gx-field-label", html: this.heightLabel}},
                this.heightField,
                {autoEl: {cls: "gx-field-label", html: this.widthLabel}},
                this.widthField
            ]
        });

        return {
            border: false,
            defaults: {
                border: false,
                cls: "gx-export-section",
                xtype: "container",
                layout: "fit"
            },
            items: [{
                xtype: "box",
                autoEl: {
                    tag: "p",
                    html: this.publishMessage
                }
            }, {
                items: [this.snippetArea]
            }, {
                items: [adjustments]
            }],
            listeners: {
                "afterrender": this.updateSnippet,
                scope: this
            }
        }
    }
});

/** api: xtype = gx_embedmapdialog */
Ext.reg('gx_embedmapdialog', gxp.EmbedMapDialog);