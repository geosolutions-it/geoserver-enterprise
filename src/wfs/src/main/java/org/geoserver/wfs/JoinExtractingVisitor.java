 package org.geoserver.wfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.Join;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.filter.visitor.FilterVisitorSupport;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.temporal.BinaryTemporalOperator;

public class JoinExtractingVisitor extends FilterVisitorSupport {

    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
    
    FeatureTypeInfo primaryFeatureType;
    String primaryAlias;
    List<FeatureTypeInfo> featureTypes;
    List<String> aliases;

    List<Filter> joinFilters = new ArrayList<Filter>();
    List<Filter> filters = new ArrayList<Filter>();

    public JoinExtractingVisitor(List<FeatureTypeInfo> featureTypes, List<String> aliases) {
        this.primaryFeatureType = featureTypes.get(0);
        this.featureTypes = featureTypes.subList(1, featureTypes.size());
        
        if (aliases == null || aliases.isEmpty()) {
            //assign prefixes
            aliases = new ArrayList<String>();
            for (int i = 0; i < featureTypes.size(); i++) {
                aliases.add(String.valueOf((char)('a' + i)));
            }
        }
        
        this.primaryAlias = aliases.get(0);
        this.aliases = aliases.subList(1, aliases.size());
    }

    public Object visitNullFilter(Object extraData) {
        return null;
    }

    public Object visit(ExcludeFilter filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(IncludeFilter filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(Id filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(Not filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(PropertyIsBetween filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(PropertyIsLike filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(PropertyIsNull filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    public Object visit(PropertyIsNil filter, Object extraData) {
        return handleOther(filter, extraData);
    }

    @Override
    protected Object visit(BinaryLogicOperator op, Object extraData) {
        for (Filter f : op.getChildren()) {
            f.accept(this, extraData);
        }
        return extraData;
    }

    @Override
    protected Object visit(BinaryComparisonOperator op, Object extraData) {
        return handle(op, op.getExpression1(), op.getExpression2(), extraData);
    }

    @Override
    protected Object visit(BinarySpatialOperator op, Object extraData) {
        return handle(op, op.getExpression1(), op.getExpression2(), extraData);
    }

    @Override
    protected Object visit(BinaryTemporalOperator op, Object extraData) {
        return handle(op, op.getExpression1(), op.getExpression2(), extraData);
    }

//    void handle(Filter f) {
//        if (f instanceof And) {
//            for (Filter g : ((And)f).getChildren()) {
//                handle(g);
//            }
//        }
//        else {
//            //check if this is a join filter
//            boolean join = false;
//            if (f instanceof BinaryComparisonOperator) {
//                join = isJoinFilter(((BinaryComparisonOperator)f).getExpression1(), 
//                    ((BinaryComparisonOperator)f).getExpression2());
//            }
//            else if (f instanceof BinarySpatialOperator) {
//                join = isJoinFilter(((BinarySpatialOperator)f).getExpression1(), 
//                        ((BinarySpatialOperator)f).getExpression2());
//            }
//            else if (f instanceof BinaryTemporalOperator) {
//                join = isJoinFilter(((BinaryTemporalOperator)f).getExpression1(), 
//                        ((BinaryTemporalOperator)f).getExpression2());
//            }
//            
//            if (join) {
//                joinFilters.add(f);
//            }
//            else {
//                filters.add(f);
//            }
//        }
//    }

    Object handle(Filter f, Expression e1, Expression e2, Object extraData) {
        if (isJoinFilter(e1, e2)) {
            joinFilters.add(f);
        }
        else {
            handleOther(f, extraData);
        }
        return null;
    }

    Object handleOther(Filter f, Object extraData) {
        filters.add(f);
        return null;
    }
    
    boolean isJoinFilter(Expression e1, Expression e2) {
        return e1 instanceof PropertyName && e2 instanceof PropertyName;
    }

    public List<Join> getJoins() {
        List<Join> joins = new ArrayList();
 
        //unroll the contents of the join filters and rewrite them and and assign to correct 
        //feature type
        List<Filter> joinFilters = rewriteAndSort(unroll(this.joinFilters), true);

        //do same for other secondary filters
        List<Filter> otherFilters = rewriteAndSort(unroll(this.filters), false);
        
        for (int i = 0; i < featureTypes.size(); i++) {
            Join join = new Join(featureTypes.get(i).getNativeName(), joinFilters.get(i+1));
            if (aliases != null) {
                join.setAlias(aliases.get(i));
            }
            if (otherFilters.get(i+1) != null) {
                join.setFilter(otherFilters.get(i+1));
            }
            joins.add(join);
        }

        return joins;
    }

    public Filter getPrimaryFilter() {
        List<Filter> otherFilters = rewriteAndSort(unroll(this.filters), false);
        return otherFilters.get(0);
    }
    
    List<Filter> unroll(List<Filter> filters) {
        JoinFilterUnroller unroller = new JoinFilterUnroller();
        for (Filter f : filters) {
            f.accept(unroller, null);
        }
        return unroller.getFilters();
    }

    List<Filter> rewriteAndSort(List<Filter> filters, boolean prefix) {
        Filter[] sorted = new Filter[featureTypes.size()+1];
O:      for (Filter f : filters) {
            PropertyName[] names = names(f);
            
            //find the secondary feature type referenced
            for (int i = 0; i < featureTypes.size(); i++) {
                PropertyName[] rewritten = rewrite(i, names[0], names[1], prefix);
                if (rewritten != null) {
                    Filter newFilter = rewrite(f, names, rewritten);
                    updateFilter(sorted, i+1, newFilter);
                    continue O;
                }
            }

            //this could be a filter against the primary
            PropertyName[] rewritten = 
                rewrite(primaryFeatureType, primaryAlias,names[0],names[1],prefix);
            if (rewritten != null) {
                Filter newFilter = rewrite(f, names, rewritten);
                updateFilter(sorted, 0, newFilter);
            }
            else {
                throw new IllegalStateException("Join filter inconsistent with regard to feature types");
            }
        }
        return Arrays.asList(sorted);
    }

    void updateFilter(Filter[] filters, int i, Filter filter) {
        if (filters[i] == null) {
            filters[i] = filter;
        }
        else {
            filters[i] = ff.and(filters[i], filter);
        }
    }
    
    PropertyName[] rewrite(int i, PropertyName n1, PropertyName n2, boolean prefix) {
        FeatureTypeInfo featureType = featureTypes.get(i);
        String alias = aliases != null ? aliases.get(i) : null;
        return rewrite(featureType, alias, n1, n2, prefix);
    }
    
    PropertyName[] rewrite(FeatureTypeInfo featureType, String alias, PropertyName n1, PropertyName n2, boolean prefix) {
        if (n1 != null) {
            PropertyName n = rewrite(featureType, alias, n1, prefix);
            if (n != null) {
                return new PropertyName[]{n, n2 != null ? rewrite(primaryFeatureType, primaryAlias, n2, prefix) : null};
            }    
        }

        if (n2 != null) {
            PropertyName n = rewrite(featureType, alias, n2, prefix);
            if (n != null) {
                return new PropertyName[]{n1 != null ? rewrite(primaryFeatureType, primaryAlias, n1, prefix) : null, n};
            }    
        }

        return null;
    }

    PropertyName rewrite(FeatureTypeInfo featureType, String alias, PropertyName name, boolean prefix) {
        String n = name.getPropertyName();
        if (n.startsWith(featureType.getPrefixedName()+"/")) {
            n = n.substring((featureType.getPrefixedName()+"/").length());
        }
        else if (n.startsWith(featureType.getName()+"/")) {
            n = n.substring((featureType.getName()+"/").length());
        }
        else if (alias != null && n.startsWith(alias+"/")) {
            n = n.substring((alias+"/").length());
        }
        else {
            n = null;
        }
        
        if (n != null) {
            if (prefix) {
                n = (alias != null ? alias : "") + "."+ n;
            }
            return ff.property(n, name.getNamespaceContext());
        }
        return null;
    }

    Filter rewrite(Filter f, PropertyName[] names, PropertyName[] rewritten) {
      //create a new filter with the rewritten property names
        Filter newFilter = null;
        if (names[0] != null) {
            newFilter = (Filter)f.accept(new PropertyNameRewriter(names[0],rewritten[0]), null);
        }
        if (names[1] != null) {
            newFilter = (Filter) (newFilter != null ? newFilter : f).accept(
                new PropertyNameRewriter(names[1],rewritten[1]), null);
        }
        return newFilter;
    }
    
    PropertyName[] names(Filter f) {
        //TODO: use a filter visitor
        Expression e1 = null;
        Expression e2 = null;
        if (f instanceof BinaryComparisonOperator) {
            e1 = ((BinaryComparisonOperator) f).getExpression1();
            e2 = ((BinaryComparisonOperator) f).getExpression2();
        }
        else if (f instanceof BinarySpatialOperator) {
            e1 = ((BinarySpatialOperator) f).getExpression1();
            e2 = ((BinarySpatialOperator) f).getExpression2();
        }
        else if (f instanceof BinaryTemporalOperator) {
            e1 = ((BinaryTemporalOperator) f).getExpression1();
            e2 = ((BinaryTemporalOperator) f).getExpression2();
        }
        else if (f instanceof PropertyIsNil){
            e1 = ((PropertyIsNil) f).getExpression();
        }
        else if (f instanceof PropertyIsNull) {
            e1 = ((PropertyIsNull) f).getExpression();
        }
        else if (f instanceof PropertyIsLike) {
            e1 = ((PropertyIsLike) f).getExpression();
        }
        else if (f instanceof PropertyIsBetween) {
            e1 = ((PropertyIsBetween) f).getExpression();
        }
        else {   
            throw new IllegalStateException();
        }
        
        return new PropertyName[]{e1 instanceof PropertyName ? (PropertyName) e1 : null, 
            e2 instanceof PropertyName ? (PropertyName) e2 : null};
    }

    class JoinFilterUnroller extends FilterVisitorSupport {

        List<Filter> unrolled = new ArrayList();
        
        public Object visitNullFilter(Object extraData) {
            return null;
        }

        public Object visit(ExcludeFilter filter, Object extraData) {
            return handle(filter, extraData);
        }

        public Object visit(IncludeFilter filter, Object extraData) {
            return handle(filter, extraData);
        }

        public Object visit(Id filter, Object extraData) {
            return handle(filter, extraData);
        }

        public Object visit(PropertyIsBetween filter, Object extraData) {
            return handle(filter, extraData);
        }

        public Object visit(PropertyIsLike filter, Object extraData) {
            return handle(filter, extraData);
        }

        public Object visit(PropertyIsNil filter, Object extraData) {
            return handle(filter, extraData);
        }

        public Object visit(PropertyIsNull filter, Object extraData) {
            return handle(filter, extraData);
        }

        public Object visit(Not filter, Object extraData) {
            return null;
        }

        @Override
        protected Object visit(BinaryLogicOperator op, Object extraData) {
            if (op instanceof And) {
                for (Filter f : op.getChildren()) {
                    f.accept(this, extraData);
                }
            }
            return null;
        }

        @Override
        protected Object visit(BinaryComparisonOperator op, Object extraData) {
            return handle(op, extraData);
        }

        @Override
        protected Object visit(BinarySpatialOperator op, Object extraData) {
            return handle(op, extraData);
        }

        @Override
        protected Object visit(BinaryTemporalOperator op, Object extraData) {
            return handle(op, extraData);
        }
        
        protected Object handle(Filter filter, Object extraData) {
            unrolled.add(filter);
            return extraData;
        }

        public List<Filter> getFilters() {
            return unrolled;
        }
    }
    
    class PropertyNameRewriter extends DuplicatingFilterVisitor {
        PropertyName from, to;
        
        PropertyNameRewriter(PropertyName from, PropertyName to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Object visit(PropertyName expression, Object extraData) {
            if (expression.equals(from)) {
                return to;
            }
            return super.visit(expression, extraData);
        }
    }
}
