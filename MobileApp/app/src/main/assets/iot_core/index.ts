// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

import { auth, iot, mqtt } from "aws-iot-device-sdk-v2";
import { CredentialsProvider } from "aws-crt/dist.browser/browser/auth";
import { MqttClientConnection } from "aws-crt/dist.browser/browser/mqtt";
// @ts-ignore
import JavaBridge  = require("./javaBridge");

let mqttConnection: mqtt.MqttClientConnection | null = null;
let cognitoCredentialProvider: AWSCognitoCredentialsProvider;

function randomString(e: number) {
  e = e || 32;
  let t = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz0123456789",
    a = t.length,
    n = "";
  for (let i = 0; i < e; i++) n += t.charAt(Math.floor(Math.random() * a));
  return n;
}

function getFormValues() {
  return {
    region: $('#region').val() as string,
    clientId: $('#clientId').val() as string,
    endpoint: $('#endpoint').val() as string,
    accessKeyId: $('#accessKeyId').val() as string,
    secretAccessKey: $('#secretAccessKey').val() as string,
    sessionToken: $('#sessionToken').val() as string,
    webrtc_metrics: $('#webrtc-metrics-input').val() as string
  };
}

export default class AWSCognitoCredentialsProvider extends auth.CredentialsProvider {

  time: number = 3600 * 1000;

  constructor(expire_interval_in_ms?: number) {
    super();
    setInterval(async () => {
      await this.refreshCredentials();
    }, expire_interval_in_ms ?? this.time);
  }

  getCredentials(): auth.AWSCredentials {
    const formValue = getFormValues();
    JavaBridge.syncCredentialRefresh(true);
    return {
      aws_region: formValue.region,
      aws_access_id: formValue.accessKeyId ?? "",
      aws_secret_key: formValue.secretAccessKey ?? "",
      aws_sts_token: formValue.sessionToken ?? "",
    }
  }

  async refreshCredentials() {
    JavaBridge.refreshAuthSession().then(
      () => console.log('Refreshing Cognito credentials')
    );
  }
}

async function connect_websocket(provider: CredentialsProvider, clientId: string, endpoint: string) {
  return new Promise<mqtt.MqttClientConnection>((resolve, reject) => {
    let config = iot.AwsIotMqttConnectionConfigBuilder.new_builder_for_websocket()
      .with_clean_session(true)
      .with_client_id(`${clientId}/${randomString(16)}`)
      .with_endpoint(endpoint)
      .with_credential_provider(provider)
      .with_use_websockets()
      .with_reconnect_max_sec(600)
      .build();

    console.log("region = " + getFormValues().region);
    console.log("clientId = " + config.client_id);
    console.log("endpoint = " + getFormValues().endpoint);

    const client = new mqtt.MqttClient();
    let connection = client.new_connection(config);
    console.log("create new connection");

    connection.on("connect", (session_present) => {
      console.log("Connection is connected, existing session: " + session_present);
      JavaBridge.syncMQTTConnection("connect")
      resolve(connection);
    });
    connection.on("interrupt", (error) => {
      console.error("Connection is interrupt " + error.error_name);
      JavaBridge.syncMQTTConnection("interrupt")
    });
    connection.on("resume", (return_code, session_present) => {
      console.log("Connection is resume " + return_code + ", existing session: " + session_present);
      JavaBridge.syncMQTTConnection("resume")
    });
    connection.on("disconnect", () => {
      console.log("Connection is disconnect");
      mqttConnection = null
      JavaBridge.syncMQTTConnection("disconnect")
    });
    connection.on("error", (error) => {
      console.log("Connection is error " + error);
      JavaBridge.syncMQTTConnection("error")
      reject(error);
    });
    connection.on("message", (topic, payload, dup, qos, retain) => {
      console.log("message, topic = " + topic);
    })

    console.log("start to connect");
    connection.connect()
      .catch((error) => {
        console.log(error);
      });
  });
}

async function publish(connection: MqttClientConnection, topic: string, payload: string) {
  console.log("start to publish, topic = " + topic);
  console.log(`start to publish, message =  ${payload}`)
  const pubResult = await connection.publish(topic, payload, mqtt.QoS.AtMostOnce)
    .catch((reason) => {
      console.log(`Error while topic ${topic} publish : ${reason}`);
    });
  console.log("pubResult = " + JSON.stringify(pubResult));
  return pubResult;
}

async function start() {

  const formValues = getFormValues();

  /** Set up the credentialsProvider */
  cognitoCredentialProvider = new AWSCognitoCredentialsProvider();
  connect_websocket(cognitoCredentialProvider, formValues.clientId as string, formValues.endpoint as string)
    .then((connection) => {
        mqttConnection = connection;
      }
    )
    .catch((reason) => {
      console.log(`Error while connecting: ${reason}`);
    });
}

export function startNewConnection() {
  if (mqttConnection) {
    console.log("remove all listeners");
    mqttConnection.removeAllListeners();
    mqttConnection.disconnect().then();
  }
  start().then();
}

$('#start-iot-button').click(async () => {
  console.log("start iot core");
  startNewConnection();
});

$('#stop-iot-button').click(async () => {
  console.log("stop iot core");
  if (mqttConnection) {
    const disconnectResult = mqttConnection.disconnect();
    console.log("disconnectResult = " + JSON.stringify(disconnectResult));
  }
});

$('#upload-data-button').click(async () => {
  console.log(`upload-data-button click`);
  const id = getFormValues().clientId;
  const getShadowRequestTopic = id + "/request/metrics/webrtc";
  const payload = getFormValues().webrtc_metrics;
  if (mqttConnection) {
    publish(mqttConnection, getShadowRequestTopic, payload).then();
  }
});

$('#reconnect-button').click(async () => {
  if (mqttConnection || mqttConnection != null) {
    console.log("mqtt reconnect start");
    const reconResult = mqttConnection.connect();
    console.log("reconResult = " + JSON.stringify(reconResult));
  } else {
    console.log("mqtt reconnect start");
    startNewConnection();
  }
});
