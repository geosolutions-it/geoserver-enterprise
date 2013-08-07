package it.geosolutions.geoserver.jms.impl;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import it.geosolutions.geoserver.jms.JMSFactory;
import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;
import it.geosolutions.geoserver.jms.impl.configuration.BrokerConfiguration;
import it.geosolutions.geoserver.jms.impl.configuration.TopicConfiguration;

public class JMSActiveMQFactory extends JMSFactory {

	// <bean id="JMSClientDestination"
	// class="org.apache.activemq.command.ActiveMQQueue">
	// <value="Consumer.${instance.name}.VirtualTopic.${topic.name}" />
	// </bean>
	@Override
	public Destination getClientDestination(Properties configuration) {
		StringBuilder builder=new StringBuilder("Consumer.");
		String instanceName=configuration.getProperty(JMSConfiguration.INSTANCE_NAME_KEY);
		String topicName=configuration.getProperty(TopicConfiguration.TOPIC_NAME_KEY);
		return new org.apache.activemq.command.ActiveMQQueue(builder.append(instanceName).append(".").append(topicName).toString());
	}

//	<!-- DESTINATION -->
//	<!-- A Destination in ActiveMQ -->
//	<bean id="JMSServerDestination" class="org.apache.activemq.command.ActiveMQTopic">
//<!-- 		<constructor-arg value="VirtualTopic.${topic.name}" /> -->
//		<constructor-arg value="VirtualTopic.>" />
//	</bean>
	@Override
	public Destination getServerDestination(Properties configuration) {
		return new org.apache.activemq.command.ActiveMQTopic(configuration.getProperty(TopicConfiguration.TOPIC_NAME_KEY));
	}
	
//	<!-- A connection to ActiveMQ -->
//	<bean id="JMSConnectionFactory" class="org.apache.activemq.ActiveMQConnectionFactory">
//		<!-- <property name="brokerURL" value="${broker.url}" /> -->
//		<property name="brokerURL" value="tcp://localhost:61616" />
//	</bean>
	@Override
	public ConnectionFactory getConnectionFactory(Properties configuration) {
		return new org.apache.activemq.ActiveMQConnectionFactory(configuration.getProperty(BrokerConfiguration.BROKER_URL_KEY));
	}

}
