package org.geoserver.security.web.role;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.SecurityNamedServiceEditPage;

public class EditRolePageTest extends AbstractSecurityWicketTestSupport {

    EditRolePage page;

    public void testFill() throws Exception{
        initializeForXML();
        doTestFill();
    }
    
    public void testFill2() throws Exception{
        initializeForXML();
        doTestFill2();
    }

    protected void doTestFill() throws Exception {
        insertValues();        
        
        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRoleServiceName());
        tester.startPage(page=(EditRolePage) new EditRolePage(getRoleServiceName(),
            gaService.getRoleByName("ROLE_WFS")).setReturnPage(returnPage));
        tester.assertRenderedPage(EditRolePage.class);
        
        assertFalse(tester.getComponentFromLastRenderedPage("form:name").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        assertTrue(tester.getComponentFromLastRenderedPage("form:parent").isEnabled());
        tester.assertVisible("form:save");

        tester.assertModelValue("form:name", "ROLE_WFS");
        tester.assertModelValue("form:parent", "ROLE_AUTHENTICATED");
        
        FormTester form = tester.newFormTester("form");
        form.setValue("parent", null);
        //form.select("parent", index);
        
        
        //tester.executeAjaxEvent("form:properties:add", "onclick");
        //form = tester.newFormTester("form");
        
        
        //form.setValue("properties:container:list:0:key", "bbox");
        //form.setValue("properties:container:list:0:value", "10 10 20 20");
                
        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);
        tester.assertErrorMessages(new String[0]);
        
        GeoServerRole role = gaService.getRoleByName("ROLE_WFS");
        assertNotNull(role);
        //assertEquals(1,role.getProperties().size());
        //assertEquals("10 10 20 20",role.getProperties().get("bbox"));
        GeoServerRole parentRole = gaService.getParentRole(role);
        assertNull(parentRole);
    }
    
    protected void doTestFill2() throws Exception {
        insertValues();        
        
        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRoleServiceName());
        tester.startPage(page=(EditRolePage) new EditRolePage(getRoleServiceName(),
            gaService.getRoleByName("ROLE_AUTHENTICATED")).setReturnPage(returnPage));
        tester.assertRenderedPage(EditRolePage.class);
        
        tester.assertModelValue("form:name", "ROLE_AUTHENTICATED");
        tester.assertModelValue("form:parent", null);

        // role params are shown sorted by key
        tester.assertModelValue("form:properties:container:list:0:key", "bbox");
        tester.assertModelValue("form:properties:container:list:0:value", "lookupAtRuntime");
        tester.assertModelValue("form:properties:container:list:1:key", "employee");
        tester.assertModelValue("form:properties:container:list:1:value", "");
        
        tester.executeAjaxEvent("form:properties:container:list:1:remove", "onclick");
        FormTester form = tester.newFormTester("form");
        form.submit("save");
        tester.assertRenderedPage(SecurityNamedServiceEditPage.class);

        GeoServerRole role = gaService.getRoleByName("ROLE_AUTHENTICATED");
        assertNotNull(role);
        assertEquals(1,role.getProperties().size());
        assertEquals("lookupAtRuntime",role.getProperties().get("bbox"));

    }
        
    public void testReadOnlyRoleService() throws Exception {
        initializeForXML();
        activateRORoleService();
        
        AbstractSecurityPage returnPage = initializeForRoleServiceNamed(getRORoleServiceName());
        tester.startPage(page=(EditRolePage) new EditRolePage(getRORoleServiceName(),
            GeoServerRole.ADMIN_ROLE).setReturnPage(returnPage));
        tester.assertRenderedPage(EditRolePage.class);
        assertFalse(tester.getComponentFromLastRenderedPage("form:name").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:properties").isEnabled());
        assertFalse(tester.getComponentFromLastRenderedPage("form:parent").isEnabled());
        tester.assertInvisible("form:save");
    }

}
