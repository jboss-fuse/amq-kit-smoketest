package org.fusesource.amqsmoketest;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * These tests are part of the smoke tests for ActiveMQ kits.  The need to be run against a real broker TODO reword
 */
public class MiscTest {
    protected static final Logger LOG = LoggerFactory.getLogger(MiscTest.class);
    @Rule
    public TestName testName = new TestName();

    protected static String brokerURL = "tcp://localhost:61616";
    protected static String amqUser ="admin";
    protected static String amqPassword ="admin";

    private static ConnectionFactory connectionFactory;
    private Connection connection;
    private Session session;

    @BeforeClass
    public static void init() throws Exception {
        amqUser = System.getProperty("AMQ_USER", amqUser);
        amqPassword = System.getProperty("AMQ_PASSWORD", amqPassword);
        brokerURL = System.getProperty("BROKER_URL", brokerURL);
    }

    @Before
    public void setUp() throws JMSException, NamingException {
        LOG.info("Starting test " + testName.getMethodName());
        connectionFactory = new ActiveMQConnectionFactory(amqUser, amqPassword, brokerURL);
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
    }

    @After
    public void tearDown() {
        try {
            session.close();
        } catch (JMSException e) {
        }
        try {
            connection.close();
        } catch (JMSException e) {
        }
        connectionFactory = null;
    }

    /**
     * Set some headers on a message, send it using a queue, and confirm the received
     * message has the same headers set.
     *
     * @throws Exception
     */
    @Test
    public void testHeadersInQueue() throws Exception {
        Destination queue = session.createQueue("smoke.testQueue.headers");

        MessageProducer producer = session.createProducer(queue);
        Message message = session.createTextMessage("This is a test");
        setMessageProperties(message);
        producer.send(message);

        MessageConsumer consumer = session.createConsumer(queue);
        Message receivedMessage = consumer.receive(5 * 1000);
        assertNotNull(receivedMessage);
        assertEquals(message, receivedMessage);
        checkMessageProperties(receivedMessage);

        producer.close();
    }

    /**
     * Set some headers on a message, send it using a topic, and confirm the received
     * message has the same headers set.
     *
     * @throws Exception
     */
    @Test
    public void testHeadersInTopic() throws Exception {
        Destination topic = session.createTopic("testTopic");
        MessageConsumer messageConsumer = session.createConsumer(topic);
        TopicListener topicListener = new TopicListener();
        messageConsumer.setMessageListener(topicListener);

        MessageProducer producer = session.createProducer(topic);
        Message message = session.createTextMessage("Sample text");
        producer.send(setMessageProperties(message));
        Thread.sleep(5*1000);

        List<Message> messagesReceived = topicListener.getMessagesReceived();
        assertEquals(1, messagesReceived.size());
        Message targetMessage = messagesReceived.get(0);
        assertNotNull(targetMessage);
        assertTrue(targetMessage instanceof TextMessage);
        checkMessageProperties(targetMessage);
    }

    /**
     * Set some headers on an object message, send it using a queue, confirm the received
     * message has the same headers set, and has the correct object content
     *
     * @throws JMSException
     */
    @Test
    public void testObjectMessageAndHeadersQueue() throws JMSException {
        Destination queue = session.createQueue("smoke.testQueue.objects");
        MessageProducer producer = session.createProducer(queue);

        String[] stuff = {"one", "two", "three"};
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(stuff));
        ObjectMessage objectMessage = session.createObjectMessage();
        objectMessage.setObject(list);
        producer.send(objectMessage);

        MessageConsumer consumer = session.createConsumer(queue);
        Message message = consumer.receive(5 * 1000);
        assertNotNull(message);
        assertTrue(message instanceof ObjectMessage);

        ObjectMessage receivedObjectMessage = (ObjectMessage) message;
        Object content = receivedObjectMessage.getObject();
        assertNotNull(content);
        assertTrue(content instanceof ArrayList);
        ArrayList<String> contentList = (ArrayList<String>) content;
        assertEquals(3, contentList.size());

        producer.close();
        consumer.close();
    }

    /**
     * Simple time to live test.  Set a short TTL on a message, send it, wait longer than the TTL, and
     * make sure we don't get the message.
     *
     * NOTE: I'm not sure this is guaranteed to work.  I think TTL is a minimum, and I'm not sure messages are ever
     * guaranteed to be expired.
     *
     * @throws Exception
     */
    @Test
    public void testTTL() throws Exception {
        Destination queue = session.createQueue("smoke.testQueue.ttl");
        MessageProducer producer = session.createProducer(queue);
        producer.setTimeToLive(250);
        Message m = session.createTextMessage("Sample text");
        producer.send(m);

        Thread.sleep(10 * 1000);
        MessageConsumer consumer = session.createConsumer(queue);
        Message message = consumer.receive(1 * 1000);
        assertNull(message);
    }


    /**
     * Send messages using a queue, and make sure we only get the ones specified by selector.
     *
     * @throws JMSException
     */
    @Test
    public void testQueueSelector() throws JMSException {
        Destination queue = session.createQueue("smoke.testQueue.selector");
        MessageProducer producer = session.createProducer(queue);

        for (int i=0; i < 10; i++) {
            Message m = session.createTextMessage("Test message " + i);
            m.setIntProperty("Order", i);
            producer.send(setMessageProperties(m));
        }

        String selector = "Order < 5";
        MessageConsumer consumer = session.createConsumer(queue, selector);

        int messageCount = 0;
        while(true) {
            Message message = consumer.receive(5 * 1000);
            if (message == null) {
                break;
            }
            assertTrue(message instanceof TextMessage);
            TextMessage tm = (TextMessage) message;
            int order = tm.getIntProperty("Order");
            assertTrue(order < 5);
            assertEquals("Test message " + order, tm.getText());
            messageCount++;
        }

        assertEquals(5, messageCount);

        producer.close();
        consumer.close();
    }


    /**
     * Send messages using a queue, and make sure we only get the ones specified by selector.
     *
     * @throws Exception
     */
    @Test
    public void testTopicSelector() throws Exception {
        Destination topic = session.createTopic("testTopic.selector");
        String selector = "Order < 5";
        MessageConsumer messageConsumer = session.createConsumer(topic, selector);

        TopicListener topicListener = new TopicListener();
        messageConsumer.setMessageListener(topicListener);

        MessageProducer producer = session.createProducer(topic);
        for (int i=0; i < 10; i++) {
            Message m = session.createTextMessage("Test message " + i);
            m.setIntProperty("Order", i);
            producer.send(setMessageProperties(m));
        }

        Thread.sleep(1 * 1000);
        List<Message> messagesReceived = topicListener.getMessagesReceived();
        assertEquals(5, messagesReceived.size());

    }


    /**
     * Test sending a large message on a queue
     *
     * @throws Exception
     */
    @Test
    public void testSendLargeTextMessageOnQueue() throws Exception {
        Destination queue = session.createQueue("smoketest.queue." + testName.getMethodName());
        MessageProducer producer = session.createProducer(queue);

        String messageText=createLargeString(1024 * 1024);
        Message messageToSend = session.createTextMessage(messageText);
        producer.send(messageToSend);

        MessageConsumer consumer = session.createConsumer(queue);
        Message message = consumer.receive(1 * 1000);
        assertNotNull(message);
        assertTrue(message instanceof TextMessage);
        TextMessage tm = (TextMessage) message;
        assertEquals("Message text was not the same", messageText, tm.getText());
    }


    /**
     * Test sending a large message on a topic
     *
     * @throws Exception
     */
    @Test
    public void testSendLargeTextMessageOnTopic() throws Exception {
        Destination topic = session.createTopic("smoketest.topic." + testName.getMethodName());
        MessageConsumer messageConsumer = session.createConsumer(topic);
        TopicListener topicListener = new TopicListener();
        messageConsumer.setMessageListener(topicListener);

        MessageProducer producer = session.createProducer(topic);
        String messageText=createLargeString(1024 * 1024);
        Message message = session.createTextMessage(messageText);
        producer.send(setMessageProperties(message));
        Thread.sleep(1*1000);

        List<Message> messagesReceived = topicListener.getMessagesReceived();
        assertEquals(1, messagesReceived.size());
        Message targetMessage = messagesReceived.get(0);
        assertNotNull(targetMessage);
        assertTrue(targetMessage instanceof TextMessage);
        TextMessage tm = (TextMessage) targetMessage;
        assertEquals("Message text was not the same", messageText, tm.getText());
    }


    /**
     * Return a String of sizeInBytes
     * @param sizeInBytes
     * @return
     */
    private String createLargeString(int sizeInBytes) {
        byte[] base = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sizeInBytes; i++) {
            builder.append(base[i % base.length]);
        }

        LOG.debug("Created string with size : " + builder.toString().getBytes().length + " bytes");
        return builder.toString();
    }

    private Message setMessageProperties(Message message) throws JMSException {
        message.setBooleanProperty("TestBoolean", true);
        message.setByteProperty("TestByte", (byte) 1);
        message.setDoubleProperty("TestDouble", 12.5);
        message.setFloatProperty("TestFloat", (float) 15.5);
        message.setIntProperty("TestInt", 10);
        message.setLongProperty("TestLong", 11);
        message.setObjectProperty("TestObject", new Integer(12));
        message.setShortProperty("TestShort", (short) 13);
        message.setStringProperty("TestString", "Value");

        return message;
    }


    private void checkMessageProperties(Message message) throws JMSException {
        boolean booleanProperty = message.getBooleanProperty("TestBoolean");
        byte byteProperty = message.getByteProperty("TestByte");
        double doubleProperty = message.getDoubleProperty("TestDouble");
        float floatProperty = message.getFloatProperty("TestFloat");
        int intProperty = message.getIntProperty("TestInt");
        long longProperty = message.getLongProperty("TestLong");
        short shortProperty = message.getShortProperty("TestShort");
        String stringProperty = message.getStringProperty("TestString");
        Object objectProperty = message.getObjectProperty("TestObject");

        assertTrue(booleanProperty);
        assertEquals(byteProperty, (byte) 1);
        assertTrue(doubleProperty == 12.5);
        assertTrue(floatProperty == (float) 15.5);
        assertEquals(intProperty, 10);
        assertEquals(longProperty, 11);
        assertEquals(shortProperty, (short) 13);
        assertEquals(stringProperty, "Value");
        assertTrue(objectProperty.equals(new Integer(12)));
    }

    /**
     * Simple messageListener which will collect all messages it receives
     */
    public class TopicListener implements MessageListener {
        private List<Message> messagesReceived = new ArrayList<>();

        @Override
        public void onMessage(Message message) {
            messagesReceived.add(message);
        }

        public List<Message> getMessagesReceived() {
            return messagesReceived;
        }
    }
}
