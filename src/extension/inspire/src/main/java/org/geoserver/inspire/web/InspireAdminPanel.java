package org.geoserver.inspire.web;

import static org.geoserver.inspire.wms.InspireMetadata.LANGUAGE;
import static org.geoserver.inspire.wms.InspireMetadata.SERVICE_METADATA_TYPE;
import static org.geoserver.inspire.wms.InspireMetadata.SERVICE_METADATA_URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.wms.WMSInfo;

/**
 * Panel for the WMS admin page to set the WMS INSPIRE extension preferences.
 */
public class InspireAdminPanel extends AdminPagePanel {

    private static final long serialVersionUID = -7670555379263411393L;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public InspireAdminPanel(final String id, final IModel<WMSInfo> model) {
        super(id, model);

        PropertyModel<MetadataMap> metadata = new PropertyModel<MetadataMap>(model, "metadata");

        add(new LanguageDropDownChoice("language", new MapModel(metadata, LANGUAGE.key)));

        TextField textField = new TextField("metadataURL", new MapModel(metadata,
                SERVICE_METADATA_URL.key));
        add(textField);
        textField.add(new AttributeModifier("title", true, new ResourceModel(
                "InspireAdminPanel.metadataURL.title")));

        final Map<String, String> mdUrlTypes = new HashMap<String, String>();
        mdUrlTypes.put("application/vnd.ogc.csw.GetRecordByIdResponse_xml",
                "CSW GetRecordById Response");
        mdUrlTypes.put("application/vnd.iso.19139+xml", "ISO 19139 ServiceMetadata record");

        IModel<String> urlTypeModel = new MapModel(metadata, SERVICE_METADATA_TYPE.key);

        IChoiceRenderer<String> urlTypeChoiceRenderer = new IChoiceRenderer<String>() {
            private static final long serialVersionUID = 1L;

            public Object getDisplayValue(final String key) {
                final String resourceKey = "InspireAdminPanel.metadataURLType." + key;// as found in
                                                                                      // GeoServerApplication.properties
                final String defaultValue = key;
                final String displayValue = new ResourceModel(resourceKey, defaultValue)
                        .getObject();
                return displayValue;
            }

            public String getIdValue(final String key, int index) {
                return key;
            }
        };
        List<String> urlTypeChoices = new ArrayList<String>(mdUrlTypes.keySet());
        DropDownChoice<String> serviceMetadataRecordType = new DropDownChoice<String>(
                "metadataURLType", urlTypeModel, urlTypeChoices, urlTypeChoiceRenderer);

        add(serviceMetadataRecordType);
    }
}
