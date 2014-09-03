package org.fusesource.amqsmoketest;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by kearls on 06/08/14.
 *
 * Create a consumer, create a durable subscription to a topic, and shutdown.  Create a producer to send messages on
 * that topic, and then recreate the consumer.  Make sure the consumer receives all of the messages sent when
 * they were offline.
 */
public class DurableConsumerTest {
    protected static String brokerURL = "tcp://localhost:61616";
    protected static String amqUser ="admin";
    protected static String amqPassword ="biteme";
    private static final String TOPIC_NAME = "Durable Topic";
    private static final String CLIENT_ID = "Fred";
    private static final Integer MESSAGES = 100;


    @Before
    public void setUp() throws Exception {
        amqUser = System.getProperty("AMQ_USER", amqUser);
        amqPassword = System.getProperty("AMQ_PASSWORD", amqPassword);
        brokerURL = System.getProperty("BROKER_URL", brokerURL);
    }

    /**
     * 1. Subscribe to a durable topic
     * 2. Disconnect the consumer
     * 3. Send some messages
     * 4. Restart the consumer
     * @throws Exception
     */
    @Test
    public void simpleDurableConsumerTest() throws Exception {
        // Call with 0 messages expected; create the topic, subscribe to it, and disconnect
        DurableAsyncTopicConsumer durableConsumer = new DurableAsyncTopicConsumer(CLIENT_ID, TOPIC_NAME, brokerURL, amqUser, amqPassword, 0);
        durableConsumer.go();
        durableConsumer.close();

        // Send a bunch of messages
        TopicProducer topicProducer = new TopicProducer("Durable Topic", MESSAGES, brokerURL, amqUser, amqPassword);
        topicProducer.sendAllMessages();

        // Reconnect and make sure we get all messages
        durableConsumer = new DurableAsyncTopicConsumer(CLIENT_ID, TOPIC_NAME, brokerURL, amqUser, amqPassword, MESSAGES);
        int messagesReceived = durableConsumer.go();
        durableConsumer.close();

        assertEquals(MESSAGES.intValue(), messagesReceived);
    }

}
