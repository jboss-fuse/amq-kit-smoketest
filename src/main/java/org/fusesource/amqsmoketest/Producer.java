package org.fusesource.amqsmoketest;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by kearls on 06/08/14.
 */
public class Producer {
    protected static final Logger LOG = LoggerFactory.getLogger(Producer.class);

    protected String targetQueueName;
    protected String brokerURL;
    protected String amqUser;
    protected String amqPassword;

    private static final int SLEEP_INTERVAL = 10;
    private static final int MESSAGES_PER_INTERVAL = 50;

    private transient ConnectionFactory factory;
    private transient Connection connection;
    private transient Session session;
    private transient MessageProducer producer;
    private int total;
    private int totalMessages = 1000;
    private AtomicInteger id = new AtomicInteger(1000000);

    private String jobs[];

    public AtomicInteger suspendCount = new AtomicInteger(0);
    public AtomicInteger deleteCount = new AtomicInteger(0);

    public Producer(Integer numberOfMessages, String[] jobs, String targetQueueName, String brokerUrl, String user, String password) throws JMSException {
        this.brokerURL = brokerUrl;
        this.amqUser = user;
        this.amqPassword = password;
        this.targetQueueName = targetQueueName;
        this.jobs = jobs;

        this.totalMessages = numberOfMessages;

        factory = new ActiveMQConnectionFactory(brokerURL);
        connection = factory.createConnection(amqUser, amqPassword);
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = session.createProducer(null);
    }

    public void close() throws JMSException {
        if (connection != null) {
            connection.close();
        }
    }

    public void sendMessage() throws JMSException {
        String job = jobs[id.get() % jobs.length];
        Destination destination = session.createQueue("JOBS." + job);

        if (destination.toString().equals(targetQueueName)) {
            suspendCount.incrementAndGet();
        } else {
            deleteCount.incrementAndGet();
        }

        id.incrementAndGet();
        Message message = session.createTextMessage(id.toString());
        LOG.info("Sending: id: " + id.toString() + " on queue: " + destination);
        producer.send(destination, message);
    }


    /**
     * @throws javax.jms.JMSException
     */
    public void sendAllMessages() throws JMSException {
        for (int i = 0; i < totalMessages; i++) {
            sendMessage();
            if (i % MESSAGES_PER_INTERVAL == 0) {
                try {
                    Thread.sleep(SLEEP_INTERVAL);
                } catch (InterruptedException x) {
                }
            }
        }

        producer.close();
    }
}
