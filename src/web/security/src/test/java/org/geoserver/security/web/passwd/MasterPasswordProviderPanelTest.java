package org.geoserver.security.web.passwd;

import java.net.URL;

import org.apache.wicket.Component;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.password.URLMasterPasswordProvider;
import org.geoserver.security.password.URLMasterPasswordProviderConfig;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.web.AbstractSecurityNamedServicePanelTest;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.SecurityNamedServiceEditPage;
import org.geoserver.security.web.SecurityNamedServiceNewPage;

public class MasterPasswordProviderPanelTest extends AbstractSecurityNamedServicePanelTest {

    @Override
    protected AbstractSecurityPage getBasePage() {
        return new PasswordPage();
    }

    @Override
    protected String getBasePanelId() {
        return "form:masterPasswordProviders";
    }

    @Override
    protected Integer getTabIndex() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Class<? extends Component> getNamedServicesClass() {
        return MasterPasswordProvidersPanel.class;
    }

    @Override
    protected String getDetailsFormComponentId() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void testAddModify() throws Exception{
        initializeForXML();
        
        activatePanel();
        
        assertEquals(1, countItmes());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        
        assertNull(getSecurityNamedServiceConfig("xxxxxxxx"));
        
        // Test simple add
        clickAddNew();
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        //detailsPage = (PasswordPolicyPage) tester.getLastRenderedPage();

        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();
        
        setSecurityConfigName("default2");
        clickCancel();
        
        tester.assertRenderedPage(basePage.getClass());
        assertEquals(1, countItmes());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNull(getSecurityNamedServiceConfig("default2"));

        clickAddNew();

        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        formTester.setValue("panel:content:uRL", "file:passwd");
        clickSave();

        tester.assertRenderedPage(basePage.getClass());
        assertEquals(2, countItmes());
        assertNotNull(getSecurityNamedServiceConfig("default"));
        assertNotNull(getSecurityNamedServiceConfig("default2"));
        
        // test add with name clash        
        clickAddNew();        
        setSecurityConfigClassName(URLMasterPasswordProviderPanelInfo.class);
        newFormTester();
        setSecurityConfigName("default2");
        formTester.setValue("panel:content:uRL", "file:passwd");
        clickSave(); // should not work
        
        tester.assertRenderedPage(SecurityNamedServiceNewPage.class);
        testErrorMessagesWithRegExp(".*default2.*");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());
        // end test add with name clash        

          // start test modify        
        clickNamedServiceConfig("default2");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.debugComponentTrees();
        newFormTester("panel:panel:form");
        formTester.setValue("panel:uRL", "file:passwd2");
        clickCancel();
        tester.assertRenderedPage(basePage.getClass());

        URLMasterPasswordProviderConfig config = 
                (URLMasterPasswordProviderConfig) getSecurityNamedServiceConfig("default2");
        assertEquals(new URL("file:passwd"),config.getURL());

        clickNamedServiceConfig("default2");

        newFormTester("panel:panel:form");
        formTester.setValue("panel:uRL", "file:passwd2");
        clickSave();
        
        tester.assertRenderedPage(basePage.getClass());

        config = 
                (URLMasterPasswordProviderConfig) getSecurityNamedServiceConfig("default2");
        assertEquals(new URL("file:passwd2"),config.getURL());
    }
    
    public void testRemove() throws Exception {
        initializeForXML();
        URLMasterPasswordProviderConfig config = new URLMasterPasswordProviderConfig();
        config.setName("default2");
        config.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
        config.setURL(new URL("file:passwd"));
        
        getSecurityManager().saveMasterPasswordProviderConfig(config);
        activatePanel();

        assertEquals(2, countItmes());
        
        doRemove(null, "default2");
        assertNull(getSecurityManager().loadMasterPassswordProviderConfig("default2"));
        assertEquals(1, countItmes());
    }

}
