package org.geoserver.wfs.request;

import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.emf.ecore.EObject;
import org.opengis.filter.Filter;

/**
 * Lock in a LockFeature request.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class Lock extends RequestObject {
    
    protected Lock(EObject adaptee) {
        super(adaptee);
    }

    public abstract QName getTypeName();

    public Filter getFilter() {
        return eGet(adaptee, "filter", Filter.class);
    }

    public void setFilter(Filter filter) {
        eSet(adaptee, "filter", filter);
    }

    public static class WFS11 extends Lock {
        
        public WFS11(EObject adaptee) {
            super(adaptee);
        }

        @Override
        public QName getTypeName() {
            return eGet(adaptee, "typeName", QName.class);
        }
    }
    
    public static class WFS20 extends Lock {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }
     
        @Override
        public QName getTypeName() {
            List typeNames = eGet(adaptee, "typeNames", List.class);
            if (typeNames.size() == 1) {
                return (QName) typeNames.get(0);
            }
            throw new IllegalArgumentException("Multiple type names on single lock not supported");
        }
        
        @Override
        public void setTypeName(QName typeName) {
            List typeNames = eGet(adaptee, "typeNames", List.class);
            typeNames.clear();
            typeNames.add(typeName);
        }
    }

}
