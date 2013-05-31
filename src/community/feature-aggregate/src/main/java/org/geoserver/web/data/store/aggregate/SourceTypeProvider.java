package org.geoserver.web.data.store.aggregate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.data.aggregate.AggregateTypeConfiguration;
import org.geotools.data.aggregate.SourceType;

class SourceTypeProvider extends GeoServerDataProvider<SourceType> {
    private static final long serialVersionUID = -5562124245239213938L;

    public Property<SourceType> STORE = new BeanProperty<SourceType>("storeName",
            "storeName.localPart");

    public Property<SourceType> TYPE = new BeanProperty<SourceType>("typeName", "typeName");

    public Property<SourceType> MAKE_DEFAULT = new PropertyPlaceholder("makeDefault");

    public Property<SourceType> REMOVE = new PropertyPlaceholder("remove");

    IModel<AggregateTypeConfiguration> config;

    DefaultSourceTypeProperty defaultSourceType;

    public SourceTypeProvider(IModel<AggregateTypeConfiguration> config) {
        this.config = config;
        this.defaultSourceType = new DefaultSourceTypeProperty("default", config);
    }

    @Override
    protected List<SourceType> getItems() {
        return config.getObject().getSourceTypes();
    }

    @Override
    protected List<Property<SourceType>> getProperties() {
        return Arrays.asList(STORE, TYPE, defaultSourceType, MAKE_DEFAULT, REMOVE);
    }

    public DefaultSourceTypeProperty getDefaultSourceTypeProperty() {
        return defaultSourceType;
    }

    static final class DefaultSourceTypeProperty extends AbstractProperty<SourceType> {
        IModel<AggregateTypeConfiguration> config;

        public DefaultSourceTypeProperty(String name, IModel<AggregateTypeConfiguration> config) {
            super(name);
            this.config = config;
        }
        
        @Override
        public IModel getModel(IModel itemModel) {
            // TODO Auto-generated method stub
            return super.getModel(itemModel);
        }
        
        public Comparator<SourceType> getComparator() {
            return null;
        }
        
        @Override
        public Object getPropertyValue(final SourceType item) {
            return new IModel<Boolean>() {

                @Override
                public void detach() {
                }

                @Override
                public Boolean getObject() {
                    return config.getObject().getPrimarySourceType().equals(item);
                }

                @Override
                public void setObject(Boolean object) {
                    // read only
                }
            };
        }

    };

}