package it.geosolutions.geoserver.jms.test;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.JmsTemplate;

public abstract class JMSCatalogListenerTest {
//    private JmsTemplate jmsTemplate;
//    private Queue queue;
//
//    public void setConnectionFactory(ConnectionFactory cf) {
//        this.jmsTemplate = new JmsTemplate(cf);
//    }
//
//    public void setQueue(Queue queue) {
//        this.queue = queue;
//    }
//
//    public void simpleSend() {
//        this.jmsTemplate.send(this.queue, new MessageCreator() {
//            public Message createMessage(Session session) throws JMSException {
//              return session.createTextMessage("hello queue world");
//            }
//        });
//    }
}
