/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.gridset;

import java.util.Collections;
import java.util.logging.Logger;

import javax.measure.unit.Unit;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.SRSListPanel;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;

abstract class AbstractGridSetPage extends GeoServerSecuredPage {

    protected static final Logger LOGGER = Logging.getLogger(AbstractGridSetPage.class);

    /**
     * Name of the page parameter that determines which gridset to edit
     */
    public static final String GRIDSET_NAME = "gridSet";

    /**
     * Name of page parameter that holds the name of an existing gridset to use as template to
     * create a new one
     */
    public static final String GRIDSET_TEMPLATE_NAME = "template";

    protected final Form<GridSetInfo> form;

    protected final TextParamPanel name;

    protected final Component description;

    protected final GridSetCRSPanel crs;

    protected final TextParamPanel tileWidth;

    protected final TextParamPanel tileHeight;

    protected final EnvelopePanel bounds;

    protected final Component computeBoundsLink;

    protected final Component cancelLink;

    protected final Component saveLink;

    protected final TileMatrixSetEditor tileMatrixSetEditor;

    protected final Component addLevelLink;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public AbstractGridSetPage(final PageParameters parameters) {

        final String gridSetName;
        final String templateName;
        if (parameters == null) {
            gridSetName = null;
            templateName = null;
        } else {
            gridSetName = parameters.getString(GRIDSET_NAME);
            templateName = parameters.getString(GRIDSET_TEMPLATE_NAME);
        }

        GridSetInfo gridsetInfo;
        if (templateName != null) {
            gridsetInfo = getInfo(templateName);
            gridsetInfo.setName("My_" + gridsetInfo.getName());
            gridsetInfo.setInternal(false);
        } else if (gridSetName != null) {
            gridsetInfo = getInfo(gridSetName);
        } else {
            gridsetInfo = getInfo(null);
        }

        IModel<GridSetInfo> model = new Model<GridSetInfo>(gridsetInfo);

        form = new Form<GridSetInfo>("gridSetForm", model);
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        form.add(name = name(model));
        form.add(description = new TextArea<String>("description", new PropertyModel<String>(model,
                "description")));
        form.add(crs = crs(model));
        form.add(bounds = bounds(model));
        form.add(computeBoundsLink = computeBoundsLink(form));
        form.add(tileWidth = tileWidth(model));
        form.add(tileHeight = tileHeight(model));

        form.add(tileMatrixSetEditor = new TileMatrixSetEditor("tileMatrixSetEditor", model));
        tileMatrixSetEditor.setOutputMarkupId(true);

        cancelLink = new BookmarkablePageLink("cancel", GridSetsPage.class);
        form.add(cancelLink);

        saveLink = saveLink(form);
        form.add(saveLink);
        add(form);

        tileWidth.getFormComponent().add(new AjaxFormComponentUpdatingBehavior("onblur") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(tileMatrixSetEditor);
            }
        });
        tileHeight.getFormComponent().add(new AjaxFormComponentUpdatingBehavior("onblur") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(tileMatrixSetEditor);
            }
        });

        addLevelLink = new GeoServerAjaxFormLink("addZoomLevel", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                addZoomLevel(target);
                target.addComponent(tileMatrixSetEditor);
            }
        };

        form.add(addLevelLink);

    }

    void addZoomLevel(AjaxRequestTarget target) {
        crs.processInput();
        bounds.processInput();
        tileWidth.getFormComponent().processInput();
        tileHeight.getFormComponent().processInput();

        ReferencedEnvelope bbox = (ReferencedEnvelope) bounds.getModelObject();
        if (null == bbox) {
            String message = new ResourceModel("AbstractGridSetPage.cantAddZoomLevel").getObject();
            FeedbackPanel feedback = (FeedbackPanel) form.get("feedback");
            if (feedback != null) {
                feedback.error(message);
                target.addComponent(feedback);
            } else {
                form.error(message);
            }
            return;
        }
        Integer width = (Integer) tileWidth.getFormComponent().getModelObject();
        Integer height = (Integer) tileHeight.getFormComponent().getModelObject();

        tileMatrixSetEditor.addZoomLevel(bbox, width == null ? 256 : width, height == null ? 256
                : height);
    }

    private Component computeBoundsLink(Form<GridSetInfo> form) {

        GeoServerAjaxFormLink link = new GeoServerAjaxFormLink("computeBounds", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                computeBounds();
                target.addComponent(bounds);
                target.addComponent(tileMatrixSetEditor);
            }
        };
        return link;
    }

    void computeBounds() {
        // perform manual processing of the required fields
        crs.processInput();
        bounds.processInput();
        CoordinateReferenceSystem coordSys;
        coordSys = (CoordinateReferenceSystem) crs.getModelObject();
        if (coordSys == null) {
            bounds.error(new ResourceModel("AbstractGridsetPage.computeBounds.crsNotSet"));
            return;
        }
        GWC mediator = GWC.get();
        ReferencedEnvelope aov = mediator.getAreaOfValidity(coordSys);
        if (aov == null) {
            bounds.error(new ResourceModel("AbstractGridsetPage.computeBounds.aovNotSet"));
        } else {
            bounds.setModelObject(aov);
        }
    }

    private EnvelopePanel bounds(IModel<GridSetInfo> model) {

        class UpdatingEnvelopePanel extends EnvelopePanel {
            private static final long serialVersionUID = 1L;

            public UpdatingEnvelopePanel(String id, IModel<ReferencedEnvelope> e) {
                super(id, e);

                class UpdateTableBehavior extends AjaxFormSubmitBehavior {
                    private static final long serialVersionUID = 1L;

                    public UpdateTableBehavior() {
                        super(form, "onblur");
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        target.addComponent(AbstractGridSetPage.this.tileMatrixSetEditor);
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        UpdatingEnvelopePanel.this.setModelObject(null);
                        target.addComponent(AbstractGridSetPage.this.tileMatrixSetEditor);
                    }
                }

                minXInput.add(new UpdateTableBehavior());
                minYInput.add(new UpdateTableBehavior());
                maxXInput.add(new UpdateTableBehavior());
                maxYInput.add(new UpdateTableBehavior());
            }
        }

        PropertyModel<ReferencedEnvelope> boundsModel;
        boundsModel = new PropertyModel<ReferencedEnvelope>(model, "bounds");

        EnvelopePanel panel = new UpdatingEnvelopePanel("bounds", boundsModel);
        panel.setRequired(true);
        panel.setOutputMarkupId(true);

        return panel;
    }

    private TextParamPanel tileHeight(IModel<GridSetInfo> model) {
        TextParamPanel panel = new TextParamPanel("tileHeight", new PropertyModel<Integer>(model,
                "tileHeight"), new ResourceModel("AbstractGridSetPage.tileHeight"), true,
                new RangeValidator<Integer>(16, 2048));
        return panel;
    }

    private TextParamPanel tileWidth(IModel<GridSetInfo> model) {
        TextParamPanel panel = new TextParamPanel("tileWidth", new PropertyModel<Integer>(model,
                "tileWidth"), new ResourceModel("AbstractGridSetPage.tileWidth"), true,
                new RangeValidator<Integer>(16, 2048));
        return panel;
    }

    /**
     * @param model
     * @return
     */
    private GridSetCRSPanel crs(IModel<GridSetInfo> model) {
        GridSetCRSPanel crsPanel = new GridSetCRSPanel("crs", model);
        return crsPanel;
    }

    /**
     * @author groldan
     * 
     */
    protected static class GridSetCRSPanel extends CRSPanel {
        private static final long serialVersionUID = 1L;

        private Label units;

        private Label metersPerUnit;

        private IModel<GridSetInfo> infoModel;

        @SuppressWarnings("serial")
        public GridSetCRSPanel(String id, IModel<GridSetInfo> model) {
            super(id, new PropertyModel<CoordinateReferenceSystem>(model, "crs"));
            this.infoModel = model;
            units = new Label("units", new Model<String>());
            metersPerUnit = new Label("metersPerUnit", new Model<String>());
            units.setOutputMarkupId(true);
            metersPerUnit.setOutputMarkupId(true);

            updateUnits((CoordinateReferenceSystem) getModelObject());

            add(units);
            add(metersPerUnit);

            super.srsTextField.add(new AjaxFormComponentUpdatingBehavior("onblur") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    convertInput();

                    CoordinateReferenceSystem crs = (CoordinateReferenceSystem) getConvertedInput();
                    if (crs != null) {
                        setModelObject(crs);
                        wktLabel.setDefaultModelObject(crs.getName().toString());
                        wktLink.setEnabled(true);
                    } else {
                        wktLabel.setDefaultModelObject(null);
                        wktLink.setEnabled(false);
                    }
                    target.addComponent(wktLink);
                    target.addComponent(units);
                    target.addComponent(metersPerUnit);
                }
            });
        }

        @Override
        protected SRSListPanel srsListPanel() {
            SRSListPanel srsList = new SRSListPanel(popupWindow.getContentId()) {

                @Override
                protected void onCodeClicked(AjaxRequestTarget target, String epsgCode) {
                    popupWindow.close(target);

                    String srs = "EPSG:" + epsgCode;
                    srsTextField.setModelObject(srs);
                    target.addComponent(srsTextField);

                    CoordinateReferenceSystem crs = fromSRS(srs);
                    wktLabel.setDefaultModelObject(crs.getName().toString());
                    wktLink.setEnabled(true);
                    target.addComponent(wktLink);
                    updateUnits(crs);
                    target.addComponent(units);
                    target.addComponent(metersPerUnit);
                }
            };
            srsList.setCompactMode(true);
            return srsList;
        }

        @Override
        protected void convertInput() {
            try {
                super.convertInput();
            } finally {
                updateUnits();
            }
        }

        @Override
        protected void onBeforeRender() {
            // updateUnits();
            super.onBeforeRender();
        }

        private void updateUnits() {
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) getConvertedInput();
            updateUnits(crs);
        }

        private void updateUnits(CoordinateReferenceSystem crs) {
            if (crs == null) {
                units.setDefaultModelObject("--");
                metersPerUnit.setDefaultModelObject("--");
            } else {
                Double meters = infoModel.getObject().getMetersPerUnit(crs);
                metersPerUnit.setDefaultModelObject(String.valueOf(meters));

                CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(0);
                final Unit<?> unit = axis.getUnit();
                final String unitStr = unit.toString();
                units.setDefaultModelObject(unitStr);
            }
        }

    }

    private TextParamPanel name(IModel<GridSetInfo> model) {
        TextParamPanel namePanel = new TextParamPanel("name", new PropertyModel<String>(model,
                "name"), new ResourceModel("AbstractGridSetPage.name"), true,
                new UniqueNameValidator(model.getObject().getName()));
        return namePanel;
    }

    private GridSetInfo getInfo(final String gridSetName) {
        GridSetInfo gridsetInfo;

        if (gridSetName == null) {
            gridsetInfo = new GridSetInfo();
        } else {
            GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();
            GridSet gridSet = gridSetBroker.get(gridSetName);
            if (gridSet == null) {
                throw new IllegalArgumentException("Requested GridSet does not exist: '"
                        + gridSetName + "'");
            }

            final String name = gridSet.getName();
            final boolean internal = GWC.get().isInternalGridSet(name);

            gridsetInfo = new GridSetInfo(gridSet, internal);
        }
        return gridsetInfo;
    }

    private Component saveLink(Form<GridSetInfo> form) {
        return new AjaxSubmitLink("save", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                super.onError(target, form);
                target.addComponent(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onSave(target, form);
            }

        };
    }

    protected abstract void onSave(AjaxRequestTarget target, Form<?> form);

    private static class UniqueNameValidator extends AbstractValidator<String> {
        private static final long serialVersionUID = 1L;

        private final String previousName;

        /**
         * @param previousName
         *            the initial name of the gridset when the page loaded, may be {@code null} only
         *            in case we're creating a new gridset
         */
        public UniqueNameValidator(final String previousName) {
            this.previousName = previousName;
        }

        @Override
        protected void onValidate(IValidatable<String> validatable) {
            final String name = validatable.getValue();
            if (name.equals(previousName)) {
                return;
            }
            final GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();
            if (previousName != null) {
                gridSetBroker.get(previousName);
            }
            GridSet gridSet = gridSetBroker.get(name);
            if (gridSet != null) {
                error(validatable, "gridSetAlreadyExists",
                        Collections.singletonMap("name", (Object) name));
            }
        }
    }

}
