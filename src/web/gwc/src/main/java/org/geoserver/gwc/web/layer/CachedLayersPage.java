/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.geoserver.gwc.web.layer.CachedLayerProvider.ACTIONS;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.ENABLED;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.NAME;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.PREVIEW_LINKS;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.QUOTA_LIMIT;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.QUOTA_USAGE;
import static org.geoserver.gwc.web.layer.CachedLayerProvider.TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.web.GWCIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.image.io.ImageIOExt;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;

/**
 * @author groldan
 * @see GWC#removeTileLayers(List)
 */
public class CachedLayersPage extends GeoServerSecuredPage {
    private CachedLayerProvider provider = new CachedLayerProvider();

    private GeoServerTablePanel<TileLayer> table;

    private GeoServerDialog dialog;

    private CachedLayerSelectionRemovalLink removal;

    public CachedLayersPage() {

        table = new GeoServerTablePanel<TileLayer>("table", provider, true) {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<TileLayer> property) {

                if (property == TYPE) {
                    Fragment f = new Fragment(id, "iconFragment", CachedLayersPage.this);
                    ResourceReference layerIcon;
                    TileLayer layer = (TileLayer) itemModel.getObject();
                    layerIcon = (ResourceReference) property.getPropertyValue(layer);
                    f.add(new Image("layerIcon", layerIcon));
                    return f;
                } else if (property == NAME) {
                    return nameLink(id, itemModel);
                } else if (property == QUOTA_LIMIT) {
                    IModel<Quota> quotaLimitModel = property.getModel(itemModel);
                    return quotaLink(id, quotaLimitModel);
                } else if (property == QUOTA_USAGE) {
                    IModel<Quota> quotaUsageModel = property.getModel(itemModel);
                    return quotaLink(id, quotaUsageModel);
                } else if (property == ENABLED) {
                    TileLayer layerInfo = (TileLayer) itemModel.getObject();
                    boolean enabled = layerInfo.isEnabled();
                    ResourceReference icon;
                    if (enabled) {
                        icon = GWCIconFactory.getEnabledIcon();
                    } else {
                        icon = GWCIconFactory.getDisabledIcon();
                    }
                    Fragment f = new Fragment(id, "iconFragment", CachedLayersPage.this);
                    f.add(new Image("layerIcon", icon));
                    return f;
                } else if (property == PREVIEW_LINKS) {
                    return previewLinks(id, itemModel);
                } else if (property == ACTIONS) {
                    return actionsLinks(id, itemModel);
                }

                throw new IllegalArgumentException("Don't know a property named "
                        + property.getName());
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(table.getSelection().size() > 0);
                target.addComponent(removal);
            }

        };
        table.setOutputMarkupId(true);
        add(table);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialWidth(360);
        dialog.setInitialHeight(180);
        setHeaderPanel(headerPanel());

        Long imageIOFileCachingThreshold = ImageIOExt.getFilesystemThreshold();
        if (null == imageIOFileCachingThreshold || 0L >= imageIOFileCachingThreshold.longValue()) {
            String warningMsg = new ResourceModel("GWC.ImageIOFileCachingThresholdUnsetWarning")
                    .getObject();
            super.warn(warningMsg);
        }
    }

    private Component quotaLink(String id, IModel<Quota> quotaModel) {
        Quota quota = quotaModel.getObject();
        String formattedQuota;
        if (null == quota) {
            formattedQuota = new ResourceModel("CachedLayersPage.quotaLimitNotSet").getObject();
        } else {
            formattedQuota = quota.toNiceString();
        }
        return new Label(id, formattedQuota);
    }

    private Component nameLink(String id, IModel<TileLayer> itemModel) {

        Component link;

        final TileLayer layer = itemModel.getObject();

        final String layerName = layer.getName();
        if (layer instanceof GeoServerTileLayer) {
            link = new ConfigureCachedLayerAjaxLink(id, itemModel, CachedLayersPage.class);
        } else {
            link = new Label(id, layerName);
        }

        return link;
    }

    private Component actionsLinks(String id, IModel<TileLayer> tileLayerNameModel) {
        final String name = tileLayerNameModel.getObject().getName();
        final String href = "../gwc/rest/seed/" + name;

        // openlayers preview
        Fragment f = new Fragment(id, "actionsFragment", CachedLayersPage.this);
        f.add(new ExternalLink("seedLink", href, new ResourceModel("CachedLayersPage.seed")
                .getObject()));
        f.add(truncateLink("truncateLink", tileLayerNameModel));
        return f;

    }

    private SimpleAjaxLink<String> truncateLink(final String id,
            IModel<TileLayer> tileLayerNameModel) {

        String layerName = tileLayerNameModel.getObject().getName();
        IModel<String> model = new Model<String>(layerName);
        IModel<String> labelModel = new ResourceModel("truncate");

        SimpleAjaxLink<String> link = new SimpleAjaxLink<String>(id, model, labelModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target) {

                dialog.setTitle(new ParamResourceModel("confirmTruncateTitle",
                        CachedLayersPage.this));
                dialog.setDefaultModel(getDefaultModel());

                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Component getContents(final String id) {
                        final String layerName = getDefaultModelObjectAsString();

                        // show a confirmation panel for all the objects we have to remove
                        final GWC gwcFacade = GWC.get();
                        Quota usedQuota = gwcFacade.getUsedQuota(layerName);
                        if (usedQuota == null) {
                            usedQuota = new Quota();
                        }
                        final String usedQuotaStr = usedQuota.toNiceString();
                        IModel<String> model = new ParamResourceModel(
                                "CachedLayersPage.confirmTruncateMessage", CachedLayersPage.this,
                                layerName, usedQuotaStr);
                        Label confirmLabel = new Label(id, model);
                        confirmLabel.setEscapeModelStrings(false);// allow some html inside, like
                                                                  // <b></b>, etc
                        return confirmLabel;
                    }

                    @Override
                    protected boolean onSubmit(final AjaxRequestTarget target,
                            final Component contents) {
                        final String layerName = getDefaultModelObjectAsString();
                        GWC facade = GWC.get();
                        facade.truncate(layerName);
                        return true;
                    }

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.addComponent(table);
                    }
                });
            }
        };

        return link;
    }

    private Component previewLinks(String id, IModel<TileLayer> tileLayerModel) {

        final TileLayer layer = tileLayerModel.getObject();
        if (!layer.isEnabled()) {
            return new Label(id, new ResourceModel("previewDisabled"));
        }
        final Set<String> gridSubsets = new TreeSet<String>(layer.getGridSubsets());
        final List<MimeType> mimeTypes = new ArrayList<MimeType>(layer.getMimeTypes());
        Collections.sort(mimeTypes, new Comparator<MimeType>() {
            @Override
            public int compare(MimeType o1, MimeType o2) {
                return o1.getFormat().compareTo(o2.getFormat());
            }
        });

        Fragment f = new Fragment(id, "menuFragment", CachedLayersPage.this);

        WebMarkupContainer menu = new WebMarkupContainer("menu");

        RepeatingView previewLinks = new RepeatingView("previewLink");

        int i = 0;
        for (String gridSetId : gridSubsets) {
            for (MimeType mimeType : mimeTypes) {
                String label = gridSetId + " / " + mimeType.getFileExtension();
                // build option with text and value
                Label format = new Label(String.valueOf(i++), label);
                String value = "gridSet=" + gridSetId + "&format=" + mimeType.getFormat();
                format.add(new AttributeModifier("value", true, new Model<String>(value)));
                previewLinks.add(format);
            }
        }
        menu.add(previewLinks);

        // build the wms request, redirect to it in a new window, reset the selection
        String demoUrl = "'../gwc/demo/" + layer.getName()
                + "?' + this.options[this.selectedIndex].value";
        menu.add(new AttributeAppender("onchange", new Model<String>("window.open(" + demoUrl
                + ");this.selectedIndex=0"), ";"));

        f.add(menu);
        return f;
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<String>("addNew", NewCachedLayerPage.class));

        // the removal button
        header.add(removal = new CachedLayerSelectionRemovalLink("removeSelected"));
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    private class CachedLayerSelectionRemovalLink extends AjaxLink<TileLayer> {

        private static final long serialVersionUID = 1L;

        public CachedLayerSelectionRemovalLink(String string) {
            super(string);
        }

        @Override
        public void onClick(final AjaxRequestTarget target) {

            List<TileLayer> selection = CachedLayersPage.this.table.getSelection();
            if (selection.isEmpty()) {
                return;
            }

            final List<String> selectedNames = new ArrayList<String>();
            for (TileLayer layer : selection) {
                selectedNames.add(layer.getName());
            }
            dialog.setTitle(new ParamResourceModel("confirmRemoval", CachedLayersPage.this));

            // if there is something to cancel, let's warn the user about what
            // could go wrong, and if the user accepts, let's delete what's needed
            dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                private static final long serialVersionUID = 1L;

                @Override
                protected Component getContents(final String id) {
                    // show a confirmation panel for all the objects we have to remove
                    final GWC gwcFacade = GWC.get();
                    Quota totalQuota = new Quota();
                    for (String layerName : selectedNames) {
                        Quota usedQuota = gwcFacade.getUsedQuota(layerName);
                        if (usedQuota != null) {
                            totalQuota.add(usedQuota);
                        }
                    }
                    final String usedQuotaStr = totalQuota.toNiceString();
                    final Integer selectedLayerCount = selectedNames.size();

                    IModel<String> model = new StringResourceModel(
                            "CachedLayersPage.confirmSelectionRemoval",
                            CachedLayerSelectionRemovalLink.this, null, new Object[] {
                                    selectedLayerCount.toString(), usedQuotaStr });
                    Label confirmLabel = new Label(id, model);
                    confirmLabel.setEscapeModelStrings(false);// allow some html inside, like
                                                              // <b></b>, etc
                    return confirmLabel;
                }

                @Override
                protected boolean onSubmit(final AjaxRequestTarget target, final Component contents) {
                    GWC facade = GWC.get();
                    facade.removeTileLayers(selectedNames);
                    table.clearSelection();
                    return true;
                }

                @Override
                public void onClose(final AjaxRequestTarget target) {
                    // if the selection has been cleared out it's sign a deletion
                    // occurred, so refresh the table
                    List<TileLayer> selection = table.getSelection();
                    if (selection.isEmpty()) {
                        setEnabled(false);
                        target.addComponent(CachedLayerSelectionRemovalLink.this);
                        target.addComponent(table);
                    }
                }
            });
        }
    }

}
