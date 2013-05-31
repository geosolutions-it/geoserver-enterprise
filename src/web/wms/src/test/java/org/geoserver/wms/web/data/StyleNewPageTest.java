package org.geoserver.wms.web.data;

import java.io.FileReader;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.web.GeoServerWicketTestSupport;

public class StyleNewPageTest extends GeoServerWicketTestSupport {
    
    @Override
    protected void setUpInternal() throws Exception {
        login();
        tester.startPage(StyleNewPage.class);
        // org.geoserver.web.wicket.WicketHierarchyPrinter.print(tester.getLastRenderedPage(), true, false);
    }

    public void testLoad() throws Exception {
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertNoErrorMessage();
        
        tester.assertComponent("form:name", TextField.class);
        tester.assertComponent("form:SLD:editorContainer:editor", TextArea.class);
        tester.assertComponent("uploadForm:filename", FileUploadField.class);
        
        tester.assertModelValue("form:name", null);
    }
    
    public void testUpload() throws Exception {
        FormTester upload = tester.newFormTester("uploadForm");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        
        
        upload.setFile("filename", styleFile, "application/xml");
        upload.submit();
        
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertModelValue("form:SLD", sld);
    }
    
    public void testMissingName() throws Exception {
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        form.setValue("SLD:editorContainer:editor", sld);
        form.submit();
       
        
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'Name' is required."});
    }
    
    public void testMissingStyle() throws Exception {
        FormTester form = tester.newFormTester("form");
        form.setValue("name", "test");
        form.submit();
       
        
        tester.assertRenderedPage(StyleNewPage.class);
        tester.assertErrorMessages(new String[] {"Field 'SLD' is required."});
    }

    
    public void testNewStyle() throws Exception {
        
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        String sld = IOUtils.toString(new FileReader(styleFile)).replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        form.setValue("SLD:editorContainer:editor", sld);
        form.setValue("name", "test");
        form.submit();
        
        tester.assertRenderedPage(StylePage.class);
        assertNotNull(getCatalog().getStyleByName("test"));
    }
    
    public void testNewStyleNoSLD() throws Exception {
        
        FormTester form = tester.newFormTester("form");
        File styleFile = new File(new java.io.File(getClass().getResource("default_point.sld").toURI()));
        form.setValue("name", "test");
        form.submit();
        
        tester.assertRenderedPage(StyleNewPage.class);
        assertTrue(tester.getMessages(FeedbackMessage.ERROR).size() > 0);
    }
    
//    Cannot make this one to work, the sld text area is not filled in the test
//    and I don't understand why, in the real world it is
//    public void testValidate() throws Exception {
//        tester.clickLink("form:sld:validate", false);
//        
//        tester.assertRenderedPage(StyleNewPage.class);
//        tester.assertErrorMessages(new String[] {"Invalid style"});
//    }
    
    
}
