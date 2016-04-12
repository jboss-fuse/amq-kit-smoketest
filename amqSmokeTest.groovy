// NOTE checkout should be done by caller
// stage 'Checkout'
//git url: 'git@github.com:jboss-fuse/amq-kit-smoketest.git'
//checkout scm

stage 'define tools'
// Get the tools and override EVs which may be set on the node
def M2_HOME = tool 'maven-3.2.3'  // TODO fix 3.3.3 on windows node
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

def unix = isUnix()

stage 'cleanup from previous runs'
if (unix) {
    sh 'rm -rf jboss-a-mq*'
} else {
    bat 'rm -rf jboss-a-mq*'
}

// Download the kit and unzip it
stage 'download'
if(unix) {
    sh 'wget --no-verbose ${AMQ_KIT_URL}'
    sh 'unzip -q ${ZIPFILENAME}'
} else {
    bat 'wget --no-verbose --no-check-certificate %AMQ_KIT_URL%'
    bat 'unzip -q %ZIPFILENAME%'
}

// 3. Uncomment admin user in etc/user.properties
if (unix) {
    sh 'sed -i \'s/^#admin/admin/g\' ${AMQ_HOME}/etc/users.properties'
} else {
    bat 'sed -i \'s/^#admin/admin/g\' %AMQ_HOME%/etc/users.properties'
}

try {
    // 4. Start the broker
    stage 'starting broker'
    if (unix) {
        sh './${AMQ_HOME}/bin/start'
    } else {
        bat '%AMQ_HOME%\\bin\\start'
    }
    //  Wait for it to start -- search for "Broker amq has started." in log, or sleep
    sleep 90

    stage 'Part 1 of tests'
    if (unix) {
        sh 'mvn --version'
        sh 'mvn -Dmaven.test.failure.ignore -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" clean test'
    } else {
        bat 'mvn --version'
        bat 'mvn -Dmaven.test.failure.ignore -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" clean test'
    }

    stage 'Broker Restart'
    if (unix) {
        sh './${AMQ_HOME}/bin/stop'
        sleep 30
        sh './${AMQ_HOME}/bin/start'
        sleep 60
    } else {
        bat '%AMQ_HOME%\\bin\\stop'
        sleep 30
        bat '%AMQ_HOME%\\bin\\start'
        sleep 60
    }

    stage 'Part 2 of tests'
    if (unix) {
        sh 'mvn -PpartTwo -Dmaven.test.failure.ignore -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" test'
    } else {
        bat 'mvn -PpartTwo -Dmaven.test.failure.ignore -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" test'
    }
} finally {
    stage 'Final shutdown'
    if (unix) {
        sh './${AMQ_HOME}/bin/stop'
    } else {
        bat '%AMQ_HOME%\\bin\\stop'
    }

    if (!unix) {
        build job: 'Reboot_windows', quietPeriod: 30, wait: false
    } else {
        stage 'clear out workspace'
        deleteDir()  //Looks like we can't do this on windows
    }
}
