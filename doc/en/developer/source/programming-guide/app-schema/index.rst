.. _app-schema_online_tests:

App-Schema Online Tests
=======================

The offline tests in app-schema-test suite use properties files as data source. In reality, properties files are only used as testing means, whereas in production, users would use databases as data source. Users would often encounter problems/bugs that cannot be recreated using properties files, which raises the need to run with a test database. Moreover, Niels' joining support to increase performance can only be tested online, and we need to ensure that it works with the current features/bug fixes covered in the tests. 

Prerequisites
-------------

This requires installation of Oracle driver in Maven repository::       
                                  
    mvn install:install-file -Dfile=ojdbc14.jar -DgroupId=com.oracle -DartifactId=ojdbc14 -Dversion=10.2.0.3.0 -Dpackaging=jar 

You would also need to have test databases for both Oracle and Postgis. Then follow these steps:

* Create oracle.properties and postgis.properties in {user directory}/.geoserver directory.

* Populate each properties file with database details, e.g.::

    password=onlinetestuser

    passwd=onlinetestuser

    user=onlinetestuser

    port=5432

    url=jdbc\:postgresql\://localhost:5432/onlinetest

    host=localhost

    database=onlinetest

    driver=org.postgresql.Driver

    dbtype=postgisng 

Running tests from Maven
------------------------

Without specifying any profile, the default Maven configuration for app-schema-test is to run offline tests only. 

To run online tests, enable the profile::

    -Papp-schema-online-test 

This profile enables the data reference set tests and offline tests to run online. Data reference set tests are online tests based on data and use cases from GeoScience Victoria. Each is explicit for a database type (Oracle and Postgis) and has a copy to run with joining enabled. 

The offline tests are configured to run online with joining through separate modules for each database: app-schema-oracle-test and app-schema-postgis-test. These modules are placeholders for pom.xml files containing database specific parameters. This makes it easy to identify when a test fails with a particular database when running from Maven/buildbot. 

Memory requirements
```````````````````

The online tests require more memory than usual, so specifying the usual -Dtest.maxHeapSize=256m is not enough. Specify --Dtest.maxHeapSize=1024m instead.

When the build is successful, you would see this in the "Reactor Summary"::

    [INFO] Application Schema Integration Online Test with Oracle Database  SUCCESS  [5:52.980s]
    [INFO] Application Schema Integration Online Test with Postgis Database  SUCCESS  [1:42.428s]

Running tests from JUnit
------------------------

There is no need to import the online test modules as they are empty and you cannot run the tests through them in Eclipse.

To run offline tests (in app-schema-test/src/test/java/org/geoserver/test) with a test database, 
enable joining and specify the database. Add these parameters in VM Arguments for postgis::

    -Dapp-schema.joining=true -DtestDatabase=postgis -Xmx256m 

Similarly, to test with oracle::

    -Dapp-schema.joining=true -DtestDatabase=oracle -Xmx256m 

Additionally for Oracle, you also need to add ojdbc14.jar in the test Classpath. 

.. note:: Please note that you should only run the tests in org.geoserver.test package with the above parameters, since the data reference tests in org.geoserver.test.onlineTest package contain non-joining tests which would fail.   

You do not need to specify these VM Arguments for running data reference tests (in app-schema-test/src/test/java/org/geoserver/test/onlineTest). However, you would still need to specify the Oracle JDBC driver in the Classpath for Oracle specific tests. Data reference tests package also requires 768m memory to run from JUnit. 

Adding new tests
----------------

When adding new tests to app-schema-test suite (except for onlineTest package for data reference tests), please note the following:

Test offline only
`````````````````

If your test is a special case and does not need to be tested online, exclude them in both app-schema-oracle-test and app-schema-postgis-test pom.xml and ignore the points beyond this. Otherwise, read on. 

idExpression
````````````

If your test database does not use primary keys, ensure idExpression is specified for the top level element in your mapping file.

Multi-valued properties ordering 
````````````````````````````````

When testing multi-valued properties, the order of the values could vary depending on the data source type. To be safe, compare your values as a list, instead of evaluating individual xpath node against a single value for such properties. E.g.::

        List<String> names = new ArrayList<String>();
        names.add("New Group");
        names.add("-Xy");
        String name = evaluate("//gsml:MappedFeature[@gml:id='" + id
                + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name = evaluate("//gsml:MappedFeature[@gml:id='" + id
                + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

This is because of the difference in the handling of queries with joining. Joining uses order by when querying tables. When the tests run offline, property data store returns data from properties file unordered.

When joining is enabled:

* If the multi-valued properties are not feature chained, the order is unpredictable.

* If the multi-valued properties are feature chained, they are ordered by the foreign key used in feature chaining.

Column names in upper case
``````````````````````````

Ensure column names in mapping files are in upper case, even if they are in lower case in the properties file. This is to avoid failures with Oracle database, due to OracleDialect not wrapping names with escape characters. To work around this, the script for online tests creates the columns in upper case, therefore should be referred by with upper case. 

Functions in feature chaining
`````````````````````````````

If using feature chaining, avoid using functions in sourceExpression for linking attributes, i.e. attribute used in both OCQL and linkField. This is because functions used in feature chaining are not supported with joining support. 

WMS tests
`````````
If you are testing Application Schema WMS support behaviour, it is highly recommended to also perform the optional perceptual diff tests, 
which are included in both online as well as offline unit tests.
Perceptual diff tests for app-schema WMS support will only be performed if::
      
	-Dorg.geotools.image.test.enabled=true

and `Perceptual Diff
<http://pdiff.sourceforge.net/>`_ is installed on the computer from which the tests are executed.




