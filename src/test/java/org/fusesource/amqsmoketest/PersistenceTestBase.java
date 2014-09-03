package org.fusesource.amqsmoketest;

import org.junit.BeforeClass;

/**
 * Created by kearls on 25/08/14.
 */
public abstract class PersistenceTestBase {
    protected static String brokerURL = "tcp://localhost:61616";
    protected static String amqUser ="admin";
    protected static String amqPassword ="admin";
    protected static final String TARGET_QUEUE_NAME = "queue://JOBS.suspend";
    protected static Integer totalMessages = 1000;
    public static String jobs[] = new String[]{"delete", "suspend"};


    @BeforeClass
    public static void beforeClass() {
        amqUser = System.getProperty("AMQ_USER", amqUser);
        amqPassword = System.getProperty("AMQ_PASSWORD", amqPassword);
        brokerURL = System.getProperty("BROKER_URL", brokerURL);
    }
}
