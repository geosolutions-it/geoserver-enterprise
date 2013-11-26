//package it.geosolutions.geoserver.jms.impl.test.handlers;
//
//import it.geosolutions.geoserver.jms.impl.handlers.DocumentFile;
//import it.geosolutions.geoserver.jms.impl.handlers.DocumentFileHandler;
//
//import java.io.File;
//import java.io.IOException;
//
//import org.jdom.JDOMException;
//import org.junit.Test;
//import org.springframework.core.io.ClassPathResource;
//
//import com.thoughtworks.xstream.XStream;
//
//public class SmallXMLFileTest {
//    final static XStream stream = new XStream();
//    
//    @Test
//    public void test() {
//        // DataTestCase data=new DataTestCase("NAME");
//        DocumentFile xmlFile;
//        try {
//            File sld = new ClassPathResource("src/test/resources/line.sld").getFile();
//            System.out.println(sld.getAbsolutePath());
//            xmlFile = new DocumentFile(sld);
//            final DocumentFileHandler fileHandler = new DocumentFileHandler(stream, DocumentFile.class);
//            System.out.println("SERIALIZE: " + fileHandler.serialize(xmlFile));
//        } catch (JDOMException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        } catch (IOException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//    }
//}
