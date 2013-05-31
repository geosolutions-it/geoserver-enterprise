/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */


package org.geoserver.security.web;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.config.impl.MemoryRoleServiceConfigImpl;
import org.geoserver.security.config.impl.MemoryUserGroupServiceConfigImpl;
import org.geoserver.security.impl.AbstractRoleServiceTest;
import org.geoserver.security.impl.AbstractUserGroupServiceTest;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.MemoryRoleStore;
import org.geoserver.security.impl.MemoryUserGroupStore;
import org.geoserver.security.impl.ReadOnlyRoleService;
import org.geoserver.security.impl.ReadOnlyUGService;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.xml.XMLRoleServiceTest;
import org.geoserver.security.xml.XMLUserGroupServiceTest;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;

public abstract class AbstractSecurityWicketTestSupport extends GeoServerWicketTestSupport {
    
    
    public String getRoleServiceName() {
        return "test";
    }
    public String getUserGroupServiceName() {
        return "test";
    }

    
    protected AbstractRoleServiceTest gaTest;
    protected AbstractUserGroupServiceTest ugTest;
    protected GeoServerUserGroupService ugService;
    protected GeoServerRoleService gaService;
    protected GeoServerRoleStore gaStore;
    protected GeoServerUserGroupStore ugStore;

    @Override
    protected String[] getSpringContextLocations() {
        List<String> list = new ArrayList(Arrays.asList(super.getSpringContextLocations()));
        list.add("classpath*:/applicationTestContext.xml");
        return list.toArray(new String[list.size()]);
    }

    @Override
    protected void setUpInternal() throws Exception {
        login();        
        Locale.setDefault(Locale.ENGLISH);
        // run all tests with url param encryption
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.setEncryptingUrlParams(true);
        getSecurityManager().saveSecurityConfig(config);
    }

    protected void initialize(AbstractUserGroupServiceTest ugTest, AbstractRoleServiceTest gaTest) 
        throws Exception {

        this.ugTest = ugTest;
        this.gaTest = gaTest;

        gaService=gaTest.createRoleService(getRoleServiceName());
        getSecurityManager().setActiveRoleService(gaService);
        ugService=ugTest.createUserGroupService(getUserGroupServiceName());
        
        gaStore =  gaTest.createStore(gaService);
        ugStore =  ugTest.createStore(ugService);
        initializeServiceRules();
        initializeDataAccessRules();
    }

    protected void initializeForXML() throws Exception {
        initialize(new XMLUserGroupServiceTest() ,new XMLRoleServiceTest());
    }

    protected void addAdditonalData() throws Exception {
        gaStore.associateRoleToGroup(
                gaStore.getRoleByName("ROLE_WMS"), "group1");
        gaStore.associateRoleToGroup(
                gaStore.getRoleByName("ROLE_WFS"), "group1");
        gaStore.store();                    
    }
    protected void insertValues() throws Exception {
        gaTest.insertValues(gaStore);
        gaStore.store();
        ugTest.insertValues(ugStore);
        ugStore.store();
    }
    
    protected void modifyValues() throws Exception{
        gaTest.modifyValues(gaStore);
        gaStore.store();
        ugTest.modifyValues(ugStore);
        ugStore.store();
    }

    protected void removeValues() throws IOException{
        gaTest.removeValues(gaStore);
        gaStore.store();
        ugTest.removeValues(ugStore);
        ugStore.store();
    }
    
    public String getRORoleServiceName() {
        return "ReadOnlyRoleService";
    }
    public String getROUserGroupServiceName() {
        return "ReadOnlyUGService";
    }

    
                
    protected void activateRORoleService() throws Exception{
        MemoryRoleServiceConfigImpl config = new MemoryRoleServiceConfigImpl();
        config.setName(getRORoleServiceName());
        config.setClassName(ReadOnlyRoleService.class.getName());
        getSecurityManager().saveRoleService(config);
        gaService = getSecurityManager().loadRoleService(getRORoleServiceName());
        gaService.initializeFromConfig(config);
        gaService.setSecurityManager(GeoServerApplication.get().getSecurityManager());
        gaStore = new MemoryRoleStore();
        gaStore.initializeFromService(gaService);
        gaTest.insertValues(gaStore);
        gaStore.store();
        
        getSecurityManager().setActiveRoleService(gaService);        
        gaStore=null;
    }

    protected GeoServerDigestPasswordEncoder getDigestPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
    }

    protected GeoServerPBEPasswordEncoder getPBEPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, false);
    }

    protected GeoServerPlainTextPasswordEncoder getPlainTextPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class);
    }
    protected void activateROUGService() throws Exception{
        MemoryUserGroupServiceConfigImpl config = new MemoryUserGroupServiceConfigImpl();         
        config.setName(getROUserGroupServiceName());        
        config.setClassName(ReadOnlyUGService.class.getName());
        config.setPasswordEncoderName(getDigestPasswordEncoder().getName());
        config.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
        getSecurityManager().saveUserGroupService(config);
        ugService = getSecurityManager().loadUserGroupService(getROUserGroupServiceName());
        ugService.initializeFromConfig(config);
        ugService.setSecurityManager(GeoServerApplication.get().getSecurityManager());
        ugStore = new MemoryUserGroupStore(); 
        ugStore.initializeFromService(ugService);
        ugTest.insertValues(ugStore);
        ugStore.store();
                
        ugStore=null;
    }
    
    public void executeModalWindowClosedCallback(ModalWindow modalWindow) {
        for (IBehavior behavior : modalWindow.getBehaviors()) {
          if (behavior instanceof AbstractDefaultAjaxBehavior) {
            String name = behavior.getClass().getSimpleName();
            if (name.startsWith("WindowClosedBehavior")) {
              tester.executeBehavior((AbstractAjaxBehavior) behavior);
            }
          }
        }
      }

      public void executeModalWindowCloseButtonCallback(ModalWindow modalWindow) {
        for (IBehavior behavior : modalWindow.getBehaviors()) {
          if (behavior instanceof AbstractDefaultAjaxBehavior) {
            String name = behavior.getClass().getSimpleName();
            
            if (name.startsWith("CloseButtonBehavior")) {
              tester.executeBehavior((AbstractAjaxBehavior) behavior);
            }
          }
        }
      } 

      protected void initializeServiceRules() throws IOException {
          ServiceAccessRuleDAO dao = ServiceAccessRuleDAO.get();
          dao.getRules();
          dao.addRule(new ServiceAccessRule("wms", "GetMap", "ROLE_AUTHENTICATED"));                    
          dao.addRule(new ServiceAccessRule("wms", "*", "ROLE_WMS"));
          dao.addRule(new ServiceAccessRule("wfs", "GetFeature", "ROLE_AUTHENTICATED"));                    
          dao.addRule(new ServiceAccessRule("wfs", "*", "ROLE_WFS"));
          dao.addRule(new ServiceAccessRule("*", "*", GeoServerRole.ADMIN_ROLE.getAuthority()));
          dao.storeRules();
      }
      
      protected void initializeDataAccessRules() throws IOException{
/*
 *      List of namespace:layer in test data          
          cdf:Other
          cite:NamedPlaces
          sf:PrimitiveGeoFeature
          cgf:MPoints
          cgf:Points
          cgf:Lines
          cgf:Polygons
          cite:DividedRoutes
          cgf:MLines
          sf:AggregateGeoFeature
          cdf:Locks
          cite:Geometryless
          cite:MapNeatline
          cite:RoadSegments
          cdf:Nulls
          cdf:Deletes
          cite:Forests
          cite:Ponds
          cdf:Updates
          cdf:Fifteen
          cdf:Seven
          cite:Bridges
          sf:GenericEntity
          cite:Lakes
          cite:Buildings
          cite:Streams
          cite:BasicPolygons
          cdf:Inserts
          cgf:MPolygons
          */
                    
//          Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");                    
//          for (LayerInfo info : catalog.getLayers()) {                            
//              System.out.println(
//                      info.getResource().getNamespace().getName() 
//                      + ":" +info.getName());
//          }
          
          DataAccessRuleDAO dao = DataAccessRuleDAO.get();
          dao.getRules();
          dao.addRule(new DataAccessRule("*", "*", AccessMode.WRITE, 
                  GeoServerRole.ADMIN_ROLE.getAuthority()));
          dao.addRule(new DataAccessRule(MockData.CITE_PREFIX, "*", AccessMode.READ,                   
                  "ROLE_AUTENTICATED"));
          dao.addRule(new DataAccessRule(MockData.CITE_PREFIX, MockData.LAKES.getLocalPart(), AccessMode.WRITE,                   
                  "ROLE_WMS,ROLE_WFS"));
          dao.addRule(new DataAccessRule(MockData.CITE_PREFIX, MockData.BRIDGES.getLocalPart(), AccessMode.WRITE,                   
                  "ROLE_WMS,ROLE_WFS"));          
          dao.storeRules();

      }
      
      protected boolean testErrorMessagesWithRegExp(String regExp) {
          List<Serializable> msgs = tester.getMessages(FeedbackMessage.ERROR);
          for (Serializable msg : msgs) {
              if (msg.toString().matches(regExp))
                  return true;
          }
          return false;
      }
      
      
      protected void createUserPasswordAuthProvider(String name, String ugName) throws Exception{
          UsernamePasswordAuthenticationProviderConfig config = new 
                  UsernamePasswordAuthenticationProviderConfig();
          config.setName(name);
          config.setClassName(UsernamePasswordAuthenticationProvider.class.getName());
          config.setUserGroupServiceName(ugName);
          getSecurityManager().saveAuthenticationProvider(config);
      }
      
      protected AbstractSecurityPage initializeForRoleServiceNamed(String name) {
          initializeForService(name, "roleServices");
          //tester.assertRenderedPage(RoleTabbedPage.class);
          return (AbstractSecurityPage) tester.getLastRenderedPage();
      }

      protected AbstractSecurityPage initializeForUGServiceNamed(String name) {
          initializeForService(name, "userGroupServices");
          //tester.assertRenderedPage(UserGroupTabbedPage.class);
          return (AbstractSecurityPage) tester.getLastRenderedPage();
      }

      private void initializeForService(String name,String panelName) {
          Page page = new UserGroupRoleServicesPage();
          tester.startPage(page);
          tester.assertRenderedPage(page.getPageClass());

          SecurityNamedServicesPanel panel = (SecurityNamedServicesPanel) page.get("panel:panel:" +panelName);
          
          
//          AjaxTabbedPanel tabbedPanel=  (AjaxTabbedPanel) page.get(AbstractSecurityPage.TabbedPanelId);
//          String linkId = tabbedPanel.getId()+":tabs-container:tabs:"+tabIndex+":link";        
//          tester.clickLink(linkId,true);

          DataView<SecurityNamedServiceConfig> dv =
                  (DataView<SecurityNamedServiceConfig>)panel.get("table:listContainer:items");
          //page.get("tabbedPanel:panel:table:listContainer:items");
          
          Iterator<Item<SecurityNamedServiceConfig>> it = dv.getItems();
          while (it.hasNext()) {
              Item<SecurityNamedServiceConfig> item = it.next();
              if (name.equals(item.getModelObject().getName()))
                  tester.clickLink(item.getPageRelativePath()+":itemProperties:0:component:link");
          }          
      }
}
