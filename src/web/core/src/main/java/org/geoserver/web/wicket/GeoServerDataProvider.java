/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

/**
 * GeoServer specific data provider. In addition to the services provided by a SortableDataProvider
 * it can perform keyword based filtering, enum the model properties used for display and sorting.
 * 
 * Implementors of providers for editable tables need to remember to raise the {@link #editable} flag. 
 * 
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class GeoServerDataProvider<T> extends SortableDataProvider {
    static final Logger LOGGER = Logging.getLogger(GeoServerDataProvider.class);

    /**
     * Keywords used for filtering data
     */
    protected String[] keywords;

    /**
     * regular expression matchers, one per keyword
     */
    private transient Matcher[] matchers;

    /**
     * A cache used to avoid recreating models over and over, this make it possible
     * to make {@link GeoServerTablePanel} editable
     */
    Map<T, IModel> modelCache = new IdentityHashMap<T, IModel>();
    
    /**
     * Sets the data provider as editable, in that case the models should be preserved
     */
    boolean editable = false;
    
    
    /**
     * Returns true if this data provider is setup for editing (it will reuse models). Defaults to false
     * @return
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Sets the data provider as editable/non editable
     * @param editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Returns the current filtering keywords
     * 
     * @return
     */
    public String[] getKeywords() {
        return keywords;
    }

    /**
     * Sets the keywords used for filtering
     * 
     * @param keywords
     */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
        this.matchers = null;
    }

    /**
     * @return a regex matcher for each search keyword
     */
    private Matcher[] getMatchers() {
        if (matchers != null) {
            return matchers;
        }

        if (keywords == null) {
            return new Matcher[0];
        }

        // build the case insensitive regex patterns
        matchers = new Matcher[keywords.length];

        String keyword;
        String regex;
        Pattern pattern;
        for (int i = 0; i < keywords.length; i++) {
            keyword = keywords[i];
            regex = ".*" + escape(keyword) + ".*";
            pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            matchers[i] = pattern.matcher("");
        }

        return matchers;
    }

    /**
     * Escape any character that's special for the regex api
     * 
     * @param keyword
     * @return
     */
    private String escape(String keyword) {
        final String escapeSeq = "\\";
        final int len = keyword.length();
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < len; i++) {
            c = keyword.charAt(i);
            if (isSpecial(c)) {
                sb.append(escapeSeq);
            }
            sb.append(keyword.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Convenience method to determine if a character is special to the regex system.
     * 
     * @param chr
     *            the character to test
     * 
     * @return is the character a special character.
     */
    private boolean isSpecial(final char chr) {
        return ((chr == '.') || (chr == '?') || (chr == '*') || (chr == '^') || (chr == '$')
                || (chr == '+') || (chr == '[') || (chr == ']') || (chr == '(') || (chr == ')')
                || (chr == '|') || (chr == '\\') || (chr == '&'));
    }

    /**
     * Returns the application singleton.
     */
    protected GeoServerApplication getApplication() {
        return GeoServerApplication.get();
    }
    /**
     * Provides catalog access for the provider (cannot be stored as a field, this class is going to
     * be serialized)
     * 
     * @return
     */
    protected Catalog getCatalog() {
        return getApplication().getCatalog();
    }

    public Iterator<T> iterator(int first, int count) {
        List<T> items = getFilteredItems();

        // global sorting
        Comparator<T> comparator = getComparator(getSort());
        if (comparator != null) {
            Collections.sort(items, comparator);
        }

        // in memory paging
        int last = first + count;
        if (last > items.size())
            last = items.size();
        return items.subList(first, last).iterator();
    }

    /**
     * Returns a filtered list of items. Subclasses can override if they have a more efficient way
     * of filtering than in memory keyword comparison
     * 
     * @return
     */
    protected List<T> getFilteredItems() {
        List<T> items = getItems();

        // if needed, filter
        if (keywords != null && keywords.length > 0) {
            return filterByKeywords(items);
        } else {
            // make a deep copy anyways, the catalog does not do that for us
            return new ArrayList<T>(items);
        }
    }

    /**
     * Returns the size of the filtered item collection
     */
    public int size() {
        return getFilteredItems().size();
    }

    /**
     * Returns the global size of the collection, without filtering it
     * 
     * @return
     */
    public int fullSize() {
        return getItems().size();
    }

    private List<T> filterByKeywords(List<T> items) {
        List<T> result = new ArrayList<T>();

        final Matcher[] matchers = getMatchers();

        List<Property<T>> properties = getProperties();
        for (T item : items) {
            ITEM:
            // find any match of any pattern over any property
            for (Property<T> property : properties) {
                if(property.isSearchable()) {
                    Object value = property.getPropertyValue(item);
                    if (value != null) {
                        // brute force check for keywords
                        for (Matcher matcher : matchers) {
                            matcher.reset(String.valueOf(value));
                            if (matcher.matches()) {
                                result.add(item);
                                break ITEM;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns only the properties that have been marked as visible
     * 
     * @return
     */
    List<Property<T>> getVisibleProperties() {
        List<Property<T>> results = new ArrayList<Property<T>>();
        for (Property<T> p : getProperties()) {
            if (p.isVisible())
                results.add(p);
        }
        return results;
    }

    /**
     * Returns the list of properties served by this provider. The property keys are used to
     * establish the layer sorting, whilst the Property itself is used to extract the value of the
     * property from the item.
     * 
     * @return
     */
    protected abstract List<Property<T>> getProperties();

    /**
     * Returns a non filtered list of all the items the provider must return
     * 
     * @return
     */
    protected abstract List<T> getItems();

    /**
     * Returns a comparator given the sort property.
     * 
     * @param sort
     * @return
     */
    protected Comparator<T> getComparator(SortParam sort) {
        if (sort == null || sort.getProperty() == null)
            return null;

        for (Property<T> property : getProperties()) {
            if (sort.getProperty().equals(property.getName())) {
                Comparator<T> comparator = property.getComparator();
                if (comparator != null) {
                    if (!sort.isAscending())
                        return new ReverseComparator<T>(comparator);
                    else
                        return comparator;
                }
            }
        }
        LOGGER.log(Level.WARNING, "Could not find any comparator " + "for sort property "
                + sort.getProperty());

        return null;
    }
    
    /**
     * This implementation uses the {@link #modelCache} to avoid recreating over and over
     * different models for the various items, this allows the grid panel to be editable
     */
    public final IModel model(Object object) {
        if(editable) {
            IModel result = modelCache.get((T) object);
            if(result == null) {
                result = newModel(object);
                modelCache.put((T) object, result);
            }
            return result;
        } else {
            return newModel(object);
        }
    }
    
    /**
     * Simply wraps the object into a Model assuming the Object is serializable. Subclasses
     * can override this
     * @param object
     * @return
     */
    protected IModel newModel(Object object) {
        return new Model((Serializable) object);
    }

    /**
     * Simply models the concept of a property in this provider. A property has a key, that
     * identifies it and can be used for i18n, and can return the value of the property given an
     * item served by the {@link GeoServerDataProvider}
     * 
     * @author Andrea Aime - OpenGeo
     * 
     * @param <T>
     */
    public interface Property<T> extends Serializable {
        public String getName();

        /**
         * Given the item, returns the property
         * 
         * @param item
         * @return
         */
        public Object getPropertyValue(T item);

        /**
         * Given the item model, returns a model for the property value
         * 
         * @param itemModel
         * @return
         */
        public IModel getModel(IModel itemModel);

        /**
         * Allows for sorting the property
         * 
         * @return
         */
        public Comparator<T> getComparator();

        /**
         * If false the property will be used for searches, but not shown in the table
         */
        public boolean isVisible();
        

        /**
         * Returns true if it makes sense to search over this property
         * @return
         */
        public boolean isSearchable();
    }
    
    /**
     * Base property class. Assumes T is serializable, if it's not, manually override
     * the getModel() method
     */
    public abstract static class AbstractProperty<T> implements Property<T> {
        String name;
        boolean visible;
        
        public AbstractProperty(String name) {
            this(name, true);
        }
        
        public AbstractProperty(String name, boolean visible) {
            this.name = name;
            this.visible = visible;
        }
        
        public Comparator<T> getComparator() {
            return new PropertyComparator<T>(this);
        }

        /**
         * Returns a model based on the getPropertyValue(...) result. Mind, this is
         * not suitable for editable tables, if you need to make one you'll have to
         * roll your own getModel() implementation ( {@link BeanProperty} provides a good example)
         */
        public IModel getModel(IModel itemModel) {
            Object value = getPropertyValue((T) itemModel.getObject());
            if(value instanceof IModel)
                return (IModel) value;
            else
                return new Model((Serializable) value);
        }

        public String getName() {
            return name;
        }

        public boolean isVisible() {
            return visible;
        }
        
        @Override
        public String toString() {
            return "Property[" + name + "]";
        }
        
        public boolean isSearchable() {
            return true;
        }
    }

    /**
     * A Property implementation that uses BeanUtils to access a bean properties
     * 
     * @author Andrea Aime - OpenGeo
     * 
     * @param <T>
     */
    public static class BeanProperty<T> extends AbstractProperty<T> {
        String propertyPath;

        public BeanProperty(String key, String propertyPath) {
            this(key, propertyPath, true);
        }

        public BeanProperty(String key, String propertyPath, boolean visible) {
            super(key, visible);
            this.propertyPath = propertyPath;
        }
        
        /**
         * Overrides the base class {@link #getModel(IModel)} to allow for editable
         * tables: uses a property model against the bean so that writes will hit the
         * bean instead of the possibly immutable values contained in it (think a String property)
         */
        public IModel getModel(IModel itemModel) {
            return new PropertyModel(itemModel, propertyPath);
        }

        public Object getPropertyValue(T bean) {
            // allow rest of the code to assume bean != null
            if (bean == null)
                return null;

            try {
                return PropertyUtils.getProperty(bean, propertyPath);
            } catch(NestedNullException nne) {
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Could not find property " + propertyPath + " in "
                        + bean.getClass(), e);
            }
        }

        @Override
        public String toString() {
            return "BeanProperty[" + name + "]";
        }
    }

    /**
     * Placeholder for a column that does not contain a real property (for example, a column
     * containing commands instead of data). Will return the item model as the model, and as the
     * property value.
     * 
     * @author Andrea Aime
     * 
     * @param <T>
     */
    public static class PropertyPlaceholder<T> implements Property<T> {
        String name;

        public PropertyPlaceholder(String name) {
            this.name = name;
        }

        public Comparator<T> getComparator() {
            return null;
        }

        public IModel getModel(IModel itemModel) {
            return itemModel;
        }

        public String getName() {
            return name;
        }

        public Object getPropertyValue(T item) {
            return item;
        }

        public boolean isVisible() {
            // the very reason for placeholder existence
            // is to show up in the table
            return true;
        }

        @Override
        public String toString() {
            return "PropertyPlacehoder[" + name + "]";
        }

        public boolean isSearchable() {
            return false;
        }
    }

    /**
     * Uses {@link Property} to extract the values, and then compares them assuming they are
     * {@link Comparable}
     * 
     * @param <T>
     */
    public static class PropertyComparator<T> implements Comparator<T> {
        Property<T> property;

        public PropertyComparator(Property<T> property) {
            this.property = property;
        }

        public int compare(T o1, T o2) {
            Comparable p1 = (Comparable) property.getPropertyValue(o1);
            Comparable p2 = (Comparable) property.getPropertyValue(o2);

            // what if any property is null? We assume null < (not null)
            if (p1 == null)
                return p2 != null ? -1 : 0;
            else if (p2 == null)
                return 1;

            return p1.compareTo(p2);
        }

    }

    /**
     * A simple comparator inverter
     * 
     * @author Andrea Aime - OpenGeo
     * 
     * @param <T>
     */
    private static class ReverseComparator<T> implements Comparator<T> {
        Comparator<T> comparator;

        public ReverseComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }

        public int compare(T o1, T o2) {
            return comparator.compare(o1, o2) * -1;
        }

    }

}
