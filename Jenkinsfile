node('checkin-short') {
    // TODO
    // 1. Add a try/catch so we always shutdown the broker

    // Mark the code checkout 'stage'....
    stage 'Checkout'

    // Get some code from a GitHub repository
    //git url: 'git@github.com:jboss-fuse/amq-kit-smoketest.git'
    checkout scm

    // Get the tools and override EVs which may be set on the node
    def M2_HOME = tool 'maven-3.3.3'
    def JAVA_HOME = tool 'jdk8'
    env.JAVA_HOME="${JAVA_HOME}"
    env.M2_HOME="${M2_HOME}"
    env.PATH = "${M2_HOME}/bin:${JAVA_HOME}/bin:${env.PATH}"

    // Get the zipfile name and home directory from the full download URL
    env.AMQ_KIT_URL = "${AMQ_KIT_URL}"

    def lastSlash = AMQ_KIT_URL.lastIndexOf("/");
    def zipFileName = AMQ_KIT_URL.substring(lastSlash + 1, AMQ_KIT_URL.length());
    def amqHome = zipFileName.substring(0, zipFileName.length() - 4);

    env.ZIPFILENAME="${zipFileName}"
    env.AMQ_HOME="${amqHome}"

    sh 'env | sort'

    // 1. Clean up from previous runs
    sh 'rm -rf jboss-a-mq*'

    // 2.  Download the kit and unzip it
    stage 'download'
    sh 'wget --no-verbose ${AMQ_KIT_URL}'
    sh 'unzip -q ${ZIPFILENAME}'

    // 3. Uncomment admit user in etc/passwd
    sh 'sed -i \'s/^#admin/admin/g\' ${AMQ_HOME}/etc/users.properties'

    // 4. Start the broker
    stage 'starting broker'
    sh './${AMQ_HOME}/bin/start'

    // TODO Wait for it to start -- search for "Broker amq has started." in log, or sleep
    sleep 90
    //grep "Broker amq has started." ${AMQ_HOME}/data/log/amq.log

    stage 'Part 1 of tests'
    sh 'mvn --version'
    sh 'mvn -Dmaven.test.failure.ignore? -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" clean test'

    stage 'Broker Restart'
    sh './${AMQ_HOME}/bin/stop'
    sleep 30
    sh './${AMQ_HOME}/bin/start'
    sleep 60

    stage 'Part 2 of tests'
    sh 'mvn -PpartTwo -Dmaven.test.failure.ignore? -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" test'

    stage 'Final shutdown'
    sh './${AMQ_HOME}/bin/stop'

    stage 'clear out workspace'
    deleteDir()
}