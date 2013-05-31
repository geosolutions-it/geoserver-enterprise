package org.geoserver.wfs.request;

import java.util.List;

import net.opengis.wfs.IdentifierGenerationOptionType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs20.InsertType;

import org.eclipse.emf.ecore.EObject;

/**
 * Insert element in a Transaction request.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class Insert extends TransactionElement {

    protected Insert(EObject adaptee) {
        super(adaptee);
    }
    
    public abstract List getFeatures();

    public boolean isIdGenUseExisting() {
        return false;
    }

    public static class WFS11 extends Insert {

        public WFS11(EObject adaptee) {
            super(adaptee);
        }
        
        @Override
        public List getFeatures() {
            return eGet(adaptee, "feature", List.class);
        }

        @Override
        public boolean isIdGenUseExisting() {
            return ((InsertElementType)adaptee).getIdgen() 
                    == IdentifierGenerationOptionType.USE_EXISTING_LITERAL;
        }

        public static InsertElementType unadapt(Insert insert) {
            if (insert instanceof WFS11) {
                return (InsertElementType) insert.getAdaptee();
            }

            InsertElementType ie = WfsFactory.eINSTANCE.createInsertElementType();
            ie.setHandle(insert.getHandle());
            ie.getFeature().addAll(insert.getFeatures());

            return ie;
        }
    }
    
    public static class WFS20 extends Insert {

        public WFS20(EObject adaptee) {
            super(adaptee);
        }
        
        @Override
        public List getFeatures() {
            return eGet(adaptee, "any", List.class);
        }

    }

}
