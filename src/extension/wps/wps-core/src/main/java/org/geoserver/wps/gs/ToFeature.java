/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.geoserver.wps.ppio.FeatureAttribute;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * 
 * @author Alessio Fabiani - GeoSolutions
 * 
 */
@DescribeProcess(title = "toFeature", description = "Transforms a map of KVP into a Feature.")
public class ToFeature implements GSProcess {

	@DescribeResult(name = "result", description = "The Simple Feature representing the Map of KVP")
	public SimpleFeatureCollection execute(
			@DescribeParameter(name = "geometry", description = "The feature geometry", min = 1) Geometry geometry,
			@DescribeParameter(name = "crs", description = "The geometry CRS (if not already available)") CoordinateReferenceSystem crs,
			@DescribeParameter(name = "typeName", description = "The generated feature type name", min = 1) String typeName,
			@DescribeParameter(name = "attribute", description = "attribute KVP (name,value)", collectionType = FeatureAttribute.class, min = 1) List attribute,
			ProgressListener progressListener)
	throws ProcessException {
		
		// get the crs
		if (crs == null) {
			try {
				crs = (CoordinateReferenceSystem) geometry.getUserData();
			} catch (Exception e) {
				// may not have a CRS attached
			}
		}
		if (crs == null && geometry.getSRID() > 0) {
			try {
				crs = CRS.decode("EPSG:" + geometry.getSRID());
			} catch (Exception e) {
				// unable to find the CRS
				throw new ProcessException(e);
			}
		}

		// build the feature type
		List<Object> values = new LinkedList<Object>();
		SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
		tb.setName(typeName);
		tb.add("the_geom", geometry.getClass(), crs);
		values.add(geometry);

		for (Object att : attribute) {
			if (att instanceof FeatureAttribute) {
				tb.add((String) ((FeatureAttribute) att).getName(),
						((FeatureAttribute) att).getValue().getClass());
				values.add(((FeatureAttribute) att).getValue());
			} else if (att instanceof HashMap) {
				HashMap<String, Object> kvpMap = (HashMap<String, Object>) att;
					tb.add((String) kvpMap.get("name"), kvpMap.get("value").getClass());
					values.add(kvpMap.get("value"));
			}
		}

		SimpleFeatureType schema = tb.buildFeatureType();

		// build the feature
		SimpleFeature sf = SimpleFeatureBuilder.build(schema, values, null);
		ListFeatureCollection result = new ListFeatureCollection(schema);
		result.add(sf);
		return result;
	}

}