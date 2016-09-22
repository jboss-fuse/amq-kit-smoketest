// NOTE checkout should be done by caller
// stage 'Checkout'
//git url: 'git@github.com:jboss-fuse/amq-kit-smoketest.git'
//checkout scm

stage 'define tools'
// Get the tools and override EVs which may be set on the node
def M2_HOME = tool 'maven-3.3.9'
def JAVA_HOME = tool 'jdk8'
env.JAVA_HOME="${JAVA_HOME}"
env.M2_HOME="${M2_HOME}"
env.PATH = "${M2_HOME}/bin:${JAVA_HOME}/bin:${env.PATH}"

stage 'set amq version'
pwd()
sh 'ls -alF'

echo 'AMQ_KIT_URL is ' +  AMQ_KIT_URL
def lastSlash = AMQ_KIT_URL.lastIndexOf("/");
def zipFileName = AMQ_KIT_URL.substring(lastSlash + 1, AMQ_KIT_URL.length());
def amqHome = zipFileName.substring(0, zipFileName.length() - 4);

currentBuild.description = amqHome

stage 'Update pom version'
def pom = new File("pom.xml");
def updated = pom.getText().replaceAll(/<jboss.fuse.bom.version>.*<\/jboss.fuse.bom.version>/, '<jboss.fuse.bom.version>' + version +'</jboss.fuse.bom.version>');
echo '---------- new pom ----------'
println(updated);
pom.write(updated);
echo '-----------------------------'

stage 'cleanup from previous runs'
cleanup("jboss-a-mq*")
stage 'download kit'
downloadAndUnzipKit(AMQ_KIT_URL, zipFileName)
uncommentAdminUserPassword(amqHome);

try {
    stage 'starting broker'
    startBroker(amqHome)
    sleep 90

    stage 'Part 1 of tests'
    maven('--version')
    maven('-Dmaven.test.failure.ignore -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" clean test')

    stage 'Broker Restart'
    stopBroker(amqHome)
    sleep 30
    startBroker(amqHome)
    sleep 60

    stage 'Part 2 of tests'
    maven('-PpartTwo -Dmaven.test.failure.ignore -DAMQ_USER=admin -DAMQ_PASSWORD=admin -DBROKER_URL="tcp://localhost:61616" clean test')
} finally {
    stage 'Final shutdown'
    stopBroker(amqHome)

    // TODO do this here, or in calling script?
    if (!isUnix()) {
        build job: 'Reboot_windows', quietPeriod: 30, wait: false
    } else {
        stage 'clear out workspace'
        deleteDir()  //Looks like we can't do this on windows
    }
}

// TODO find somewhere to put this code so it can be shared.
def cleanup(directoryName) {
    if (isUnix()) {
        sh 'rm -rf ' + directoryName
    } else {
        bat 'rm -rf ' + directoryName
    }
}

def downloadAndUnzipKit(downloadUrl, zipFileName) {
    if(isUnix()) {
        sh 'wget --no-verbose ' + downloadUrl
        sh 'unzip -q ' + zipFileName
    } else {
        bat 'wget --no-verbose --no-check-certificate ' + downloadUrl
        bat 'unzip -q ' + zipFileName
    }
}

def uncommentAdminUserPassword(amqHomeDirectory) {
    if (isUnix()) {
        sh 'sed -i \'s/^#admin/admin/g\' ' + amqHomeDirectory + '/etc/users.properties'
    } else {
        bat 'sed -i \'s/^#admin/admin/g\' ' + amqHomeDirectory + '/etc/users.properties'
    }
}

def startBroker(amqHomeDirectory) {
    if (isUnix()) {
        sh './' + amqHomeDirectory + '/bin/start'
    } else {
        bat amqHomeDirectory + '\\bin\\start'
    }
}

def stopBroker(amqHomeDirectory) {
    if (isUnix()) {
        sh './' + amqHomeDirectory + '/bin/stop'
    } else {
        bat amqHomeDirectory + '\\bin\\stop'
    }
}

def maven(command) {
    if (isUnix()) {
        sh 'mvn ' + command
    } else {
        bat 'mvn ' + command
    }
}

