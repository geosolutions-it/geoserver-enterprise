package org.geoserver.security.web.data;

import java.lang.reflect.Method;
import java.util.Collections;

import org.apache.wicket.Component;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.RadioChoice;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.MockData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.web.AbstractListPageTest;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public class DataSecurityPageTest extends AbstractListPageTest<DataAccessRule> {

    protected Page listPage(PageParameters params) {
        return new  DataSecurityPage();
    }
    protected Page newPage(Object...params) {
        return new  NewDataAccessRulePage();
    }
    protected Page editPage(Object...params) {
        if (params.length==0)
            return new  EditDataAccessRulePage( new DataAccessRule("it.geosolutions", "layer.dots", 
                    AccessMode.READ, Collections.singleton("ROLE_ABC")));
        else
            return new  EditDataAccessRulePage( (DataAccessRule) params[0]);
    }


    @Override
    protected Property<DataAccessRule> getEditProperty() {
        return DataAccessRuleProvider.RULEKEY;
    }
    
    @Override
    protected boolean checkEditForm(String objectString) {
        String[] array=objectString.split("\\.");
        return  array[0].equals(
                    tester.getComponentFromLastRenderedPage("form:workspace").getDefaultModelObject()) &&
                array[1].equals( 
                        tester.getComponentFromLastRenderedPage("form:layer").getDefaultModelObject());
    }
    
    @Override
    protected String getSearchString() throws Exception{
        for (DataAccessRule rule : DataAccessRuleDAO.get().getRules()) {
            if (MockData.CITE_PREFIX.equals(rule.getWorkspace()) && 
                    MockData.BRIDGES.getLocalPart().equals(rule.getLayer()))
                return rule.getKey();
        }
        return null;
    }

    
    
    @Override
    protected void simulateDeleteSubmit() throws Exception {
        
        assertTrue (DataAccessRuleDAO.get().getRules().size()>0);
        
        SelectionDataRuleRemovalLink link = (SelectionDataRuleRemovalLink) getRemoveLink();
        Method m = link.delegate.getClass().getDeclaredMethod("onSubmit", AjaxRequestTarget.class,Component.class);
        m.invoke(link.delegate, null,null);
        
        assertEquals(0,DataAccessRuleDAO.get().getRules().size());        
    }

    public void testDefaultCatalogMode() throws Exception {
        tester.startPage(DataSecurityPage.class);
        tester.assertRenderedPage(DataSecurityPage.class);
        assertEquals("HIDE", tester.getComponentFromLastRenderedPage("catalogModeForm:catalogMode")
                .getDefaultModelObject().toString());
    }

    public void testEditCatalogMode() throws Exception {
        tester.startPage(DataSecurityPage.class);
        tester.assertRenderedPage(DataSecurityPage.class);
        
        // simple test 
        assertFalse(("CHALLENGE".equals(tester.getComponentFromLastRenderedPage(
                "catalogModeForm:catalogMode").getDefaultModelObject())));
        
        // edit catalogMode value
        final FormTester form = tester.newFormTester("catalogModeForm");

        form.select("catalogMode", 1);

        form.getForm().visitChildren(RadioChoice.class, new IVisitor() {
            public Object component(final Component component) {
                if (component.getId().equals("catalogMode")) {
                    ((RadioChoice) component).onSelectionChanged();
                }
                return CONTINUE_TRAVERSAL;
            }
        });

        assertEquals("MIXED", tester.getComponentFromLastRenderedPage(
                "catalogModeForm:catalogMode").getDefaultModelObject().toString());

    }
}
