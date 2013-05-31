package org.geoserver.security.web.data;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.data.test.MockData;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.web.AbstractConfirmRemovalPanelTest;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;

public class ConfirmRemovalDataAccessRulePanelTest extends AbstractConfirmRemovalPanelTest<DataAccessRule> {
    private static final long serialVersionUID = 1L;

    public void testRemoveRule() throws Exception {
        initializeForXML();
        removeObject();        
    }

    @Override
    protected void setupPanel(final List<DataAccessRule> roots) {
        tester.startPage(new FormTestPage(new ComponentBuilder() {
            private static final long serialVersionUID = 1L;
            public Component buildComponent(String id) {                
                return new ConfirmRemovalDataAccessRulePanel(id, roots) {
                    @Override
                    protected IModel<String> canRemove(DataAccessRule data) {
                        SelectionDataRuleRemovalLink link = new SelectionDataRuleRemovalLink("XXX",null,null);
                        return link.canRemove(data);
                    }

                    private static final long serialVersionUID = 1L;                    
                };
            }
        })); 
    }

    
    @Override
    protected DataAccessRule getRemoveableObject() throws Exception {
        for (DataAccessRule rule : DataAccessRuleDAO.get().getRules()) {
            if (MockData.CITE_PREFIX.equals(rule.getWorkspace()) && 
                MockData.BRIDGES.getLocalPart().equals(rule.getLayer()))
                return rule;
        }
        return null;
    }
    @Override
    protected DataAccessRule getProblematicObject() throws Exception {
        return null;
    }

    @Override
    protected String getProblematicObjectRegExp() throws Exception {
        return null;
    }

    @Override
    protected String getRemoveableObjectRegExp() throws Exception {
        DataAccessRule rule = getRemoveableObject();
        return ".*"+rule.getWorkspace() + ".*" + rule.getLayer()
                +".*" + "ROLE_WFS"+".*";                
    }
    
}
