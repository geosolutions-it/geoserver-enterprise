/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.Converters;


/**
 * Creates a request bean from a kvp set.
 * <p>
 * A request bean is an object which captures the parameters of an operation
 * being requested to a service.
 * </p>
 * <p>
 * This class is intended to be subclassed in cases when the creation
 * and initilization of a request bean cannot be created reflectivley.
 * </p>
 * <p>
 * The type of the request bean must be declared by the class.
 * See {@link #getRequestBean()}.
 * </p>
 * <p>
 * Instances need to be declared in a spring context like the following:
 * <pre>
 *         <code>
 *    &lt;bean id="myKvpRequestReader" class="org.geoserver.ows.KvpRequestReader"&gt;
 *      &lt;constructor-arg value="com.xyz.MyRequestBean"/&gt;
 *    &lt;/bean&gt;
 *         </code>
 * </pre>
 * Where <code>com.xyz.MyRequestBean</code> is a simple java bean such as:
 * <pre>
 *         <code>
 *   public class MyRequestBean {
 *
 *      public void setX( Object x ) {
 *        ...
 *      }
 *
 *      public Object getX() {
 *        ...
 *      }
 *   }
 *         </code>
 * </pre>
 * </p>
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class KvpRequestReader {
    /**
     * logging instance
     */
    protected static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.ows");

    /**
     * The class of the request bean
     */
    private Class requestBean;

    /**
     * A list of kvp names to filter.
     */
    protected Set<String> filter;
    
    /**
     * Creats the new kvp request reader.
     *
     * @param requestBean The type of the request read, not <code>null</code>
     */
    public KvpRequestReader(Class requestBean) {
        if (requestBean == null) {
            throw new NullPointerException();
        }

        this.requestBean = requestBean;
    }

    /**
     * @return The class of the request bean.
     */
    public final Class getRequestBean() {
        return requestBean;
    }

    /**
     * Sets a list of kvp's to filter by.
     * <p>
     * This value usually does not need to be set. The only case is when a kvp 
     * matches a property of a request object, but is not intended to be mapped
     * to that property.  
     * </p>
     * @param filter A list of names to filter, null to set no filter. 
     */
    public void setFilter(Set<String> filter) {
        this.filter = filter;
    }
    
    /**
     * A list of kvp's to filter.
     * <p>
     * See {@link #setFilter(Set)} for a better description of this property.
     * </p>
     * @return A list of kvp's to filter, or null for no filter.
     */
    public Set<String> getFilter() {
        return filter;
    }
    
    /**
     * Creats a new instance of the request object.
     * <p>
     * Subclasses may with to override this method. The default implementation
     * attempts to reflectivley create an instance of the request bean.
     * </p>
     * @return A new instance of the request.
     */
    public Object createRequest() throws Exception {
        return getRequestBean().newInstance();
    }

    /**
     * Reads the request from the set of kvp parameters.
     * <p>
     * Subclasses may wish to override this method. The default implementation
     * uses java bean reflection to populate the request bean with parameters
     * taken from the kvp map.
     * </p>
     * <p>
     * The "raw" (unparsed) kvp map is also made available.
     * </p>
     * <p>
     * This method may return a new instance of the request object, or the original
     * passed in.
     * </p>
     * @param request The request instance.
     * @param kvp The kvp set, map of String,Object.
     * @param rawKvp The raw kvp set (unparsed), map of String,String
     * 
     * @return A new request object, or the original
     */
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        for (Iterator e = kvp.entrySet().iterator(); e.hasNext();) {
            Map.Entry entry = (Map.Entry) e.next();
            String property = (String) entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }
            
            //check the filter
            if ( filter(property) ) {
                continue;
            }

            Class<? extends Object> targetClass = request.getClass();
            Class<? extends Object> valueClass = value.getClass();
            Method setter = OwsUtils.setter(targetClass, property, valueClass);

            if ( setter == null ) {
                //no setter matching the object of teh type, try to convert
                setter = OwsUtils.setter( request.getClass(), property, null );
                if ( setter != null ) {
                    //convert
                    Class target = setter.getParameterTypes()[0];
                    Object converted = Converters.convert( value, target );
                    if ( converted != null ) {
                        value = converted;
                    }
                    else {
                        setter = null;
                    }
                }
            }
            
            if (setter != null) {
                setter.invoke(request, new Object[] { value });
            }
        }

        return request;
    }

    /**
     * Determines if a kvp should be filtered based on {@link #getFilter()}.
     *
     * @param kvp The name of the kvp.
     * @return true if it sould be filtered and ignored, otherwise false.
     */
    protected boolean filter( String kvp ) {
        if (filter == null) {
            return false;
        }
        
        for ( String f : filter ) {
            if ( f.equalsIgnoreCase( kvp ) ) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Equals override, equality is based on {@link #getRequestBean()}
     */
    public final boolean equals(Object obj) {
        if (obj instanceof KvpRequestReader) {
            KvpRequestReader other = (KvpRequestReader) obj;

            return requestBean == other.requestBean;
        }

        return false;
    }

    public final int hashCode() {
        return requestBean.hashCode();
    }
}
