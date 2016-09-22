amq-kit-smoketest
=================

This project contains some simple smoke tests for testing a JBoss A-MQ kit.

1. Update the value of jboss.fuse.bom.version in pom.xml to the current version
2. Start the broker 
3. Run most of the unit tests with this command:

    mvn -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" clean test
4. Restart the broker
5. Run the final test.

   - mvn -PpartTwo -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" clean test
   
This should just run PersistenceReceiveTest, which verifies that messages send by PersistenceSendTest in part 1 are
available after the broker restart.

