/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.core.userdetails.memory.UserAttributeEditor;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.PropertyFileWatcher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.vfny.geoserver.global.GeoserverDataDirectory;

/**
 * A simple DAO reading/writing the user's property files
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class GeoServerUserDao implements UserDetailsService {
    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security");

    TreeMap<String, User> userMap;

    PropertyFileWatcher userDefinitionsFile;
    
    File securityDir;
    
    /**
     * Returns the {@link GeoServerUserDao} instance registered in the GeoServer Spring context
     */
    public static GeoServerUserDao get() {
        return GeoServerExtensions.bean(GeoServerUserDao.class);
    }
    
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException,
            DataAccessException {
        checkUserMap();

        UserDetails user = userMap.get(username);
        if (user == null)
            throw new UsernameNotFoundException("Could not find user: " + username);

        return user;
    }

    /**
     * Either loads the default property file on the first access, or reloads it if it has been
     * modified since last access.
     * 
     * @throws DataAccessResourceFailureException
     */
    void checkUserMap() throws DataAccessResourceFailureException {
        InputStream is = null;
        OutputStream os = null;
        if ((userMap == null) || userDefinitionsFile == null || userDefinitionsFile.isStale()) {
            try {
                if (userDefinitionsFile == null) {
                    securityDir = GeoserverDataDirectory.findCreateConfigDir("security");
                    File propFile = new File(securityDir, "users.properties");

                    if (!propFile.exists()) {
                        // we're probably dealing with an old data dir, create
                        // the file without
                        // changing the username and password if possible
                        Properties p = new Properties();
                        GeoServerInfo global = GeoServerExtensions.bean(GeoServer.class).getGlobal();
                        if ((global != null) && (global.getAdminUsername() != null)
                                && !global.getAdminUsername().trim().equals("")) {
                            p.put(global.getAdminUsername(), global.getAdminPassword()
                                    + ",ROLE_ADMINISTRATOR");
                        } else {
                            p.put("admin", "geoserver,ROLE_ADMINISTRATOR");
                        }

                        os = new FileOutputStream(propFile);
                        p.store(os, "Format: name=password,ROLE1,...,ROLEN");
                        os.close();

                        // setup a sample service.properties
                        File serviceFile = new File(securityDir, "service.properties");
                        os = new FileOutputStream(serviceFile);
                        is = GeoServerUserDao.class
                                .getResourceAsStream("serviceTemplate.properties");
                        byte[] buffer = new byte[1024];
                        int count = 0;
                        while ((count = is.read(buffer)) > 0) {
                            os.write(buffer, 0, count);
                        }
                    }

                    userDefinitionsFile = new PropertyFileWatcher(propFile);
                }

                userMap = loadUsersFromProperties(userDefinitionsFile.getProperties());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "An error occurred loading user definitions", e);
            } finally {
                if (is != null)
                    try {
                        is.close();
                    } catch (IOException ei) { /* nothing to do */
                    }
                if (os != null)
                    try {
                        os.close();
                    } catch (IOException eo) { /* nothing to do */
                    }
            }
        }
    }

    /**
     * Get the list of roles currently known by users (there's guarantee the well known
     * ROLE_ADMINISTRATOR will be part of the lot)
     */
    public List<String> getRoles() {
        checkUserMap();
        
        Set<String> roles = new TreeSet<String>();
        roles.add("ROLE_ADMINISTRATOR");
        for (User user : getUsers()) {
            for (GrantedAuthority ga : user.getAuthorities()) {
                roles.add(ga.getAuthority());
            }
        }
        return new ArrayList<String>(roles);
    }

    /**
     * Returns the list of users. To be used for UI editing of users, it's a live map
     * 
     * @return
     */
    public List<User> getUsers() {
        checkUserMap();
        
        return new ArrayList<User>(userMap.values());
    }
    
    /**
     * Adds a user in the user map
     * @param user
     */
    public void putUser(User user) {
        checkUserMap();
        
        if(userMap.containsKey(user.getUsername()))
            throw new IllegalArgumentException("The user " + user.getUsername() + " already exists");
        else
            userMap.put(user.getUsername(), user);
    }
    
    /**
     * Updates a user in the user map
     * @param user
     */
    public void setUser(User user) {
        checkUserMap();
        
        if(userMap.containsKey(user.getUsername()))
            userMap.put(user.getUsername(), user);
        else
            throw new IllegalArgumentException("The user " + user.getUsername() + " already exists");
    }
    
    /**
     * Removes the specified user from the users list
     * @param username
     * @return
     */
    public boolean removeUser(String username) {
        checkUserMap();
        
        return userMap.remove(username) != null;
    }

    /**
     * Writes down the current users map to file system
     */
    public void storeUsers() throws IOException {
        FileOutputStream os = null;
        try {
            // turn back the users into a users map
            Properties p = storeUsersToProperties(userMap);

            // write out to the data dir
            File propFile = new File(securityDir, "users.properties");
            os = new FileOutputStream(propFile);
            p.store(os, null);
        } catch (Exception e) {
            if (e instanceof IOException)
                throw (IOException) e;
            else
                throw (IOException) new IOException(
                        "Could not write updated users list to file system").initCause(e);
        } finally {
            if (os != null)
                os.close();
        }
    }

    /**
     * Force the dao to reload its definitions from the file
     */
    public void reload() {
        userDefinitionsFile = null;
    }

    /**
     * Loads the user from property file into the users map
     * 
     * @param users
     * @param props
     */
    TreeMap<String, User> loadUsersFromProperties(Properties props) {
        TreeMap<String, User> users = new TreeMap<String, User>();
        UserAttributeEditor configAttribEd = new UserAttributeEditor();

        for (Iterator<Object> iter = props.keySet().iterator(); iter.hasNext();) {
            // the attribute editors parses the list of strings into password, username and enabled
            // flag
            String username = (String) iter.next();
            configAttribEd.setAsText(props.getProperty(username));

            // if the parsing succeeded turn that into a user object
            UserAttribute attr = (UserAttribute) configAttribEd.getValue();
            if (attr != null) {
                User user = createUserObject(username, attr.getPassword(), attr.isEnabled(), attr.getAuthorities());
                users.put(username, user);
            }
        }

        return users;
    }

    protected User createUserObject(String username,String password, boolean isEnabled,List<GrantedAuthority> authorities) {
       return new User(username, password, isEnabled, true, true,
                true, authorities);
    }
    
    
    /**
     * Stores the provided user map into a properties object
     * 
     * @param userMap
     * @return
     */
    Properties storeUsersToProperties(Map<String, User> userMap) {
        Properties p = new Properties();
        for (User user : userMap.values()) {
            p.setProperty(user.getUsername(), serializeUser(user));
        }
        return p;
    }

    /**
     * Turns the users password, granted authorities and enabled state into a property file value
     * 
     * @param user
     * @return
     */
    String serializeUser(User user) {
        StringBuffer sb = new StringBuffer();
        sb.append(user.getPassword());
        sb.append(",");
        for (GrantedAuthority ga : user.getAuthorities()) {
            sb.append(ga.getAuthority());
            sb.append(",");
        }
        sb.append(user.isEnabled() ? "enabled" : "disabled");
        return sb.toString();
    }

}
