amq-kit-smoketest
=================

This project contains some simple smoke tests for testing a JBOSS Fuse A-MQ Cartridge for OpenShift, or for
smoke testing a normal JBoss A-MQ kit.

1. Run most of the unit tests with this command:

    mvn -DAMQ_USER=user -DAMQ_PASSWORD=password -DBROKER_URL="tcp://localhost:61616" clean test

2. Then restart the broker or app, and run the final test.

   - restart the broker or app
   - mvn -PpartTwo -DAMQ_USER=user -DAMQ_PASSWORD=password -DBROKER_URL="tcp://localhost:61616" clean test
   
This should just run PersistenceReceiveTest, which verifies that messages send by PersistenceSendTest in part 1 are
available after the broker restart.

