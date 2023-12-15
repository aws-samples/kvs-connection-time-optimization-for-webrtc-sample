Running Mobile App Sample
=============================================
This is a sample Android application that demonstrates how to integrate [KVS WebRTC Test Page](https://github.com/awslabs/amazon-kinesis-video-streams-webrtc-sdk-js/tree/master/examples), implement optimizations for KVS WebRTC viewer, 
collect and upload timing metrics to the backend.

## Requirements
* [AndroidStudio 3.2+](https://developer.android.com/studio/)
* Android API 28 or greater
  * Recommended: 30+
* Android WebView 89.0.4389.90 or greater
  * Recommended: 115.0.5790.166
* Bash
* [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm)
* Curl

## Using the Sample

1. Import the MobileApp project into Android Studio.
    - From the Welcome screen, click on "Import project".
    - Browse to the MobileApp directory and press OK.
    - Accept the messages about adding Gradle to the project.
    - If the SDK reports some missing Android SDK packages (like Build Tools or the Android API package), follow the instructions to install them.

2. Import libraries :
    - Gradle will take care of downloading these dependencies for you.

3. Open `app/src/main/res/raw/amplifyconfiguration.json` and replace `REPLACE_ME` with the appropriate values:

    ```
        "CredentialsProvider": {
            "CognitoIdentity": {
                "Default": {
                    "PoolId": "REPLACE_ME",
                    "Region": "REPLACE_ME"
                }
            }
        },
        "CognitoUserPool": {
            "Default": {
                "PoolId": "REPLACE_ME",
                "AppClientId": "REPLACE_ME",
                "Region": "REPLACE_ME"
            }
        },
        "awsAPIPlugin": {
            "end-user-api": {
                "endpointType": "REST",
                "endpoint": "REPLACE_ME",
                "region": "REPLACE_ME",
                "authorizationType": "AWS_IAM"
            }
        }
    ```

4. Run `download-dependency.sh` in current script working space to download third-party dependencies with the following command.
      ```
         bash download-dependency.sh
      ```
   
5. Open `app/src/main/assets/kvs-webrtc/index.html` and replace `REPLACE_ME` with the appropriate values:
   ```
       <input type="text" id="region" value="REPLACE_ME">
       <input type="text" id="accessKeyId" placeholder="Access key id" value="REPLACE_ME">
       <input type="password" id="secretAccessKey" placeholder="Secret access key" value="REPLACE_ME">
       <input type="text" id="channelName" placeholder="Channel" value="REPLACE_ME">
   ```
6. Run the sample application in simulator or in Android device (connected through USB).
* *Note:* To build a signed apk for release, configure signingConfigs in `keystore.properties` first
  ```
        keyAlias=REPLACE_ME
        keyPassword=REPLACE_ME
        storeFile=REPLACE_ME
        storePassword=REPLACE_ME
  ```