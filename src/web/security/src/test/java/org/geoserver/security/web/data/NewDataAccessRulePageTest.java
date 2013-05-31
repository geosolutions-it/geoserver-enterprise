package org.geoserver.security.web.data;

import java.util.List;

import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.role.NewRolePage;

public class NewDataAccessRulePageTest extends AbstractSecurityWicketTestSupport {

    NewDataAccessRulePage page;
    
    public void testFill() throws Exception {
        
        initializeForXML();
        //insertValues();        
        tester.startPage(page=new NewDataAccessRulePage());        
        tester.assertRenderedPage(NewDataAccessRulePage.class);
        
        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.workspaceChoice.getChoices(),MockData.CITE_PREFIX);        
        form.select("workspace", index);
        tester.executeAjaxEvent("form:workspace", "onchange");
        form = tester.newFormTester("form");
        index = indexOf(page.layerChoice.getChoices(),MockData.STREAMS.getLocalPart());
        form.select("layer", index);
        
        index = page.accessModeChoice.getChoices().indexOf(AccessMode.READ);
        form.select("accessMode",index);
                
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);
        
        // add a role on the fly
        form.submit("roles:addRole");        
        tester.assertRenderedPage(NewRolePage.class);
        form=tester.newFormTester("form");                
        form.setValue("name", "ROLE_NEW");
        form.submit("save");
        
        // assign the new role to the method
        form=tester.newFormTester("form");
        tester.assertRenderedPage(NewDataAccessRulePage.class);
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_NEW").getAuthority());
        
        // reopen new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(NewDataAccessRulePage.class);
        
        // now save
        form=tester.newFormTester("form");
        form.submit("save");
        
        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(DataSecurityPage.class);

        DataAccessRule foundRule=null;
        for (DataAccessRule rule : DataAccessRuleDAO.get().getRules()) {
            if (MockData.CITE_PREFIX.equals(rule.getWorkspace())
                    && MockData.STREAMS.getLocalPart().equals(rule.getLayer())
                    && AccessMode.READ.equals(rule.getAccessMode())) {
                foundRule = rule;
                break;
            }
        }
        assertNotNull(foundRule);
        assertEquals(1,foundRule.getRoles().size());
        assertEquals("ROLE_NEW",foundRule.getRoles().iterator().next());        
    }
    
    public void testDuplicateRule() throws Exception {
        initializeForXML();
        initializeServiceRules();

        tester.startPage(page=new NewDataAccessRulePage());

        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.workspaceChoice.getChoices(),MockData.CITE_PREFIX);        
        form.select("workspace", index);
        tester.executeAjaxEvent("form:workspace", "onchange");
        form = tester.newFormTester("form");
        index = indexOf(page.layerChoice.getChoices(),MockData.BRIDGES.getLocalPart());
        form.select("layer", index);
        
        index = page.accessModeChoice.getChoices().indexOf(AccessMode.WRITE);
        form.select("accessMode",index);
        
        form.setValue("roles:palette:recorder", "ROLE_WMS");
                        
        form.submit("save");                
        assertTrue(testErrorMessagesWithRegExp(".*"+MockData.CITE_PREFIX+"\\."+
                MockData.BRIDGES.getLocalPart()+".*"));
        tester.assertRenderedPage(NewDataAccessRulePage.class);
    }
    
    public void testEmptyRoles() throws Exception {
        initializeForXML();
        initializeServiceRules();
        tester.startPage(page=new NewDataAccessRulePage());
                
        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.workspaceChoice.getChoices(),MockData.CITE_PREFIX);        
        form.select("workspace", index);
        tester.executeAjaxEvent("form:workspace", "onchange");
        form = tester.newFormTester("form");
        index = indexOf(page.layerChoice.getChoices(),MockData.STREAMS.getLocalPart());
        form.select("layer", index);
        
        index = page.accessModeChoice.getChoices().indexOf(AccessMode.READ);
        form.select("accessMode",index);
                        
        form.submit("save");
        assertTrue(testErrorMessagesWithRegExp(".*no role.*"));
        tester.assertRenderedPage(NewDataAccessRulePage.class);
    }

    
    public void testReadOnlyRoleService() throws Exception{
        initializeForXML();
        activateRORoleService();
        tester.startPage(page=new NewDataAccessRulePage());
        tester.assertInvisible("form:roles:addRole");
    }

    public void testAddAdminRule() throws Exception {
        initializeForXML();
   
        tester.startPage(page=new NewDataAccessRulePage());
        tester.assertRenderedPage(NewDataAccessRulePage.class);

        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.workspaceChoice.getChoices(),MockData.CITE_PREFIX);        
        form.select("workspace", index);
        tester.executeAjaxEvent("form:workspace", "onchange");
        form = tester.newFormTester("form");
        index = indexOf(page.layerChoice.getChoices(),MockData.STREAMS.getLocalPart());
        form.select("layer", index);
        
        index = page.accessModeChoice.getChoices().indexOf(AccessMode.ADMIN);
        form.select("accessMode",index);
                
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);
        
        // add a role on the fly
        form.submit("roles:addRole");        
        tester.assertRenderedPage(NewRolePage.class);
        form=tester.newFormTester("form");                
        form.setValue("name", "ROLE_NEW");
        form.submit("save");
        
        // assign the new role to the method
        form=tester.newFormTester("form");
        tester.assertRenderedPage(NewDataAccessRulePage.class);
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_NEW").getAuthority());
        
        // reopen new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        tester.clickLink("form:cancel");
        tester.assertRenderedPage(NewDataAccessRulePage.class);
        
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();

        DataAccessRule rule = new DataAccessRule(MockData.CITE_PREFIX, MockData.STREAMS.getLocalPart(), AccessMode.ADMIN);
        assertFalse(dao.getRules().contains(rule));
       
        // now save
        form=tester.newFormTester("form");
        form.submit("save");

        assertTrue(dao.getRules().contains(rule));
    }

    protected int indexOf(List<? extends String> strings, String searchValue) {
        int index =0;
        for (String s : strings) {
            if (s.equals(searchValue))
                return index;
            index++;
        }
        assertTrue(index!=-1);
        return -1;
    }

}
