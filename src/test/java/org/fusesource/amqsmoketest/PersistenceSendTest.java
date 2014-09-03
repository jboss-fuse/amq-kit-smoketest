package org.fusesource.amqsmoketest;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.jms.JMSException;

/**
 * Created by kearls on 25/08/14.
 */
public class PersistenceSendTest extends PersistenceTestBase {

    @BeforeClass
    public static void before() {
        PersistenceTestBase.beforeClass();
    }

    @Test
    public void sendTest() throws JMSException {
        Producer producer = new Producer(totalMessages, jobs, TARGET_QUEUE_NAME, brokerURL, amqUser, amqPassword);
        producer.sendAllMessages();

        System.out.println("Sent " + producer.suspendCount.get() + " on " + jobs[0] + " queue, " + producer.deleteCount.get() + " on " + jobs[1] + " queue");
        producer.close();
    }
}
