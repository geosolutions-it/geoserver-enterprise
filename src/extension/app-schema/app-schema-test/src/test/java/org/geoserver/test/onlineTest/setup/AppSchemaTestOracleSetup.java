/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.setup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;
import org.geotools.data.property.PropertyFeatureReader;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.resources.Classes;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.identity.FeatureId;

/**
 * Oracle data setup for app-schema-test with online mode.
 * 
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
@SuppressWarnings("deprecation")
public class AppSchemaTestOracleSetup extends ReferenceDataOracleSetup {

    /**
     * Mapping file database parameters
     */
    public static String DB_PARAMS = "<parameters>" //
        + "\n<Parameter>\n" //
        + "<name>dbtype</name>\n" //
        + "<value>Oracle</value>" //
        + "\n</Parameter>" //
        + "\n<Parameter>\n" //
        + "<name>host</name>\n" //
        + "<value>${host}</value>" //
        + "\n</Parameter>" //
        + "\n<Parameter>\n" //
        + "<name>port</name>\n" //
        + "<value>${port}</value>" //
        + "\n</Parameter>" //
        + "\n<Parameter>\n" //
        + "<name>database</name>\n" //
        + "<value>${database}</value>" //
        + "\n</Parameter>" //
        + "\n<Parameter>\n" //
        + "<name>user</name>\n" //
        + "<value>${user}</value>" //
        + "\n</Parameter>" //
        + "\n<Parameter>\n" //
        + "<name>passwd</name>\n" //
        + "<value>${passwd}</value>" //
        + "\n</Parameter>" //
        + "\n<Parameter>\n"
        + "<name>Expose primary keys</name>"
        + "<value>true</value>"
        + "\n</Parameter>" //
        + "\n</parameters>"; //

    private String sql;
    
    /**
     * Factory method.
     * 
     * @param propertyFiles
     *            Property file name and its parent directory map
     * @return This class instance.
     * @throws Exception
     */
    public static AppSchemaTestOracleSetup getInstance(Map<String, File> propertyFiles) throws Exception {
        return new AppSchemaTestOracleSetup(propertyFiles);
    }

    /**
     * Ensure the app-schema properties file is loaded with the database parameters. Also create
     * corresponding tables on the database based on data from properties files.
     * 
     * @param propertyFiles
     *            Property file name and its feature type directory map
     * @throws Exception
     */
    public AppSchemaTestOracleSetup(Map<String, File> propertyFiles) throws Exception {
        configureFixture();
        createTables(propertyFiles);
    }

    /**
     * Write SQL string to create tables in the test database based on the property files.
     * 
     * @param propertyFiles
     *            Property files from app-schema-test suite.
     * @throws IllegalAttributeException
     * @throws NoSuchElementException
     * @throws IOException
     */
    private void createTables(Map<String, File> propertyFiles) throws IllegalAttributeException,
            NoSuchElementException, IOException {
        StringBuffer buf = new StringBuffer();
        StringBuffer spatialIndex = new StringBuffer();
        // drop table procedure I copied from Victor's Oracle_Data_ref_set.sql
        buf
                .append("CREATE OR REPLACE PROCEDURE DROP_TABLE_OR_VIEW(TabName in Varchar2) IS ")
                .append("temp number:=0;")
                .append(" tes VARCHAR2 (200) := TabName;")
                .append(" drp_stmt VARCHAR2 (200):=null;")
                .append("BEGIN select count(*) into temp from user_tables where TABLE_NAME = tes;")
                .append("if temp = 1 then drp_stmt := 'Drop Table '||tes;")
                .append("EXECUTE IMMEDIATE drp_stmt;")
                 // drop views too
                .append("else select count(*) into temp from user_views where VIEW_NAME = tes;") 
                .append("if temp = 1 then drp_stmt := 'Drop VIEW '||tes;")
                .append("EXECUTE IMMEDIATE drp_stmt;end if;end if;")    
                .append("EXCEPTION WHEN OTHERS THEN ")
                .append(
                        "raise_application_error(-20001,'An error was encountered - '||SQLCODE||' -ERROR- '||SQLERRM);")
                .append("END DROP_TABLE_OR_VIEW;\n"); 

        for (String fileName : propertyFiles.keySet()) {
            PropertyFeatureReader reader = new PropertyFeatureReader(propertyFiles.get(fileName),
            // remove .properties as it will be added in PropertyFeatureReader constructor
                    fileName.substring(0, fileName.lastIndexOf(".properties")));
            SimpleFeatureType schema = reader.getFeatureType();
            String tableName = schema.getName().getLocalPart().toUpperCase();
            // drop table if exists
            buf.append("CALL DROP_TABLE_OR_VIEW('").append(tableName).append("')\n");
            // create the table
            buf.append("CREATE TABLE ").append(tableName).append("(");
            // + id + pkey
            int size = schema.getAttributeCount() + 2;
            String[] fieldNames = new String[size];
            List<String> createParams = new ArrayList<String>();
            int j = 0;
            String type;
            String field;
            int spatialIndexCounter = 0;
            for (PropertyDescriptor desc : schema.getDescriptors()) {
                field = desc.getName().toString().toUpperCase();
                fieldNames[j] = field;
                if (desc instanceof GeometryDescriptor) {
                    type = "SDO_GEOMETRY";
                    // Update spatial index
                    int srid = getSrid(((GeometryType) desc.getType()));
                    
                    spatialIndex.append("DELETE FROM user_sdo_geom_metadata WHERE table_name = '")
                            .append(tableName).append("'\n");
		
                    spatialIndex
                            .append("Insert into user_sdo_geom_metadata ")
                            .append("(TABLE_NAME,COLUMN_NAME,DIMINFO,SRID)")
                            .append("values ('")
                            .append(tableName)
                            .append("','")
                            .append(field)
                            .append(
                                    "',MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',140.962,144.909,0.00001),")
                            .append("MDSYS.SDO_DIM_ELEMENT('Y',-38.858,-33.98,0.00001)),").append(
                                    srid).append(")\n");

                    // ensure it's <= 30 characters to avoid Oracle exception
                    String indexName = (tableName.length() <= 26 ? tableName : tableName.substring(
                            0, 26)) + "_IDX";
                    if (spatialIndexCounter > 0) {
                        // to avoid duplicate index name when there are > 1 geometry in the same table
                        indexName += spatialIndexCounter;
                    }

                    spatialIndex.append("CREATE INDEX \"").append(indexName).append("\" ON \"")
                            .append(tableName).append("\"(\"").append(field).append("\") ").append(
                                    "INDEXTYPE IS \"MDSYS\".\"SPATIAL_INDEX\"\n");
                    spatialIndexCounter++;
                } else {
                    type = Classes.getShortName(desc.getType().getBinding());
                    if (type.equalsIgnoreCase("String")) {
                        type = "NVARCHAR2(250)";
                    } else if (type.equalsIgnoreCase("Double")) {
                        type = "NUMBER";
                    }
                    // etc. assign as required
                }
                createParams.add(field + " " + type);
                j++;
            }
            if (schema.isIdentified()) {  
                // row id .. can't use ID because there are columns with that name in
                // some tables and for oracle everythign has to be upper case so we
                // can't differentiate between id and ID, we we use ROW_ID instead
                fieldNames[j] = "ROW_ID";
                createParams.add("ROW_ID VARCHAR2(30)");
                j++;
            }
            // Add numeric PK for sorting
            fieldNames[j] = "PKEY";
            createParams.add("PKEY NUMBER(5)");
            buf.append(StringUtils.join(createParams.iterator(), ", "));
            buf.append(")\n");
            buf.append("ALTER TABLE " + tableName + " ADD CONSTRAINT " + tableName + " PRIMARY KEY (PKEY)\n");
            // then insert rows
            SimpleFeature feature;
            FeatureId id;
            // primary key sequence
            int pKey = 0;
            while (reader.hasNext()) {
                buf.append("INSERT INTO ").append(tableName).append("(");
                feature = reader.next();
                buf.append(StringUtils.join(fieldNames, ", "));
                buf.append(") ");
                buf.append("VALUES (");
                Collection<Property> properties = feature.getProperties();
                String[] values = new String[size];
                int valueIndex = 0;
                for (Property prop : properties) {
                    Object value = prop.getValue();
                    if (value == null || value.toString().equalsIgnoreCase("null")) {
                        values[valueIndex] = "null";
                    } else if (prop.getType() instanceof GeometryType) {
                        int srid = getSrid(((GeometryType) prop.getType()));                        
                        StringBuffer geomValue = new StringBuffer("SDO_GEOMETRY('");
                        geomValue.append(value).append("'");
                        if (srid > -1) {
                            // attach srid
                            geomValue.append(", ").append(srid);
                        }
                        geomValue.append(")");
                        values[valueIndex] = geomValue.toString();
                    } else if (prop.getType().getBinding().getSimpleName().equalsIgnoreCase("DATE")) {
                        values[valueIndex] = "TO_DATE('" + value + "', 'yyyy-MM-dd')";
                    } else {
                        values[valueIndex] = "'" + value + "'";
                    }
                    valueIndex++;
                }
                id = feature.getIdentifier();
                if (id != null) {
                    values[valueIndex] = "'" + id.toString() + "'";
                    valueIndex++;
                }
                // insert primary key
                values[valueIndex] = "'" + pKey + "'";
                pKey++;
                buf.append(StringUtils.join(values, ","));
                buf.append(")\n");
            }
        }
        buf.append(spatialIndex.toString());
        if (buf.length() > 0) {
            this.sql = buf.toString();
        }
    }

    @Override
    protected void runSqlInsertScript() throws Exception {
        System.out.println(sql);
        this.run(sql, false);
    }
}
