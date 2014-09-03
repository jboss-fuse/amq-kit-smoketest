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
public class AsyncConsumer implements MessageListener {
    protected static final Logger LOG = LoggerFactory.getLogger(AsyncConsumer.class);

    protected static String targetQueueName;

    private static transient ConnectionFactory factory;
    private transient Connection connection;
    private transient Session session;

    private String jobs[];
    public AtomicInteger suspendCount = new AtomicInteger(0);
    public AtomicInteger deleteCount = new AtomicInteger(0);
    public CountDownLatch receivedCount;

    public AsyncConsumer(String[] jobs, int receivedCount, String targetQueueName, String brokerURL, String amqUser, String amqPassword) throws JMSException {
        this.jobs = jobs;
        this.targetQueueName = targetQueueName;

        LOG.info("Trying to connect to broker: " + brokerURL);
        factory = new ActiveMQConnectionFactory(brokerURL);
        connection = factory.createConnection(amqUser, amqPassword);
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        this.receivedCount = new CountDownLatch(receivedCount);

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

            if (destination.toString().equals(targetQueueName)) {
                suspendCount.incrementAndGet();
            } else {
                deleteCount.incrementAndGet();
            }
            receivedCount.countDown();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
