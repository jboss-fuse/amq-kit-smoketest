package org.fusesource.amqsmoketest;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by kearls on 25/08/14.
 *
 * This is the second part of the persistence test.  It should only be run after the first part (PersistenceSendTest)
 * runs, and the broker is restarted.
 *
 */
public class PersistenceReceiveTest extends PersistenceTestBase {
    @BeforeClass
    public static void before() {
        PersistenceTestBase.beforeClass();
    }

    @Test
    public void receiveTest() throws JMSException, InterruptedException {
        AsyncConsumer consumer = new AsyncConsumer(queueNames, totalMessages, TARGET_QUEUE_NAME, brokerURL, amqUser, amqPassword);

        for (String jobName : queueNames) {
            Session session = consumer.getSession();
            String messageQueueName = "JOBS." + jobName;
            Destination destination = session.createQueue(messageQueueName);
            MessageConsumer messageConsumer = session.createConsumer(destination);
            System.out.println("Setting listener for " + messageQueueName);
            messageConsumer.setMessageListener(consumer);
        }

        consumer.receivedCount.await(30, TimeUnit.SECONDS);
        consumer.close();

        System.out.println("Received " + consumer.suspendCount.get() + " messages on foo queue, " + consumer.deleteCount.get() + " on bar queue");

        assertEquals(totalMessages/2, consumer.suspendCount.get());
        assertEquals(totalMessages/2, consumer.deleteCount.get());
    }
}
