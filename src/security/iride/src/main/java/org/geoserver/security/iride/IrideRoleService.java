/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.security.iride;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.impl.GeoServerRole;

/**
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public class IrideRoleService extends AbstractGeoServerSecurityService implements GeoServerRoleService {
    /** emptySet */
    private static final TreeSet<GeoServerRole> emptySet = new TreeSet<GeoServerRole>();
    private static final String[] requestParams = new String[] {
        "CODICEFISCALE",
        "NOME",
        "COGNOME",
        "PROVIDER",
        "TIMESTAMP",
        "LIVELLOAUTH",
        "MAC"
    };
    private String serverURL;
    private String applicationName;
    private String adminRole;
    
    private HttpClient httpClient = new HttpClient();    
    private HttpConnectionManagerParams params = new HttpConnectionManagerParams();
    
    private static Pattern searchRuolo = Pattern.compile("<codiceRuolo[^>]*?>\\s*(.*?)\\s*<\\/codiceRuolo>", Pattern.CASE_INSENSITIVE);
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config)
            throws IOException {
        this.name=config.getName();
        
        if(config instanceof IrideSecurityServiceConfig) {
            IrideSecurityServiceConfig irideCfg = (IrideSecurityServiceConfig)config;
            serverURL = irideCfg.getServerURL();
            applicationName = irideCfg.getApplicationName();
            adminRole = irideCfg.getAdminRole();
            
            params.setSoTimeout(30000);
            params.setConnectionTimeout(30000);
            MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
            manager.setParams(params);
            httpClient.setHttpConnectionManager(manager);
        }
    }

    @Override
    public boolean canCreateStore() {
        return false;
    }

    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role)
            throws IOException {
        return null;
    }

    @Override
    public SortedSet<String> getUserNamesForRole(GeoServerRole role)
            throws IOException {
        return null;
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForUser(String username)
            throws IOException {
        TreeSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();
        String requestXml = getServiceRequestXml(username);
        String responseXml = callWebService(requestXml).replace("\\r", "").replace("\\n", "");
        Matcher m = searchRuolo.matcher(responseXml);
        while(m.find()) {
            roles.add(createRoleObject(m.group(1)));
        }
        return roles;
    }

    /**
     * @param requestXml
     * @return
     * @throws IOException 
     * @throws HttpException 
     */
    private String callWebService(String requestXml) throws HttpException, IOException {
        PostMethod post = new PostMethod(serverURL);
        Header header = new Header();
        header.setName("Content-type");
        header.setValue("text/xml; charset=UTF-8");
        post.setRequestHeader(header);
        header.setName("SOAPAction");
        header.setValue("dummy");
        post.setRequestHeader(header);
        post.setRequestEntity(new StringRequestEntity(requestXml, "text/xml", "UTF-8"));
        try {
            int status = httpClient.executeMethod(post);
            if (status == 200) {
                return post.getResponseBodyAsString();
            } else {
                throw new IOException("Error getting remote resources from " + serverURL
                        + ", http error " + status + ": " + post.getStatusText());
            }
        } finally {
            post.releaseConnection();
        }
        
    }

    /**
     * @param username
     * @return
     * @throws IOException 
     */
    private String getServiceRequestXml(String username) throws IOException {
        
        BufferedReader reader = null;
        StringBuilder result = new StringBuilder();
        try {
            reader= new BufferedReader(new InputStreamReader(this.getClass()
                    .getResourceAsStream("/findRuoliForPersonaInApplication.xml")));
            String line;
            while((line = reader.readLine()) != null) {
                result.append(replaceParamsInRequest(line, username));
            }
            return result.toString();
        } finally {
            if(reader != null) {
                reader.close();
            }
        }        
    }

    /**
     * @param line
     * @param usernameParts
     * @return
     */
    private String replaceParamsInRequest(String line, String username) {
        String[] usernameParts = username.split("\\/");
        int index = 0;
        for(String param : requestParams) {
            line = line.replace("%" + param + "%", usernameParts[index]);
            index++;
        }
        line = line.replace("%APPLICATION%", applicationName);
        line = line.replace("%FULLUSER%", username.substring(0, username.lastIndexOf("/")));
        return line;
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        return emptySet;
    }

    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return null;
    }

    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new GeoServerRole(role);
    }

    @Override
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return null;
    }

    @Override
    public GeoServerRole getRoleByName(String role) throws IOException {
        return null;
    }

    @Override
    public void load() throws IOException {
        
    }

    @Override
    public Properties personalizeRoleParams(String roleName,
            Properties roleParams, String userName, Properties userProps)
            throws IOException {
        return null;
    }

    @Override
    public GeoServerRole getAdminRole() {
        try {
            return createRoleObject(adminRole);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public GeoServerRole getGroupAdminRole() {
        return getAdminRole();
    }

    @Override
    public int getRoleCount() throws IOException {
        return 0;
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        return null;
    }

    @Override
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        
    }

    @Override
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        
    }

}
