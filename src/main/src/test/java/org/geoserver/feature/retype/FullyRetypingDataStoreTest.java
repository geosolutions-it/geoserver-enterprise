package org.geoserver.feature.retype;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureLock;
import org.geotools.data.FeatureLockFactory;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.io.WKTReader;

public class FullyRetypingDataStoreTest extends TestCase {

    SimpleFeatureType primitive;
    
    final static String RENAMED = "primitive";

    RetypingDataStore rts;

    private File data;

    private Id fidFilter;

    private String fid;

    @Override
    protected void setUp() throws Exception {
    	// setup property file
        data = File.createTempFile("retype", "data", new File("./target"));
        data.delete();
        data.mkdir();
        final String fileName = MockData.PRIMITIVEGEOFEATURE.getLocalPart() + ".properties";
        URL properties = MockData.class.getResource(fileName);
        IOUtils.copy(properties.openStream(), new File(data, fileName));

        // build a feature type with less attributes, extra attributes, type changes
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.add("description", String.class);
        ftb.add("pointProperty", MultiPoint.class); // poly -> multi-poly
        ftb.add("intProperty", Long.class); // int -> long
        ftb.add("dateTimeProperty", Date.class); // timestamp -> date
        ftb.add("newProperty", String.class); // new property
        ftb.setName(RENAMED); // rename type
        primitive = ftb.buildFeatureType();
        
        PropertyDataStore pds = new PropertyDataStore(data);
        rts = new RetypingDataStore(pds) {
        	
        	@Override
        	protected SimpleFeatureType transformFeatureType(
        			SimpleFeatureType original) throws IOException {
        		if (original.getTypeName().equals(MockData.PRIMITIVEGEOFEATURE.getLocalPart()))
                    return primitive;
                else
                    return super.transformFeatureType(original);
        	}
        	
        	@Override
        	protected String transformFeatureTypeName(String originalName) {
        		if (originalName.equals(MockData.PRIMITIVEGEOFEATURE.getLocalPart()))
                    return primitive.getTypeName();
                else
                    return super.transformFeatureTypeName(originalName);
        	}
        	
        };

        // build a filter that will retrieve one feature only
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        fid = RENAMED + ".f001";
        fidFilter = ff.id(Collections.singleton(ff.featureId(fid)));
    }

    @Override
    protected void tearDown() throws Exception {
        IOUtils.delete(data);
    }

    public void testLookupFeatureType() throws Exception {
        try {
            rts.getSchema(MockData.GENERICENTITY.getLocalPart());
            fail("The original type name should not be visible");
        } catch (IOException e) {
            // cool, as expected
        }

        final SimpleFeatureType schema = rts.getSchema(RENAMED);
        assertNotNull(schema);
        assertEquals(primitive, schema);
    }

    public void testGetFeaturesFeatureSource() throws Exception {
        // check the schemas in feature source and feature collection
        SimpleFeatureSource fs = rts.getFeatureSource(RENAMED);
        assertEquals(primitive, fs.getSchema());
        SimpleFeatureCollection fc = fs.getFeatures();
        assertEquals(primitive, fc.getSchema());
        assertTrue(fc.size() > 0);

        // make sure the feature schema is good as well
        FeatureIterator <SimpleFeature> it = fc.features();
        SimpleFeature sf = it.next();
        it.close();

        assertEquals(primitive, sf.getFeatureType());

        // check the feature ids have been renamed as well
        assertTrue("Feature id has not been renamed, it's still " + sf.getID(), sf.getID()
                .startsWith(RENAMED));
        
        // check mappings occurred
        assertEquals("description-f001", sf.getAttribute("description"));
        assertTrue(new WKTReader().read("MULTIPOINT(39.73245 2.00342)").equalsExact(
        		(Geometry) sf.getAttribute("pointProperty")));
        assertEquals(new Long(155), sf.getAttribute("intProperty"));
        assertNull(sf.getAttribute("newProperty"));
    }

    public void testGetFeaturesReader() throws Exception {
        FeatureReader<SimpleFeatureType, SimpleFeature> fr;
        fr = rts.getFeatureReader(new Query(RENAMED), Transaction.AUTO_COMMIT);
        SimpleFeature sf = fr.next();
        fr.close();

        assertEquals(primitive, sf.getFeatureType());

        // check the feature ids have been renamed as well
        assertTrue("Feature id has not been renamed, it's still " + sf.getID(), sf.getID()
                .startsWith(RENAMED));
    }

    public void testFeautureSourceFidFilter() throws Exception {
        // grab the last feature in the collection (there are more than one)
        SimpleFeatureSource fs = rts.getFeatureSource(RENAMED);

        // build a filter that will retrieve that feature only
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        final String fid = RENAMED + ".f001";
        Filter fidFilter = ff.id(Collections.singleton(ff.featureId(fid)));

        SimpleFeatureCollection fc = fs.getFeatures(new Query(RENAMED, fidFilter));
        assertEquals(RENAMED, fc.getSchema().getName().getLocalPart());
        assertEquals(1, fc.size());
        FeatureIterator <SimpleFeature> it = fc.features();
        assertTrue(it.hasNext());
        SimpleFeature sf = it.next();
        assertFalse(it.hasNext());
        it.close();
        assertEquals(fid, sf.getID());
    }

    public void testFeatureReaderFidFilter() throws Exception {
        FeatureReader<SimpleFeatureType, SimpleFeature> fr;
        fr = rts.getFeatureReader(new Query(RENAMED, fidFilter), Transaction.AUTO_COMMIT);
        assertEquals(primitive, fr.getFeatureType());
        assertTrue(fr.hasNext());
        SimpleFeature sf = fr.next();
        assertFalse(fr.hasNext());
        fr.close();
        assertEquals(fid, sf.getID());
    }

    public void testDelete() throws Exception {
        final Query queryAll = new Query(RENAMED);

        SimpleFeatureStore store;
        store = (SimpleFeatureStore) rts.getFeatureSource(RENAMED);
        int count = store.getCount(queryAll);
        store.removeFeatures(fidFilter);

        assertEquals(count - 1, store.getCount(queryAll));
    }

    public void testModify() throws Exception {
        final Query queryAll = new Query(RENAMED);

        SimpleFeatureStore store;
        store = (SimpleFeatureStore) rts.getFeatureSource(RENAMED);
        SimpleFeature original = store.getFeatures(fidFilter).features().next();

        // test a non mapped attribute
        String newDescription = ((String) original.getAttribute("description")) + " xxx";
        store.modifyFeatures(original.getFeatureType().getDescriptor("description"), newDescription,
                fidFilter);
        SimpleFeature modified = store.getFeatures(fidFilter).features().next();
        assertEquals(newDescription, modified.getAttribute("description"));
        
        // test a mapped attribute
        MultiPoint mpo = (MultiPoint) original.getAttribute("pointProperty");
        MultiPoint mpm = mpo.getFactory().createMultiPoint(new Coordinate[] {new Coordinate(10, 12)});
        store.modifyFeatures(original.getFeatureType().getDescriptor("pointProperty"), mpm,
                fidFilter);
        modified = store.getFeatures(fidFilter).features().next();
        assertTrue(mpm.equalsExact((Geometry) modified.getAttribute("pointProperty")));
    }

    /**
     * This test is made with mock objects because the property data store does
     * not generate fids in the <type>.<id> form
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testAppend() throws Exception {
        SimpleFeatureType type = DataUtilities.createType("trees",
                "the_geom:Point,FID:String,NAME:String");

        SimpleFeatureStore fs = createMock(SimpleFeatureStore.class);
        expect(fs.addFeatures(isA(FeatureCollection.class))).andReturn(
                Collections.singletonList((FeatureId)(new FeatureIdImpl("trees.105"))));
        replay(fs);

        DataStore ds = createMock(DataStore.class);
        expect(ds.getTypeNames()).andReturn(new String[] { "trees" }).anyTimes();
        expect(ds.getSchema("trees")).andReturn(type).anyTimes();
        expect(ds.getFeatureSource("trees")).andReturn(fs);
        replay(ds);

        RetypingDataStore rts = new RetypingDataStore(ds) {
            @Override
            protected String transformFeatureTypeName(String originalName) {
                return "oaks";
            }
        };

        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(type);
        WKTReader reader = new WKTReader();
        sfb.set("the_geom", reader.read("POINT (0.002 0.0008)"));
        sfb.set("FID", "023");
        sfb.set("NAME", "Old oak");
        SimpleFeature feature = sfb.buildFeature(null);
        SimpleFeatureCollection fc = DataUtilities.collection(feature);

        SimpleFeatureStore store = (SimpleFeatureStore) rts.getFeatureSource("oaks");
        List<FeatureId> ids = store.addFeatures(fc);
        assertEquals(1, ids.size());
        String id = ((FeatureId) ids.iterator().next()).getID();
        assertTrue("Id does not start with " + "oaks" + " it's " + id, id.startsWith("oaks"));
    }

    public void testLockUnlockFilter() throws Exception {
        SimpleFeatureLocking fl;
        fl = (SimpleFeatureLocking) rts.getFeatureSource(RENAMED);
        final FeatureLock lock = FeatureLockFactory.generate(10 * 60 * 1000);
        Transaction t = new DefaultTransaction();
        t.addAuthorization(lock.getAuthorization());
        fl.setTransaction(t);
        fl.setFeatureLock(lock);

        SimpleFeatureLocking fl2;
        fl2 = (SimpleFeatureLocking) rts.getFeatureSource(RENAMED);
        fl.setFeatureLock(lock);
        fl2.setTransaction(new DefaultTransaction());

        assertEquals(1, fl.lockFeatures(fidFilter));
        assertEquals(0, fl2.lockFeatures(fidFilter));

        fl.unLockFeatures(fidFilter);
        assertEquals(1, fl2.lockFeatures(fidFilter));
    }
    
    public void testLockUnlockQuery() throws Exception {
        SimpleFeatureLocking fl;
        fl = (SimpleFeatureLocking) rts.getFeatureSource(RENAMED);
        final FeatureLock lock = FeatureLockFactory.generate(10 * 60 * 1000);
        Transaction t = new DefaultTransaction();
        t.addAuthorization(lock.getAuthorization());
        fl.setTransaction(t);
        fl.setFeatureLock(lock);

        SimpleFeatureLocking fl2;
        fl2 = (SimpleFeatureLocking) rts.getFeatureSource(RENAMED);
        fl.setFeatureLock(lock);
        fl2.setTransaction(new DefaultTransaction());

        Query q = new Query(RENAMED, fidFilter);
        assertEquals(1, fl.lockFeatures(q));
        assertEquals(0, fl2.lockFeatures(q));

        fl.unLockFeatures(q);
        assertEquals(1, fl2.lockFeatures(q));
    }

}
