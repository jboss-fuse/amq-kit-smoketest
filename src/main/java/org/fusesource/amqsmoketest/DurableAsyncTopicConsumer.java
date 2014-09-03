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
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kearls on 06/08/14.
 */
public class DurableAsyncTopicConsumer implements MessageListener {
    protected static final Logger LOG = LoggerFactory.getLogger(DurableAsyncTopicConsumer.class);
    public AtomicInteger receiveCount = new AtomicInteger(0);
    private Session session;
    private Connection connection;
    private CountDownLatch expectedMessageCount;
    private String clientId;
    private String topicName;

    public DurableAsyncTopicConsumer(String clientId, String topicName, String brokerURL, String amqUser, String amqPassword, int expectedMessageCount) throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerURL);
        connection = factory.createConnection(amqUser, amqPassword);
        connection.setClientID(clientId);
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        this.expectedMessageCount = new CountDownLatch(expectedMessageCount);
        this.clientId = clientId;
        this.topicName = topicName;
    }

    public void close() throws Exception {
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    public void onMessage(Message messageArgument)  {
        try {
            TextMessage message = (TextMessage) messageArgument;
            Destination destination = message.getJMSDestination();
            String id = message.getText();
            LOG.info("Message received: " + id + " from queue " + destination);
            receiveCount.incrementAndGet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Session getSession() {
        return session;
    }

    /**
     * @throws javax.jms.JMSException
     */
    public int go() throws Exception {
        Topic testTopic = session.createTopic(topicName);
        TopicSubscriber subscriber = session.createDurableSubscriber(testTopic, clientId);
        subscriber.setMessageListener(this);

        expectedMessageCount.await(5, TimeUnit.SECONDS);

        return receiveCount.get();
    }
}
