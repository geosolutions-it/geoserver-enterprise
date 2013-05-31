package org.geoserver.ows.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides lookup information about java bean properties in a class.
 * 
 * @author Justin Deoliveira, OpenGEO
 * @author Andrea Aime, OpenGEO
 *
 */
public class ClassProperties {
    private static final List<Method> EMPTY = new ArrayList<Method>(0);
    List<Method> methods;
    List<Method> getters;
    List<Method> setters;
    
    public ClassProperties(Class clazz) {
        methods = Arrays.asList(clazz.getMethods());
        getters = new ArrayList<Method>();
        setters = new ArrayList<Method>();
        for (Method method : methods) {
            final String name = method.getName();
            final Class<?>[] params = method.getParameterTypes();
            if((name.startsWith("get") || name.startsWith("is")) && params.length == 0) {
                getters.add(method);
            } else if(name.startsWith("set") && params.length == 1) {
                setters.add(method);
            }
        }

        // avoid keeping lots of useless empty arrays in memory for 
        // the long term, use just one
        if(methods.size() == 0)
            methods = EMPTY;
        if(getters.size() == 0)
            getters = EMPTY;
        if(setters.size() == 0)
            setters = EMPTY;
    }

    /**
     * Returns a list of all the properties of the class.
     * 
     * @return A list of string.
     */
    public List<String> properties() {
        //TODO: factor out check if method is a getter
        ArrayList<String> properties = new ArrayList<String>();
        for ( Method g : getters ) {
            properties.add( gp( g ) );
        }
        return properties;
    }

    /**
     * Looks up a setter method by property name.
     * <p>
     * setter("foo",Integer) -> void setFoo(Integer); 
     * </p>
     * @param property The property.
     * @param type The type of the property.
     * 
     * @return The setter for the property, or null if it does not exist.
     */
    public Method setter(String property, Class type) {
        for (Method setter : setters) {
            if(setter.getName().substring(3).equalsIgnoreCase(property)) {
                if(type == null) {
                    return setter;
                } else {
                    Class target = setter.getParameterTypes()[0];
                    if(target.isAssignableFrom(type) || 
                            (target.isPrimitive() && type == wrapper(target)) ||
                            (type.isPrimitive() && target == wrapper(type))) {
                        return setter;
                    }
                }
            }
        }
        
        // could not be found, try again with a more lax match
        String lax = lax(property);
        if (!lax.equals(property)) {
            return setter(lax, type);
        }
        
        return null;
    }
    
    /**
     * Looks up a getter method by its property name.
     * <p>
     * getter("foo",Integer) -> Integer getFoo(); 
     * </p>
     * @param property The property.
     * @param type The type of the property.
     * 
     * @return The getter for the property, or null if it does not exist.
     */
    public Method getter(String property, Class type) {
        for (Method getter : getters) {
            if(gp(getter).equalsIgnoreCase(property)) {
                if(type == null) {
                    return getter;
                } else {
                    Class target = getter.getReturnType();
                    if(type.isAssignableFrom(target) || 
                            (target.isPrimitive() && type == wrapper(target)) ||
                            (type.isPrimitive() && target == wrapper(type))) {
                        return getter;
                    }
                }
            }
        }
        
        // could not be found, try again with a more lax match
        String lax = lax(property);
        if (!lax.equals(property)) {
            return getter(lax, type);
        }
        
        return null;
    }
    
    /**
     * Does some checks on the property name to turn it into a java bean property.
     * <p>
     * Checks include collapsing any "_" characters.
     * </p>
     */
    static String lax(String property) {
        return property.replaceAll("_", "");
    }
    
    /**
    * Returns the wrapper class for a primitive class.
    * 
    * @param primitive A primtive class, like int.class, double.class, etc...
    */
   static Class wrapper( Class primitive ) {
       if ( boolean.class == primitive ) {
           return Boolean.class;
       }
       if ( char.class == primitive ) {
           return Character.class;
       }
       if ( byte.class == primitive ) {
           return Byte.class;
       }
       if ( short.class == primitive ) {
           return Short.class;
       }
       if ( int.class == primitive ) {
           return Integer.class;
       }
       if ( long.class == primitive ) {
           return Long.class;
       }
       
       if ( float.class == primitive ) {
           return Float.class;
       }
       if ( double.class == primitive ) {
           return Double.class;
       }
       
       return null;
   }

   /**
    * Looks up a method by name.
    */
    public Method method(String name) {
        for(Method method : methods) {
            if(method.getName().equalsIgnoreCase(name))
                return method;
        }
        return null;
    }

    /**
     * Returns the name of the property corresponding to the getter method.
     */
    String gp( Method getter ) {
        return getter.getName().substring( getter.getName().startsWith("get") ? 3 : 2 );
    }
}
