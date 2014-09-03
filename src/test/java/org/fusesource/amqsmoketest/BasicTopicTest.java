package org.fusesource.amqsmoketest;

import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by kearls on 06/08/14.
 *
 * Send 100 messages to 5 different topic consumers and ensure they each get all 500.
 */
public class BasicTopicTest {
    protected static String brokerURL = "tcp://localhost:61616";
    protected static String amqUser ="admin";
    protected static String amqPassword ="biteme";
    protected static final String TEST_TOPIC = "aaatest";
    private static final Integer CONSUMER_COUNT = 5;
    private static final Integer MESSAGE_COUNT=100;
    public CountDownLatch totalMessagesReceived = new CountDownLatch(MESSAGE_COUNT * CONSUMER_COUNT);

    @Before
    public void setUp() throws Exception {
        amqUser = System.getProperty("AMQ_USER", amqUser);
        amqPassword = System.getProperty("AMQ_PASSWORD", amqPassword);
        brokerURL = System.getProperty("BROKER_URL", brokerURL);
    }

    @Test
    public void simpleTopicTest() throws Exception {
        TopicProducer topicProducer = new TopicProducer(TEST_TOPIC, MESSAGE_COUNT, brokerURL, amqUser, amqPassword);
        List<AsyncTopicConsumer> consumers = new ArrayList<AsyncTopicConsumer>();
        for (int i=0; i < CONSUMER_COUNT; i++) {
            AsyncTopicConsumer consumer = setUpConsumer(totalMessagesReceived);
            consumers.add(consumer);
        }

        topicProducer.sendAllMessages();
        totalMessagesReceived.await(5, TimeUnit.SECONDS);

        System.out.println("============================================================================");
        System.out.println("    Sent " + topicProducer.sentCount.get() + " messages on topic");
        for (AsyncTopicConsumer consumer : consumers) {
            System.out.println("Received " + consumer.receiveCount.get() + " messages on topic");
            consumer.close();
            assertEquals("Expected " + MESSAGE_COUNT + " messages for consumer", MESSAGE_COUNT.intValue(), consumer.receiveCount.intValue());
        }

        assertEquals(0, totalMessagesReceived.getCount());

        topicProducer.close();
    }


    private static AsyncTopicConsumer setUpConsumer(CountDownLatch totalMessagesReceived) throws JMSException {
        AsyncTopicConsumer consumer = new AsyncTopicConsumer(brokerURL, amqUser, amqPassword, totalMessagesReceived);
        Session session = consumer.getSession();
        Topic testTopic = session.createTopic(TEST_TOPIC);
        MessageConsumer messageConsumer = session.createConsumer(testTopic);
        messageConsumer.setMessageListener(consumer);
        return consumer;
    }
}
