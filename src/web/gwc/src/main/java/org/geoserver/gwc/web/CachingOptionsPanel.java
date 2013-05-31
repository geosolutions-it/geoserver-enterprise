/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.gwc.config.GWCConfig;

public class CachingOptionsPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private CheckGroup<String> otherFormatsGroup;

    private CheckGroup<String> rasterFormatsGroup;

    private CheckGroup<String> vectorFormatsGroup;

    public CachingOptionsPanel(final String id, final IModel<GWCConfig> gwcConfigModel) {

        super(id, gwcConfigModel);

        final IModel<Boolean> autoCacheLayersModel;
        autoCacheLayersModel = new PropertyModel<Boolean>(gwcConfigModel, "cacheLayersByDefault");
        final CheckBox autoCacheLayers = new CheckBox("cacheLayersByDefault", autoCacheLayersModel);
        add(autoCacheLayers);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final WebMarkupContainer configs = new WebMarkupContainer("configs");
        configs.setOutputMarkupId(true);
        configs.setVisible(autoCacheLayersModel.getObject().booleanValue());
        container.add(configs);

        autoCacheLayers.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                final boolean visibleConfigs = autoCacheLayers.getModelObject().booleanValue();
                configs.setVisible(visibleConfigs);
                target.addComponent(container);
            }
        });

        IModel<Boolean> nonDefaultStylesModel;
        nonDefaultStylesModel = new PropertyModel<Boolean>(gwcConfigModel, "cacheNonDefaultStyles");
        CheckBox cacheNonDefaultStyles = new CheckBox("cacheNonDefaultStyles",
                nonDefaultStylesModel);
        configs.add(cacheNonDefaultStyles);

        List<Integer> metaTilingChoices = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
                14, 16, 16, 17, 18, 19, 20);
        IModel<Integer> metaTilingXModel = new PropertyModel<Integer>(gwcConfigModel, "metaTilingX");
        DropDownChoice<Integer> metaTilingX = new DropDownChoice<Integer>("metaTilingX",
                metaTilingXModel, metaTilingChoices);
        metaTilingX.setRequired(true);
        configs.add(metaTilingX);

        IModel<Integer> metaTilingYModel = new PropertyModel<Integer>(gwcConfigModel, "metaTilingY");
        DropDownChoice<Integer> metaTilingY = new DropDownChoice<Integer>("metaTilingY",
                metaTilingYModel, metaTilingChoices);
        metaTilingY.setRequired(true);
        configs.add(metaTilingY);

        IModel<Integer> gutterModel = new PropertyModel<Integer>(gwcConfigModel, "gutter");
        List<Integer> gutterChoices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50,
                100);
        DropDownChoice<Integer> gutterChoice = new DropDownChoice<Integer>("gutter", gutterModel,
                gutterChoices);
        configs.add(gutterChoice);

        final List<String> formats = Arrays.asList("image/png", "image/png8", "image/jpeg",
                "image/gif");

        {
            IModel<List<String>> vectorFormatsModel = new PropertyModel<List<String>>(
                    gwcConfigModel, "defaultVectorCacheFormats");
            vectorFormatsGroup = new CheckGroup<String>("vectorFormatsGroup", vectorFormatsModel);
            configs.add(vectorFormatsGroup);
            ListView<String> formatsList = new ListView<String>("vectorFromats", formats) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    item.add(new Check<String>("vectorFormatsOption", item.getModel()));
                    item.add(new Label("name", item.getModel()));
                }
            };
            formatsList.setReuseItems(true);// otherwise it looses state on invalid form submits
            vectorFormatsGroup.add(formatsList);
        }

        {
            IModel<List<String>> rasterFormatsModel = new PropertyModel<List<String>>(
                    gwcConfigModel, "defaultCoverageCacheFormats");
            rasterFormatsGroup = new CheckGroup<String>("rasterFormatsGroup", rasterFormatsModel);
            configs.add(rasterFormatsGroup);
            ListView<String> formatsList = new ListView<String>("rasterFromats", formats) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    item.add(new Check<String>("rasterFormatsOption", item.getModel()));
                    item.add(new Label("name", item.getModel()));
                }
            };
            formatsList.setReuseItems(true);// otherwise it looses state on invalid form submits
            rasterFormatsGroup.add(formatsList);
        }
        {
            IModel<List<String>> otherFormatsModel = new PropertyModel<List<String>>(
                    gwcConfigModel, "defaultOtherCacheFormats");
            otherFormatsGroup = new CheckGroup<String>("otherFormatsGroup", otherFormatsModel);
            configs.add(otherFormatsGroup);
            ListView<String> formatsList = new ListView<String>("otherFromats", formats) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    item.add(new Check<String>("otherFormatsOption", item.getModel()));
                    item.add(new Label("name", item.getModel()));
                }
            };
            formatsList.setReuseItems(true);// otherwise it looses state on invalid form submits
            otherFormatsGroup.add(formatsList);
        }

        IModel<Set<String>> cachedGridsetsModel = new PropertyModel<Set<String>>(gwcConfigModel,
                "defaultCachingGridSetIds");
        DefaultGridsetsEditor cachedGridsets = new DefaultGridsetsEditor("cachedGridsets",
                cachedGridsetsModel);
        configs.add(cachedGridsets);

        cachedGridsets.add(new IValidator<Set<String>>() {

            private static final long serialVersionUID = 1L;

            @Override
            public void validate(IValidatable<Set<String>> validatable) {
                boolean validate = autoCacheLayersModel.getObject().booleanValue();
                if (validate && validatable.getValue().isEmpty()) {
                    ValidationError error = new ValidationError();
                    error.setMessage(new ResourceModel(
                            "CachingOptionsPanel.validation.emptyGridsets").getObject());
                    validatable.error(error);
                }
            }
        });

        class FormatsValidator implements IValidator<Collection<String>> {
            private static final long serialVersionUID = 1L;

            @Override
            public void validate(IValidatable<Collection<String>> validatable) {
                boolean validate = autoCacheLayersModel.getObject().booleanValue();
                Collection<String> value = validatable.getValue();
                if (validate && value.isEmpty()) {
                    ValidationError error = new ValidationError();
                    error.setMessage(new ResourceModel(
                            "CachingOptionsPanel.validation.emptyCacheFormatList").getObject());
                    validatable.error(error);
                }
            }
        }
        ;
        FormatsValidator validator = new FormatsValidator();
        vectorFormatsGroup.add(validator);
        rasterFormatsGroup.add(validator);
        otherFormatsGroup.add(validator);
    }
}
