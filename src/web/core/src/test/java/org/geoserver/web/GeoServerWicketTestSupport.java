package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public abstract class GeoServerWicketTestSupport extends GeoServerSecurityTestSupport {
    public static WicketTester tester;

    public void oneTimeSetUp() throws Exception {        
        super.oneTimeSetUp();
        // prevent Wicket from bragging about us being in dev mode (and run
        // the tests as if we were in production all the time)
        System.setProperty("wicket.configuration", "deployment");
        
        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);
        
        GeoServerApplication app = 
            (GeoServerApplication) applicationContext.getBean("webApplication");
        tester = new WicketTester(app);
        app.init();
        
    }

    @Override
    protected void oneTimeTearDown() throws Exception {
        super.oneTimeTearDown();
        tester.destroy();
    }

    public GeoServerApplication getGeoServerApplication(){
        return GeoServerApplication.get();
    }

    /**
     * Logs in as administrator.
     */
    public void login(){
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    /**
     * Logs in with the specified credentials and associates the specified roles with the resulting
     * authentication. 
     */
    public void login(String user, String passwd, String... roles) {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l= new ArrayList<GrantedAuthority>();
        for (String role : roles) {
            l.add(new GrantedAuthorityImpl(role));
        }
        
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,passwd,l));
    }

    public void logout(){
        login("anonymousUser","", "ROLE_ANONYMOUS");
    }
    
    /**
     * Prints the specified component/page containment hierarchy to the standard output
     * <p>
     * Each line in the dump looks like: <componentId>(class) 'value'
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue) {
        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }
    
   /**
    * Prints the specified component/page containment hierarchy to the standard output
    * <p>
    * Each line in the dump looks like: <componentId>(class) 'value'
    * @param c the component to be printed
    * @param dumpClass if enabled, the component classes are printed as well
    * @param dumpValue if enabled, the component values are printed as well
    */
   public void print(Component c, boolean dumpClass, boolean dumpValue, boolean dumpPath) {
       WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
   }
    
    public void prefillForm(final FormTester tester) {
        Form form = tester.getForm();
        form.visitChildren(new Component.IVisitor() {
            
            public Object component(Component component) {
                if(component instanceof FormComponent) {
                    FormComponent fc = (FormComponent) component;
                    String name = fc.getInputName();
                    String value = fc.getValue();
                    
                    tester.setValue(name, value);
                }
                return Component.IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }
    
    /**
     * Finds the component whose model value equals to the specified content, and
     * the component class is equal, subclass or implementor of the specified class
     * @param root the component under which the search is to be performed
     * @param content 
     * @param componentClass the target class, or null if any component will do
     * @return
     */
    public Component findComponentByContent(MarkupContainer root, Object content, Class componentClass) {
        ComponentContentFinder finder = new ComponentContentFinder(content);
        root.visitChildren(componentClass, finder);
        return finder.candidate;
    }
    
    class ComponentContentFinder implements Component.IVisitor {
        Component candidate;
        Object content;
        
        ComponentContentFinder(Object content) {
            this.content = content;
        }
        

        public Object component(Component component) {
            if(content.equals(component.getDefaultModelObject())) {
                this.candidate = component;
                return Component.IVisitor.STOP_TRAVERSAL;
            }
            return Component.IVisitor.CONTINUE_TRAVERSAL;
        }
        
    }
    
    /**
     * Helper method to initialize a standalone WicketTester with the proper 
     * customizations to do message lookups.
     */
    public static void initResourceSettings(WicketTester tester) {
        tester.getApplication().getResourceSettings().setResourceStreamLocator(new GeoServerResourceStreamLocator());
        tester.getApplication().getResourceSettings().addStringResourceLoader(0, new GeoServerStringResourceLoader());
    }
}
