#
# TODO check parameters; 
#

export OSE_AMQ_CARTRIDGE_RPM=$1
export APP_NAME=amqsmoketestapp
export SMOKETESTNAMESPACE=smokenamespace
rhc setup --rhlogin demo --password openshift --create-token --server vm.openshift.example.com
rhc app-delete --confirm ${APP_NAME}
rm -rf ${APP_NAME}
sudo oo-admin-ctl-cartridge --command delete --name amq-6.2.0
sudo yum remove --assumeyes openshift-origin-cartridge-amq
rm -rf *.rpm    
wget $OSE_AMQ_CARTRIDGE_RPM
sudo yum --assumeyes localinstall $OSE_AMQ_CARTRIDGE_RPM
sudo service ruby193-mcollective restart
sudo oo-admin-ctl-cartridge --command import-profile --activate
sudo oo-admin-ctl-cartridge --command list 

#
# Create the smoketest app
# 
rhc app-create ${APP_NAME} amq-6.2.0 --namespace ${SMOKETESTNAMESPACE}
rhc apps
rhc show-app ${APP_NAME}

#
# Find the fuse password and OSE...port
#
export OPENSHIFT_APP_UUID=`rhc show-app ${APP_NAME} | grep SSH | sed 's/^.*SSH:[ \t]*//g' | sed 's/@.*//g'`
export AMQ_PASSWORD=`sudo cat /var/lib/openshift/${OPENSHIFT_APP_UUID}/amq/container/etc/passwd`
export AMQ_OPENWIRE_PROXY_PORT=`sudo cat /var/lib/openshift/${OPENSHIFT_APP_UUID}/.env/OPENSHIFT_AMQ_OPENWIRE_PROXY_PORT`
echo OPENSHIFT_APP_UUID ${OPENSHIFT_APP_UUID}
echo AMQ_PASSWORD ${AMQ_PASSWORD}
echo AMQ_OPENWIRE_PROXY_PORT ${AMQ_OPENWIRE_PROXY_PORT}
#
# Run the smoke tests
#
set -x
mvn -DAMQ_USER=admin -DAMQ_PASSWORD=${AMQ_PASSWORD} -DBROKER_URL="tcp://${APP_NAME}-${SMOKETESTNAMESPACE}.openshift.example.com:${AMQ_OPENWIRE_PROXY_PORT}" clean test
rhc app-restart ${APP_NAME}
sleep 60s
mvn -PpartTwo -DAMQ_USER=admin -DAMQ_PASSWORD=${AMQ_PASSWORD} -DBROKER_URL="tcp://${APP_NAME}-${SMOKETESTNAMESPACE}.openshift.example.com:${AMQ_OPENWIRE_PROXY_PORT}" clean test
