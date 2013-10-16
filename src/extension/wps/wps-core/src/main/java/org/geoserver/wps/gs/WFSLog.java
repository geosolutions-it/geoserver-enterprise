/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.WPSException;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCFeatureSource;
import org.geotools.jdbc.JDBCFeatureStore;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * 
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
@DescribeProcess(title = "wfsLog", description = "Insert or updates features attributes on a datastore.")
public class WFSLog implements GSProcess {

	static final Logger LOGGER = Logging.getLogger(WFSLog.class);

	private GeoServer geoServer;

	private Catalog catalog;

	public WFSLog(GeoServer geoServer) {
		this.geoServer = geoServer;
	}

	@DescribeResult(name = "result", description = "The Simple Feature representing the Map of KVP")
	public SimpleFeatureCollection execute(
			@DescribeParameter(name = "input feature collection", description = "The feature collection to be updated", min = 1) SimpleFeatureCollection features,
			@DescribeParameter(name = "typeName", description = "The feature type name", min = 1) String typeName,
			@DescribeParameter(name = "workspace", description = "The workSpace (must exist in the catalog)", min = 1) String workspace,
			@DescribeParameter(name = "store", description = "The dataStore (must exist in the catalog)", min = 1) String store,
			@DescribeParameter(name = "filter", description = "The update filter", min = 0) Filter filter,
			@DescribeParameter(name = "filter", description = "Force insert row", min = 0) Boolean forceInsert,
			ProgressListener progressListener) throws ProcessException {

		catalog = geoServer.getCatalog();

		// first off, decide what is the target store
		WorkspaceInfo ws;
		if (workspace != null) {
			ws = catalog.getWorkspaceByName(workspace);
			if (ws == null) {
				throw new ProcessException("Could not find workspace "
						+ workspace);
			}
		} else {
			ws = catalog.getDefaultWorkspace();
			if (ws == null) {
				throw new ProcessException(
						"The catalog is empty, could not find a default workspace");
			}
		}

		// ok, find the target store
		DataStoreInfo storeInfo;
		if (store != null) {
			storeInfo = catalog.getDataStoreByName(ws.getName(), store);
			if (storeInfo == null) {
				throw new ProcessException("Could not find store " + store
						+ " in workspace " + workspace);
				// TODO: support store creation
			}
		} else {
			storeInfo = catalog.getDefaultDataStore(ws);
			if (storeInfo == null) {
				throw new ProcessException(
						"Could not find a default store in workspace "
								+ ws.getName());
			}
		}

		String tentativeTargetName = null;
		if (typeName != null) {
			tentativeTargetName = ws.getName() + ":" + typeName;
		} else {
			tentativeTargetName = ws.getName() + ":"
					+ features.getSchema().getTypeName();
		}
		boolean createNew;
		if (catalog.getLayerByName(tentativeTargetName) == null) {
			createNew = true;
		} else {
			createNew = false;
		}

		// check the target crs
		String targetSRSCode = null;
		// check we can extract a code from the original data
		GeometryDescriptor gd = features.getSchema().getGeometryDescriptor();
		ProjectionPolicy srsHandling = null;
		if (gd == null) {
			// data is geometryless, we need a fake SRS
			targetSRSCode = "EPSG:4326";
			srsHandling = ProjectionPolicy.FORCE_DECLARED;
		} else {
			CoordinateReferenceSystem nativeCrs = gd.getCoordinateReferenceSystem();
			if (nativeCrs == null) {
				//throw new ProcessException("The original data has no native CRS, you need to specify the srs parameter");
				targetSRSCode = "EPSG:4326";
				srsHandling = ProjectionPolicy.FORCE_DECLARED;
			} else {
				try {
					Integer code = CRS.lookupEpsgCode(nativeCrs, true);
					if (code == null) {
						throw new ProcessException(
								"Could not find an EPSG code for data "
										+ "native spatial reference system: "
										+ nativeCrs);
					} else {
						targetSRSCode = "EPSG:" + code;
					}
				} catch (Exception e) {
					throw new ProcessException(
							"Failed to loookup an official EPSG code for "
									+ "the source data native "
									+ "spatial reference system", e);
				}
			}
		}

		SimpleFeatureCollection targetFeatures;
		if (createNew) {
			// import the data into the target store
			try {
				targetFeatures = importDataIntoStore(features, typeName, storeInfo, filter);
			} catch (IOException e) {
				throw new ProcessException(
						"Failed to import data into the target store", e);
			}

			// now import the newly created layer into GeoServer
			try {
				CatalogBuilder cb = new CatalogBuilder(catalog);
				cb.setStore(storeInfo);

				// build the typeInfo and set CRS if necessary
				FeatureTypeInfo typeInfo = cb.buildFeatureType(targetFeatures.getSchema()
						.getName());
				if (targetSRSCode != null) {
					typeInfo.setSRS(targetSRSCode);
				}
				if (srsHandling != null) {
					typeInfo.setProjectionPolicy(srsHandling);
				}
				// compute the bounds
				cb.setupBounds(typeInfo);

				// build the layer and set a style
				LayerInfo layerInfo = cb.buildLayer(typeInfo);

				catalog.add(typeInfo);
				catalog.add(layerInfo);
			} catch (Exception e) {
				throw new ProcessException(
						"Failed to complete the import inside the GeoServer catalog",
						e);
			}
		} else {
			if (filter == null) {
				throw new ProcessException("On update mode the filter is mandatory.");
			}
			
			// update the data into the target store
			try {
				targetFeatures = updateDataStore(features, typeName, storeInfo, filter, forceInsert);
			} catch (IOException e) {
				throw new ProcessException(
						"Failed to import data into the target store", e);
			}
		}

		return targetFeatures;
	}

	private SimpleFeatureCollection updateDataStore(SimpleFeatureCollection features,
			String name, DataStoreInfo storeInfo, Filter filter, boolean forceInsert) throws IOException,
			ProcessException {
		SimpleFeatureCollection result = null;
		SimpleFeatureType targetType;
		// grab the data store
		DataStore ds = (DataStore) storeInfo.getDataStore(null);

		// decide on the target ft name
		SimpleFeatureType sourceType = features.getSchema();
		if (name != null) {
			SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
			tb.init(sourceType);
			tb.setName(name);
			sourceType = tb.buildFeatureType();
		}

		// try to get the target feature type (might have slightly different
		// name and structure)
		targetType = ds.getSchema(sourceType.getTypeName());
		if (targetType == null) {
			// ouch, the name was changed... we can only guess now...
			// try with the typical Oracle mangling
			targetType = ds.getSchema(sourceType.getTypeName().toUpperCase());
		}

		if (targetType == null) {
			throw new WPSException(
					"The target schema was created, but with a name "
							+ "that we cannot relate to the one we provided the data store. Cannot proceeed further");
		}

		// try to establish a mapping with old and new attributes. This is again
		// just guesswork until we have a geotools api that will give us the
		// exact mapping to be performed
		Map<String, String> mapping = buildAttributeMapping(sourceType,
				targetType);

		// start a transaction and fill the target with the input features
		Transaction t = new DefaultTransaction();
		SimpleFeatureStore fstore = (SimpleFeatureStore) ds
				.getFeatureSource(targetType.getTypeName());
		fstore.setTransaction(t);
		SimpleFeatureIterator fi = features.subCollection(filter).features();
		
		LOGGER.info(" - WFS Logger : Filtered features# " + fi.hasNext());
		
		if (!forceInsert)
		{
			String[] names = new String[mapping.entrySet().size()];
	        Object[] values = new Object[mapping.entrySet().size()];
			while (fi.hasNext()) {
				SimpleFeature source = fi.next();
				int j=0;
				for (String sname : mapping.keySet()) {
					names[j]=mapping.get(sname);
					values[j]=source.getAttribute(sname);
					j++;
				}
				fstore.modifyFeatures(names, values, filter);
				
				result = new ListFeatureCollection(targetType);
				((ListFeatureCollection)result).add(source);
			}
		}
		else
		{
			SimpleFeatureSource fSource = ds.getFeatureSource(targetType.getTypeName());
			
			if (fSource instanceof SimpleFeatureStore)
			{
				SimpleFeatureStore simpleFstore = (SimpleFeatureStore) fSource;
				simpleFstore.setTransaction(t);
				fi = features.features();
				SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
				while (fi.hasNext()) {
					SimpleFeature source = fi.next();
					fb.reset();
					for (String sname : mapping.keySet()) {
						fb.set(mapping.get(sname), source.getAttribute(sname));
					}
					SimpleFeature target = fb.buildFeature(null);
					simpleFstore.addFeatures(DataUtilities.collection(target));

					result = new ListFeatureCollection(targetType);
					((ListFeatureCollection)result).add(source);
				}
			}

			else if (fSource instanceof JDBCFeatureStore)
			{
				JDBCFeatureStore jdbcFstore = (JDBCFeatureStore) fSource;
				jdbcFstore.setTransaction(t);
				fi = features.features();
				SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
				while (fi.hasNext()) {
					SimpleFeature source = fi.next();
					fb.reset();
					for (String sname : mapping.keySet()) {
						fb.set(mapping.get(sname), source.getAttribute(sname));
					}
					SimpleFeature target = fb.buildFeature(null);
					jdbcFstore.addFeatures(DataUtilities.collection(target));

					result = new ListFeatureCollection(targetType);
					((ListFeatureCollection)result).add(source);
				}
			}
		}
		t.commit();		
		t.close();

		return result;
	}

	private SimpleFeatureCollection importDataIntoStore(
			SimpleFeatureCollection features, String name,
			DataStoreInfo storeInfo, Filter filter) throws IOException, ProcessException {
		SimpleFeatureCollection result = null;
		SimpleFeatureType targetType;
		// grab the data store
		DataStore ds = (DataStore) storeInfo.getDataStore(null);

		// decide on the target ft name
		SimpleFeatureType sourceType = features.getSchema();
		if (name != null) {
			SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
			tb.init(sourceType);
			tb.setName(name);
			sourceType = tb.buildFeatureType();
		}

		// create the schema
		ds.createSchema(sourceType);

		// try to get the target feature type (might have slightly different
		// name and structure)
		targetType = ds.getSchema(sourceType.getTypeName());
		if (targetType == null) {
			// ouch, the name was changed... we can only guess now...
			// try with the typical Oracle mangling
			targetType = ds.getSchema(sourceType.getTypeName().toUpperCase());
		}

		if (targetType == null) {
			throw new WPSException(
					"The target schema was created, but with a name "
							+ "that we cannot relate to the one we provided the data store. Cannot proceeed further");
		} else {
			// check the layer is not already there
			String newLayerName = storeInfo.getWorkspace().getName() + ":"
					+ targetType.getTypeName();
			LayerInfo layer = catalog.getLayerByName(newLayerName);
			// todo: we should not really reach here and know beforehand what
			// the targetType
			// name is, but if we do we should at least get a way to drop it
			if (layer != null) {
				throw new ProcessException("Target layer " + newLayerName
						+ " already exists in the catalog");
			}
		}

		// try to establish a mapping with old and new attributes. This is again
		// just guesswork until we have a geotools api that will give us the
		// exact mapping to be performed
		Map<String, String> mapping = buildAttributeMapping(sourceType,
				targetType);

		// start a transaction and fill the target with the input features
		Transaction t = new DefaultTransaction();
		
		SimpleFeatureSource fSource = ds.getFeatureSource(targetType.getTypeName());
		
		if (fSource instanceof SimpleFeatureStore)
		{
			SimpleFeatureStore simpleFstore = (SimpleFeatureStore) fSource;
			simpleFstore.setTransaction(t);
			SimpleFeatureIterator fi = features.features();
			SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
			while (fi.hasNext()) {
				SimpleFeature source = fi.next();
				fb.reset();
				for (String sname : mapping.keySet()) {
					fb.set(mapping.get(sname), source.getAttribute(sname));
				}
				SimpleFeature target = fb.buildFeature(null);
				simpleFstore.addFeatures(DataUtilities.collection(target));

				result = new ListFeatureCollection(targetType);
				((ListFeatureCollection)result).add(source);
			}
		}

		else if (fSource instanceof JDBCFeatureStore)
		{
			JDBCFeatureStore jdbcFstore = (JDBCFeatureStore) fSource;
			jdbcFstore.setTransaction(t);
			SimpleFeatureIterator fi = features.features();
			SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetType);
			while (fi.hasNext()) {
				SimpleFeature source = fi.next();
				fb.reset();
				for (String sname : mapping.keySet()) {
					fb.set(mapping.get(sname), source.getAttribute(sname));
				}
				SimpleFeature target = fb.buildFeature(null);
				jdbcFstore.addFeatures(DataUtilities.collection(target));

				result = new ListFeatureCollection(targetType);
				((ListFeatureCollection)result).add(source);
			}
		}
		
		else if (fSource instanceof JDBCFeatureSource)
		{
			JDBCFeatureSource jdbcFsource = (JDBCFeatureSource) fSource;
			FeatureWriter<SimpleFeatureType, SimpleFeature> ftWriter = jdbcFsource.getDataStore().getFeatureWriter(targetType.getTypeName(), t);
			jdbcFsource.setTransaction(t);
			SimpleFeatureIterator fi = features.features();
			while (fi.hasNext()) {
				SimpleFeature source = fi.next();
				SimpleFeature target = ftWriter.next();

				for (String sname : mapping.keySet()) {
					target.setAttribute(sname, source.getAttribute(sname));
				}
				ftWriter.write();
				result = new ListFeatureCollection(targetType);
				((ListFeatureCollection)result).add(source);
			}
		}

		t.commit();		
		t.close();
		
		return result;
	}

	/**
	 * Applies a set of heuristics to find which target attribute corresponds to
	 * a certain input attribute
	 * 
	 * @param sourceType
	 * @param targetType
	 * @return
	 */
	Map<String, String> buildAttributeMapping(SimpleFeatureType sourceType,
			SimpleFeatureType targetType) {
		// look for the typical manglings. For example, if the target is a
		// shapefile store it will move the geometry and name it the_geom

		// collect the source names
		Set<String> sourceNames = new HashSet<String>();
		for (AttributeDescriptor sd : sourceType.getAttributeDescriptors()) {
			sourceNames.add(sd.getLocalName());
		}

		// first check if we have been kissed by sheer luck and the names are
		// the same
		Map<String, String> result = new HashMap<String, String>();
		for (String name : sourceNames) {
			if (targetType.getDescriptor(name) != null) {
				result.put(name, name);
			}
		}
		sourceNames.removeAll(result.keySet());

		// then check for simple case difference (Oracle case)
		for (String name : sourceNames) {
			for (AttributeDescriptor td : targetType.getAttributeDescriptors()) {
				if (td.getLocalName().equalsIgnoreCase(name)) {
					result.put(name, td.getLocalName());
					break;
				}
			}
		}
		sourceNames.removeAll(result.keySet());

		// then check attribute names being cut (another Oracle case)
		for (String name : sourceNames) {
			String loName = name.toLowerCase();
			for (AttributeDescriptor td : targetType.getAttributeDescriptors()) {
				String tdName = td.getLocalName().toLowerCase();
				if (loName.startsWith(tdName)) {
					result.put(name, td.getLocalName());
					break;
				}
			}
		}
		sourceNames.removeAll(result.keySet());

		// consider the shapefile geometry descriptor mangling
		if (targetType.getGeometryDescriptor() != null
				&& "the_geom".equals(targetType.getGeometryDescriptor()
						.getLocalName())
				&& !"the_geom".equalsIgnoreCase(sourceType
						.getGeometryDescriptor().getLocalName())) {
			result.put(sourceType.getGeometryDescriptor().getLocalName(),
					"the_geom");
		}

		// and finally we return with as much as we can match
		if (!sourceNames.isEmpty()) {
			LOGGER.warning("Could not match the following attributes "
					+ sourceNames + " to the target feature type ones: "
					+ targetType);
		}
		return result;
	}

}