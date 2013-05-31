/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The security filter filter chain.
 * <p>
 * The content of {@link #antPatterns} must be equal to the keys of {@link #filterMap}.
 * </p>
 * <p>
 * The order of {@link #antPatterns} determines the order of ant pattern matching used by 
 * GeoServerSecurityFilterChainProxy.
 * </p>
 * @author christian
 *
 */
public class GeoServerSecurityFilterChain implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    List<RequestFilterChain> requestChains = new ArrayList();

    /*
     * chain patterns 
     */
    public static final String WEB_CHAIN = "/web/**";
    public static final String FORM_LOGIN_CHAIN = "/j_spring_security_check,/j_spring_security_check/"; 
    public static final String FORM_LOGOUT_CHAIN = "/j_spring_security_logout,/j_spring_security_logout/";
    public static final String REST_CHAIN = "/rest/**";
    public static final String GWC_WEB_CHAIN = "/gwc/rest/web/**";
    public static final String GWC_REST_CHAIN = "/gwc/rest/**"; 
    public static final String DEFAULT_CHAIN = "/**"; 
    
    /*
     * filter names
     */
    public static final String SECURITY_CONTEXT_ASC_FILTER = "contextAsc";
    public static final String SECURITY_CONTEXT_NO_ASC_FILTER = "contextNoAsc";
    
    public static final String FORM_LOGIN_FILTER = "form";
    public static final String FORM_LOGOUT_FILTER = "formLogout";

    public static final String REMEMBER_ME_FILTER = "rememberme";

    public static final String ANONYMOUS_FILTER = "anonymous";

    public static final String BASIC_AUTH_FILTER = "basic";
    //public static final String BASIC_AUTH_NO_REMEMBER_ME_FILTER = "basicAuthNrm";

    public static final String DYNAMIC_EXCEPTION_TRANSLATION_FILTER = "exception";
    public static final String GUI_EXCEPTION_TRANSLATION_FILTER = "guiException";

    public static final String FILTER_SECURITY_INTERCEPTOR = "interceptor";
    public static final String FILTER_SECURITY_REST_INTERCEPTOR = "restInterceptor";

    static RequestFilterChain WEB = new RequestFilterChain(WEB_CHAIN, GWC_WEB_CHAIN);
    static {
        WEB.setName("web");
        WEB.setFilterNames(SECURITY_CONTEXT_ASC_FILTER, REMEMBER_ME_FILTER, ANONYMOUS_FILTER,
            GUI_EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_INTERCEPTOR);
    }

    private static RequestFilterChain WEB_LOGIN = new RequestFilterChain(FORM_LOGIN_CHAIN);
    static {
        WEB_LOGIN.setName("webLogin");
        WEB_LOGIN.setFilterNames(SECURITY_CONTEXT_ASC_FILTER, FORM_LOGIN_FILTER);
    }

    private static RequestFilterChain WEB_LOGOUT = new RequestFilterChain(FORM_LOGOUT_CHAIN);
    static {
        WEB_LOGOUT.setName("webLogout");
        WEB_LOGOUT.setFilterNames(SECURITY_CONTEXT_ASC_FILTER, FORM_LOGOUT_FILTER);
    }

    private static RequestFilterChain REST = new RequestFilterChain(REST_CHAIN);
    static {
        REST.setName("rest");
        REST.setFilterNames(SECURITY_CONTEXT_NO_ASC_FILTER, BASIC_AUTH_FILTER, ANONYMOUS_FILTER, 
            DYNAMIC_EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_REST_INTERCEPTOR);
    }

    private static RequestFilterChain GWC = new RequestFilterChain(GWC_REST_CHAIN);
    static {
        GWC.setName("gwc");
        GWC.setFilterNames(SECURITY_CONTEXT_NO_ASC_FILTER, BASIC_AUTH_FILTER, 
            DYNAMIC_EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_REST_INTERCEPTOR);
    }

    private static RequestFilterChain DEFAULT = new RequestFilterChain(DEFAULT_CHAIN);
    static {
        DEFAULT.setName("default");
        DEFAULT.setFilterNames(SECURITY_CONTEXT_NO_ASC_FILTER, BASIC_AUTH_FILTER, ANONYMOUS_FILTER, 
            DYNAMIC_EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_INTERCEPTOR);
    }

    private static List<RequestFilterChain> INITIAL = new ArrayList<RequestFilterChain>();
    static {
        INITIAL.add(WEB);
        INITIAL.add(WEB_LOGIN);
        INITIAL.add(WEB_LOGOUT);
        INITIAL.add(REST);
        INITIAL.add(GWC);
        INITIAL.add(DEFAULT);
    }

    public GeoServerSecurityFilterChain() {
        requestChains = new ArrayList();
    }

    /**
     * Constructor cloning all collections
     */
    public GeoServerSecurityFilterChain(List<RequestFilterChain> requestChains) {
        this.requestChains = requestChains;
    }

    /**
     * Constructor cloning all collections
     */
    public GeoServerSecurityFilterChain(GeoServerSecurityFilterChain other) {
        this.requestChains = new ArrayList(other.getRequestChains());
    }

    /**
     * Create the initial {@link GeoServerSecurityFilterChain} 
     * 
     * @return
     */
    public static GeoServerSecurityFilterChain createInitialChain() {
        return new GeoServerSecurityFilterChain(new ArrayList(INITIAL));
    }

    public void postConfigure(GeoServerSecurityManager secMgr) {
        // TODO, Justin
        // Not sure if this is correct, if it is, you can add the constant chain
        // for the root user login
        for(GeoServerSecurityProvider p : secMgr.lookupSecurityProviders()) {
            p.configureFilterChain(this);
        }
    }

    public static RequestFilterChain lookupRequestChainByName(
        String name, GeoServerSecurityManager secMgr) {
        //this is kind of a hack but we create an initial filter chain and run it through the 
        // security provider extension points to get an actual final chain, and then look through
        // the elements for a matching name
        GeoServerSecurityFilterChain filterChain = createInitialChain();
        filterChain.postConfigure(secMgr);

        for (RequestFilterChain requestChain : filterChain.getRequestChains()) {
            if (requestChain.getName().equals(name)) {
                return requestChain;
            }
        }

        return null;
    }

    public static RequestFilterChain lookupRequestChainByPattern(
        String pattern, GeoServerSecurityManager secMgr) {
        //this is kind of a hack but we create an initial filter chain and run it through the 
        // security provider extension points to get an actual final chain, and then look through
        // the elements for a matching name
        GeoServerSecurityFilterChain filterChain = createInitialChain();
        filterChain.postConfigure(secMgr);

        for (RequestFilterChain requestChain : filterChain.getRequestChains()) {
            if (requestChain.getPatterns().contains(pattern)) {
                return requestChain;
            }
        }

        return null;
    }

    public List<RequestFilterChain> getRequestChains() {
        return requestChains;
    }

    public RequestFilterChain getRequestChainByName(String name) {
        for (RequestFilterChain requestChain : requestChains) {
            if (requestChain.getName().equals(name)) {
                return requestChain;
            }
        }
        return null;
    }

    public Map<String,List<String>> compileFilterMap() {
        Map<String,List<String>> filterMap = new LinkedHashMap();
        
        for (RequestFilterChain ch : requestChains) {
            //patterns.addAll(ch.getPatterns());
            
            for (String p : ch.getPatterns()) {
                filterMap.put(p, ch.getFilterNames());
            }
        }

        return filterMap;
    }

    public void simplify() {
        int j = 0;
        for (Iterator<RequestFilterChain> it = requestChains.iterator(); it.hasNext(); j++) {
            RequestFilterChain requestChain = it.next();
            RequestFilterChain toMerge = null;

            //look at any previous chain to see if we can merge
            for (int i = 0; i < j; i++) {
                RequestFilterChain requestChain2 = requestChains.get(i);
                if (requestChain2 == requestChain) {
                    continue;
                }
                if (requestChain2.getFilterNames().equals(requestChain.getFilterNames())) {
                    toMerge = requestChain2;
                    break;
                }
            }

            if (toMerge != null) {
                toMerge.getPatterns().addAll(requestChain.getPatterns());
                it.remove();
                j--;
            }
        }
    
    }

    public void decompileFilterMap(Map<String,List<String>> filterMap) {
        List<RequestFilterChain> requestChains = new ArrayList();
        for (String pattern : filterMap.keySet()) {
            List<String> filterNames = filterMap.get(pattern);
            RequestFilterChain requestChain = null;
            for (RequestFilterChain chain : requestChains) {
                if (chain.getFilterNames().equals(filterNames)) {
                    requestChain = chain;
                    break;
                }
            }
            if (requestChain == null) {
                //new chain
                requestChain = new RequestFilterChain(pattern);
                requestChain.setFilterNames(filterNames);
            }
            else {
                //merge with existing chain
                requestChain.getPatterns().add(pattern);
            }
            requestChains.add(requestChain);
        }
        this.requestChains = requestChains;
    }

    /**
     * Inserts a filter as the first of the filter list corresponding to the specified pattern.
     *
     * @return True if the filter was inserted.
     */
    public boolean insertFirst(String pattern, String filterName) {
        RequestFilterChain requestChain = findAndCheck(pattern, filterName); 
        if (requestChain == null) {
            return false;
        }
        requestChain.getFilterNames().add(0, filterName);
        return false;
    }

    /**
     * Inserts a filter as the last of the filter list corresponding to the specified pattern.
     *
     * @return True if the filter was inserted.
     */
    public boolean insertLast(String pattern, String filterName) {
        RequestFilterChain requestChain = findAndCheck(pattern, filterName); 
        if (requestChain == null) {
            return false;
        }

        return requestChain.getFilterNames().add(filterName);
    }

    /**
     * Inserts a filter as before another in the list corresponding to the specified pattern.
     *
     * @return True if the filter was inserted.
     */
    public boolean insertBefore(String pattern, String filterName, String positionName) {
        RequestFilterChain requestChain = findAndCheck(pattern, filterName); 
        if (requestChain == null) {
            return false;
        }

        List<String> filterNames = requestChain.getFilterNames();
        int index = filterNames.indexOf(positionName);
        if (index == -1) {
            return false;
        }

        filterNames.add(index, filterName);
        return true;
    }

    
    /**
     * Inserts a filter as after another in the list corresponding to the specified pattern.
     *
     * @return True if the filter was inserted.
     */
    public boolean insertAfter(String pattern, String filterName, String positionName) {
        RequestFilterChain requestChain = findAndCheck(pattern, filterName); 
        if (requestChain == null) {
            return false;
        }

        List<String> filterNames = requestChain.getFilterNames();
        int index = filterNames.indexOf(positionName);
        if (index == -1) {
            return false;
        }

        filterNames.add(index+1,filterName);
        return true;
    }

    public RequestFilterChain find(String pattern) {
        return requestChain(pattern);
    }

    /**
     * Get a list of patterns having the filter in their chain.
     */
    public List<String> patternsForFilter(String filterName) {
        List<String> result = new ArrayList<String>();
        for (RequestFilterChain requestChain : requestChains) {
            if (requestChain.getFilterNames().contains(filterName)) {
                result.addAll(requestChain.getPatterns());
            }
        }
        return result;
    }

    /**
     * Get the filters for the specified pattern.
     */
    public List<String> filtersFor(String pattern) {
        RequestFilterChain requestChain = requestChain(pattern);
        if (requestChain == null) {
            return Collections.EMPTY_LIST;
        }

        return new ArrayList(requestChain.getFilterNames());
    }

    public boolean removeForPattern(String pattern) {
        RequestFilterChain requestChain = requestChain(pattern);
        if (requestChain != null) {
            return requestChains.remove(requestChain);
        }
        return false;
    }

    RequestFilterChain findAndCheck(String pattern, String filterName) {
        RequestFilterChain requestChain = requestChain(pattern);
        if (requestChain == null) {
            return null;
        }

        if (requestChain.getFilterNames().contains(filterName)) {
            //JD: perhaps we should move it
            return null;
        }

        return requestChain;
    }

    RequestFilterChain requestChain(String pattern) {
        for (RequestFilterChain requestChain : requestChains) {
            if (requestChain.getPatterns().contains(pattern)) {
                return requestChain;
            }
        }
        return null;
    }
}
