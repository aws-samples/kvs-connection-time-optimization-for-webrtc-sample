#!/bin/sh

cd ..
PATH_LOCAL_KVS=MobileApp/app/src/main/assets/kvs-webrtc

# Download additional js/css files
URL_REMOTE_JQUERY_JS=https://code.jquery.com/jquery-3.3.1.slim.min.js
URL_REMOTE_KVS_WEBRTC_JS=https://unpkg.com/amazon-kinesis-video-streams-webrtc@2.1.0/dist/kvs-webrtc.min.js
URL_REMOTE_AWS_SDK_JS=https://sdk.amazonaws.com/js/aws-sdk-2.1363.0.min.js

echo "start downloading third-party dependencies for kvs webrtc"
if [ ! -d $PATH_LOCAL_KVS/dependency ]; then
  mkdir $PATH_LOCAL_KVS/dependency
fi
curl -o $PATH_LOCAL_KVS/dependency/jquery-3.3.1.slim.min.js $URL_REMOTE_JQUERY_JS
curl -o $PATH_LOCAL_KVS/dependency/kvs-webrtc.min.js $URL_REMOTE_KVS_WEBRTC_JS
curl -o $PATH_LOCAL_KVS/dependency/aws-sdk-2.1363.0.min.js $URL_REMOTE_AWS_SDK_JS


# Set up environment for iot core
PATH_LOCAL_IOT=MobileApp/app/src/main/assets/iot_core

# Install packages and build
cd $PATH_LOCAL_IOT
npm install
npm run build
rm -rf node_modules
rm -rf package-lock.json

# Download additional js files
echo "start downloading third-party dependencies for iot core mqtt"
if [ ! -d dependency ]; then
  mkdir dependency
fi
curl -o dependency/jquery-3.3.1.slim.min.js $URL_REMOTE_JQUERY_JS