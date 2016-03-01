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
    //def BLAH=AMQ_KIT_URL.replaceAll("^.*amq/jboss-a-mq", "")
    //def ZIPFILENAME=BLAH.replaceAll(".*\\/", "")
    //def AMQ_HOME=ZIPFILENAME.replace(".zip", "")
    //env.ZIPFILENAME="${ZIPFILENAME}"
    //env.AMQ_HOME="${AMQ_HOME}"

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
    sh './${AMQ_HOME}/bin/start'

    // TODO Wait for it to start -- search for "Broker amq has started." in log, or sleep
    sleep 90
    //grep "Broker amq has started." ${AMQ_HOME}/data/log/amq.log

    stage 'Part 1 of tests'
    sh 'mvn --version'
    sh 'mvn -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" clean test'

    stage 'restart'
    sh './${AMQ_HOME}/bin/stop'
    sleep 30
    sh './${AMQ_HOME}/bin/start'
    sleep 60

    stage 'Part 2 of tests'
    sh 'mvn -PpartTwo -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" test'

    stage 'Final shutdown'
    sh './${AMQ_HOME}/bin/stop'

    stage 'clear out workspace'
    deleteDir()
}