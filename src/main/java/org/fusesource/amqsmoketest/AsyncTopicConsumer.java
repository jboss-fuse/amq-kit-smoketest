package org.fusesource.amqsmoketest;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kearls on 06/08/14.
 */
public class AsyncTopicConsumer implements MessageListener {
    protected static final Logger LOG = LoggerFactory.getLogger(AsyncTopicConsumer.class);
    protected static transient ConnectionFactory factory;
    protected transient Connection connection;
    protected transient Session session;
    public AtomicInteger receiveCount = new AtomicInteger(0);
    public CountDownLatch totalReceivedCount;   // TODO rename

    public AsyncTopicConsumer(String brokerURL, String amqUser, String amqPassword, CountDownLatch totalReceivedCount) throws JMSException {
        factory = new ActiveMQConnectionFactory(brokerURL);
        connection = factory.createConnection(amqUser, amqPassword);
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.totalReceivedCount = totalReceivedCount;
    }

    public Session getSession() {
        return session;
    }

    public void close() throws JMSException {
        if (connection != null) {
            connection.close();
        }
        if (session != null) {
            session.close();
        }
    }

    public void onMessage(Message messageArgument) {
        try {
            TextMessage message = (TextMessage) messageArgument;
            Destination destination = message.getJMSDestination();
            String id = message.getText();
            LOG.info("Message received: " + id + " from queue " + destination);
            receiveCount.incrementAndGet();
            totalReceivedCount.countDown();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
