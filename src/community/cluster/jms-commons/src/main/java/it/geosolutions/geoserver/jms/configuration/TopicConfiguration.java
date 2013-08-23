/* Copyright (c) 2011 GeoSolutions - http://www.geo-solutions.it/.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package it.geosolutions.geoserver.jms.configuration;

import java.io.IOException;

/**
 * 
 * class to store and load configuration from global var or properties file
 * 
 * @author carlo cancellieri - GeoSolutions SAS
 * 
 */
final public class TopicConfiguration implements JMSConfigurationExt {

    public static final String TOPIC_NAME_KEY = "topicName";

    public static final String DEFAULT_TOPIC_NAME = "VirtualTopic.>";

//    @Autowired
//    JMSPropertyPlaceholderConfigurer commonConfiguration;

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        String url = null;

//        if (commonConfiguration != null) {
//            url = commonConfiguration.getMergedProperties().getProperty(TOPIC_NAME_KEY);
//        }

        config.putConfiguration(TOPIC_NAME_KEY, url != null ? url : DEFAULT_TOPIC_NAME);
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(TOPIC_NAME_KEY, DEFAULT_TOPIC_NAME);
    }

}
