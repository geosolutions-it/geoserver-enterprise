package org.geoserver.web.data.store.aggregate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.feature.type.Name;

class SourcesProvider extends GeoServerDataProvider<Map.Entry<Name, String>> {
    private static final long serialVersionUID = -2886801617500717930L;

    public static Property<Map.Entry<Name, String>> NAME = new AbstractProperty<Map.Entry<Name, String>>(
            "store") {

        @Override
        public Object getPropertyValue(Entry<Name, String> item) {
            return item.getKey();
        }
    };

    public static Property<Map.Entry<Name, String>> TYPE = new AbstractProperty<Map.Entry<Name, String>>(
            "type") {

        @Override
        public Object getPropertyValue(Map.Entry<Name, String> item) {
            return item.getValue();
        }
    };

    static List PROPERTIES = Arrays.asList(NAME, TYPE);

    List<Map.Entry<Name, String>> items;

    public SourcesProvider(List<Map.Entry<Name, String>> items) {
        this.items = items;
    }

    @Override
    protected List<Map.Entry<Name, String>> getItems() {
        return items;
    }

    @Override
    protected List<Property<Map.Entry<Name, String>>> getProperties() {
        return PROPERTIES;
    }

}