package org.geoserver.security.web;


import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.PageParameters;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

public abstract class AbstractTabbedListPageTest<T> extends AbstractSecurityWicketTestSupport {
    
     public static final String FIRST_COLUM_PATH="itemProperties:0:component:link";
    

    @Override
    protected void setUpInternal() throws Exception {        
        login();
    }

    public void testRenders() throws Exception {
        initializeForXML();
        tester.assertRenderedPage(listPage(getServiceName()).getClass());
    }
    
    
            
    protected String getItemsPath() {
        return getTabbedPanelPath()+":panel:table:listContainer:items";
    };

    protected abstract String getTabbedPanelPath();
    protected abstract String getServiceName();
    abstract protected Page listPage(String serviceName);
    abstract protected Page newPage(AbstractSecurityPage responsePage,Object...params);
    abstract protected Page editPage(AbstractSecurityPage responsePage,Object...params);
 
    abstract protected String getSearchString() throws Exception;
    abstract protected Property<T> getEditProperty();
    abstract protected boolean checkEditForm(String search);
    
    
    public void testEdit() throws Exception {
        // the name link for the first user
        initializeForXML();
        insertValues();
        AbstractSecurityPage listPage = (AbstractSecurityPage) listPage(getServiceName());
        //tester.startPage(listPage);
                   
        String search = getSearchString();
        assertNotNull(search);
        Component c = getFromList(FIRST_COLUM_PATH, search, getEditProperty());
        assertNotNull(c);
        tester.clickLink(c.getPageRelativePath());
        
        tester.assertRenderedPage(editPage(listPage).getClass());
        assertTrue(checkEditForm(search));                
    }
    
    
    
    
    protected Component getFromList(String columnPath, Object columnValue, Property<T> property) {
        
        MarkupContainer listView = (MarkupContainer) tester.getLastRenderedPage().get(getItemsPath());
        
        @SuppressWarnings("unchecked")
        Iterator<MarkupContainer> it = (Iterator<MarkupContainer>) listView.iterator();
        
        while (it.hasNext()) {
            MarkupContainer m = it.next();
            Component c = m.get(columnPath);
            @SuppressWarnings("unchecked")
            T modelObject = (T) c.getDefaultModelObject();
            if (columnValue.equals(property.getPropertyValue(modelObject)))
                return c;
        }
        return null;
    }
    
    public void testNew() throws Exception {
        initializeForXML();
        listPage(getServiceName());        
        tester.clickLink(getTabbedPanelPath()+":panel:header:addNew");        
        Page newPage = tester.getLastRenderedPage();
        tester.assertRenderedPage(newPage.getClass());
    }
    
    
    
    public void testRemove() throws Exception {
        initializeForXML();
        insertValues();
        addAdditonalData();
        doRemove(getTabbedPanelPath()+":panel:header:removeSelected");
    }
    
    
    protected void doRemove(String pathForLink) throws Exception {
        
        
        GeoserverTablePanelTestPage testPage = 
         new GeoserverTablePanelTestPage(new ComponentBuilder() {            
            private static final long serialVersionUID = 1L;

            public Component buildComponent(String id) {
                try {
                    return listPage(getServiceName());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        tester.startPage(testPage);
        
        String selectAllPath = testPage.getWicketPath()+":"+getTabbedPanelPath()+":panel:table:listContainer:selectAllContainer:selectAll";        
        tester.assertComponent(selectAllPath, CheckBox.class);
        
        FormTester ft = tester.newFormTester(GeoserverTablePanelTestPage.FORM);
        ft.setValue(testPage.getComponentId()+":"+getTabbedPanelPath()+":panel:table:listContainer:selectAllContainer:selectAll", "true");
        tester.executeAjaxEvent(selectAllPath, "onclick");

        String windowPath=testPage.getWicketPath()+":"+getTabbedPanelPath()+ ":panel:dialog:dialog";       
        ModalWindow w  = (ModalWindow) testPage.get(windowPath);                        
        assertNull(w.getTitle()); // window was not opened
        tester.executeAjaxEvent(pathForLink, "onclick");
        assertNotNull(w.getTitle()); // window was opened        
        simulateDeleteSubmit();        
        executeModalWindowCloseButtonCallback(w);
    }
        
    protected abstract void simulateDeleteSubmit() throws Exception;        

    protected Component getRemoveLink() {
        Component result =tester.getLastRenderedPage().get(getTabbedPanelPath()+":panel:header:removeSelected");
        assertNotNull(result);
        return result;
    }
    
    protected Component getRemoveLinkWithRoles() {
        Component result =tester.getLastRenderedPage().get(getTabbedPanelPath()+":panel:header:removeSelectedWithRoles");
        assertNotNull(result);
        return result;
    }

    
    protected Component getAddLink() {
        Component result =tester.getLastRenderedPage().get(getTabbedPanelPath()+":panel:header:addNew");
        assertNotNull(result);
        return result;
    }

    
}
