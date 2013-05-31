package org.geoserver.wfs;

import java.io.Serializable;

import org.geotools.gml2.SrsSyntax;

/**
 * Configuration for gml encoding.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public interface GMLInfo extends Serializable {

    /**
     * Enumeration for srsName style.
     * <p>
     * <ul>
     *   <li>{@link #NORMAL} : EPSG:XXXX
     *   <li>{@link #XML} : http://www.opengis.net/gml/srs/epsg.xml#XXXX
     *   <li>{@link #URN} : urn:x-ogc:def:crs:EPSG:XXXX
     * </ul>
     * <p>
     *
     * @deprecated use {@link SrsSyntax}
     */
    public static enum SrsNameStyle {
        NORMAL {
            @Override
            public SrsSyntax toSrsSyntax() {
                return SrsSyntax.EPSG_CODE;
            }
        },
        XML {
            @Override
            public SrsSyntax toSrsSyntax() {
                return SrsSyntax.OGC_HTTP_URL;
            }
        },
        URN {
            @Override
            public SrsSyntax toSrsSyntax() {
                return SrsSyntax.OGC_URN_EXPERIMENTAL;
            }  
        },
        URN2 {
            @Override
            public SrsSyntax toSrsSyntax() {
                return SrsSyntax.OGC_URN;
            }
        }, 
        URL {
            @Override
            public SrsSyntax toSrsSyntax() {
                return SrsSyntax.OGC_HTTP_URI;
            }
        };
        
        public String getPrefix() {
            return toSrsSyntax().getPrefix();
        }

        public abstract SrsSyntax toSrsSyntax();

        public SrsNameStyle fromSrsSyntax(SrsSyntax srsSyntax) {
            for (SrsNameStyle s : values()) {
                if (s.toSrsSyntax() == srsSyntax) {
                    return s;
                }
            }
            return null;
        }
    }
    
    /**
     * The srs name style to be used when encoding the gml 'srsName' attribute.
     */
    SrsNameStyle getSrsNameStyle();
    
    /**
     * Sets the srs name style to be used when encoding the gml 'srsName' attribute.
     */
    void setSrsNameStyle( SrsNameStyle srsNameStyle );
    
    /**
     * Controls how attributes are handled with regard to attributes defined in the schema of
     * AbstractFeatureType, name, description, etc... 
     * <p>
     * When set this flag will cause the attributes to be redefined in the application schema 
     * namespace.
     * </p>
     */
    Boolean getOverrideGMLAttributes();
    
    /**
     * Sets the flag that controls how attributes are handled with regard to attributes defined in 
     * the schema of AbstractFeatureType.
     * 
     * @see {@link #getOverrideGMLAttributes()}
     */
    void setOverrideGMLAttributes(Boolean overrideGMLAttributes);
}
