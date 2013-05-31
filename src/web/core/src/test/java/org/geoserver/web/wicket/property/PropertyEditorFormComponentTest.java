package org.geoserver.web.wicket.property;

import java.util.Iterator;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.apache.wicket.markup.html.list.ListItem;

public class PropertyEditorFormComponentTest extends GeoServerWicketTestSupport {

    Foo foo;

    protected void setUpInternal() throws Exception {
        foo = new Foo();
    }

    void startPage() {
        tester.startPage(new PropertyEditorTestPage(foo));
        tester.assertRenderedPage(PropertyEditorTestPage.class);
    }

    public void testAdd() {
        //JD:for the life of me i can't figure out any sane way to test forms with ajax in the mix
        // so unable to test the case of adding multiple key/value pairs since it involves 
        // intermixing of the two
        startPage();
        
        tester.clickLink("form:props:add", true);
        tester.assertComponent("form:props:container:list:0:key",TextField.class);
        tester.assertComponent("form:props:container:list:0:value",TextField.class);
        tester.assertComponent("form:props:container:list:0:remove",AjaxLink.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("props:container:list:0:key", "foo");
        form.setValue("props:container:list:0:value", "bar");
        form.submit();

        assertEquals(1, foo.getProps().size());
        assertEquals("bar", foo.getProps().get("foo"));
    }

    public void testRemove() {
        foo.getProps().put("foo", "bar");
        foo.getProps().put("bar", "baz");
        foo.getProps().put("baz", "foo");
        startPage();

        tester.assertComponent("form:props:container:list:0:remove",AjaxLink.class);
        tester.assertComponent("form:props:container:list:1:remove",AjaxLink.class);
        tester.assertComponent("form:props:container:list:2:remove",AjaxLink.class);
        try {
            tester.assertComponent("form:props:container:list:3:remove",AjaxLink.class);
            fail();
        } catch(Exception e) {}

        ListView list = 
            (ListView) tester.getComponentFromLastRenderedPage("form:props:container:list");
        assertNotNull(list);

        int i = 0;
        for (Iterator<ListItem> it = list.iterator(); it.hasNext(); i++) {
            if ("baz".equals(it.next().get("key").getDefaultModelObjectAsString())) {
                break;
            }
        }
        assertFalse(i == 3);

        tester.clickLink("form:props:container:list:"+i+":remove", true);
        tester.newFormTester("form").submit();

        assertEquals(2, foo.getProps().size());
        assertEquals("bar", foo.getProps().get("foo"));
        assertEquals("baz", foo.getProps().get("bar"));
        assertFalse(foo.getProps().containsKey("baz"));
    }

    public void testAddRemove() {
        startPage();
        tester.clickLink("form:props:add", true);
        tester.assertComponent("form:props:container:list:0:key",TextField.class);
        tester.assertComponent("form:props:container:list:0:value",TextField.class);
        tester.assertComponent("form:props:container:list:0:remove",AjaxLink.class);

        FormTester form = tester.newFormTester("form");
        form.setValue("props:container:list:0:key", "foo");
        form.setValue("props:container:list:0:value", "bar");

        tester.clickLink("form:props:container:list:0:remove", true);

        assertNull(form.getForm().get("props:container:list:0:key"));
        assertNull(form.getForm().get("props:container:list:0:value"));
        assertNull(form.getForm().get("props:container:list:0:remove"));
        form.submit();

        assertTrue(foo.getProps().isEmpty());
    }
}
