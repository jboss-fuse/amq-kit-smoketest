package org.fusesource.amqsmoketest;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kearls on 06/08/14.
 */
public class TopicProducer {
    protected static final Logger LOG = LoggerFactory.getLogger(TopicProducer.class);
    protected String brokerURL;
    protected String amqUser;
    protected String amqPassword;

    private static final int SLEEP_INTERVAL = 50;
    private int MAX_TOTAL_MESSAGES = 1000;
    private static final int MESSAGES_PER_INTERVAL = 100;
    private static transient ConnectionFactory factory;
    private transient Connection connection;
    private transient Session session;
    private static int total;
    private AtomicInteger id = new AtomicInteger(0);
    private static String testTopicName = "aaatest";

    public AtomicInteger sentCount = new AtomicInteger(0);

    public TopicProducer(String testTopicName, Integer numberOfMessages, String brokerUrl, String user, String password) throws JMSException {
        this.brokerURL = brokerUrl;
        this.amqUser = user;
        this.amqPassword = password;
        this.testTopicName = testTopicName;
        this.MAX_TOTAL_MESSAGES = numberOfMessages;

        factory = new ActiveMQConnectionFactory(brokerURL);
        connection = factory.createConnection(amqUser, amqPassword);
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    public void close() throws JMSException {
        if (connection != null) {
            connection.close();
        }
    }

    public void sendMessage() throws JMSException {
        Topic testTopic = session.createTopic(testTopicName);
        MessageProducer producer = session.createProducer(testTopic);
        id.incrementAndGet();
        TextMessage message = session.createTextMessage(id.toString());
        LOG.info("Sending: id: " + id.toString());
        producer.send(testTopic, message);
        sentCount.incrementAndGet();
    }

    /**
     * @throws javax.jms.JMSException
     */
    public void sendAllMessages() throws JMSException {
        for (int i = 0; i < MAX_TOTAL_MESSAGES; i++) {
            sendMessage();
            if (i % MESSAGES_PER_INTERVAL == 0) {
                System.out.println("Sent '" + MESSAGES_PER_INTERVAL + "' of '" + MAX_TOTAL_MESSAGES + "' job messages");
                try {
                    Thread.sleep(SLEEP_INTERVAL);
                } catch (InterruptedException x) {
                }
            }
        }
    }
}
