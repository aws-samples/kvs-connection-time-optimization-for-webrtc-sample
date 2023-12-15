// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

function assignRegion(regionValue) {
    const region = document.getElementById("region");
    region.setAttribute("value", regionValue);
    console.log("assignRegion, region = " + regionValue);
}

function assignIotAtsEndpoint(iotAtsEndpoint) {
    const endpoint = document.getElementById("endpoint");
    endpoint.setAttribute("value", iotAtsEndpoint);
    console.log("assignIotAtsEndpoint, IotAtsEndpoint = " + iotAtsEndpoint);
}

function assignCredential(accessKey, secretKey, token) {
    const accessKeyId = document.getElementById("accessKeyId");
    accessKeyId.setAttribute("value", accessKey);

    const secretAccessKey = document.getElementById("secretAccessKey");
    secretAccessKey.setAttribute("value", secretKey);

    const sessionToken = document.getElementById("sessionToken");
    sessionToken.setAttribute("value", token);
}

function assignMqttClientId(mqttClientId) {
    const clientId = document.getElementById("clientId");
    clientId.setAttribute("value", mqttClientId);
}

function connectWebViewJavascriptBridge(callback) {
    if (window.WebViewJavascriptBridge) {
        callback(window.WebViewJavascriptBridge)
    } else {
        document.addEventListener(
            'WebViewJavascriptBridgeReady'
            , function () {
                callback(window.WebViewJavascriptBridge)
            },
            false
        );
    }
}

async function getAuthSession() {
    window.WebViewJavascriptBridge.callHandler(
        'getAuthSession',
        {'Data': 'js call getAuthSession() to java'},
        function (responseData) {
            console.log('Js has received auth session Token');
            const json = JSON.parse(responseData);
            assignCredential(json.accessKeyId, json.secretAccessKey, json.sessionToken);
            const startMqttBtn = document.getElementById("start-iot-button");
            if (startMqttBtn !== undefined) {
                console.log('startMqttBtn click');
                startMqttBtn.click();
            }
        }
    );
}

async function getIdentityId() {
    window.WebViewJavascriptBridge.callHandler(
        'getIdentityId',
        {'Data': 'js call getIdentityId() to java'},
        function (responseData) {
            console.log('Js has received identityId === ' + responseData);
            assignMqttClientId(responseData);
        }
    );
}

async function getIotAtsEndpoint() {
    console.log("getIotAtsEndpoint");
    window.WebViewJavascriptBridge.callHandler(
        'getIotAtsEndpoint',
        {'Data': 'js call getIotAtsEndpoint() to java'},
        function (responseData) {
            const IotAtsEndpoint = responseData;
            console.log('Js has received iot ats endpoint, endpoint === ' + IotAtsEndpoint);
            const region = IotAtsEndpoint.split(".")[2];
            assignRegion(region);
            assignIotAtsEndpoint(responseData);
        }
    );
}

connectWebViewJavascriptBridge(function (bridge) {
    bridge.init(function (message, responseCallback) {
        const data = {'Javascript Responds': 'Init!'};
        responseCallback(data);
    });

    bridge.registerHandler("connectMQTT", function (data, responseCallback) {
        getIotAtsEndpoint().then(
            async () => {
                getIdentityId().then(
                    async () => {
                        getAuthSession().then();
                    });
            }
        );
        const responseData = "Js connectMQTT Response!";
        if (responseCallback) {
            responseCallback(responseData);
        }
    });

    bridge.registerHandler("refreshToken", function (data, responseCallback) {
        const json = JSON.parse(data);
        assignCredential(json.accessKeyId, json.secretAccessKey, json.sessionToken);
        const refreshBtn = document.getElementById("refresh-button");
        if (refreshBtn !== undefined) {
            refreshBtn.click();
        }
        const responseData = "Js refreshToken Response!";
        if (responseCallback) {
            responseCallback(responseData);
        }
    });

    bridge.registerHandler("uploadData", function (data, responseCallback) {
        const message = document.getElementById("webrtc-metrics-input");
        message.setAttribute("value", data)
        const btn = document.getElementById("upload-data-button");
        if (btn !== undefined) {
            btn.click();
        }
        const responseData = "Js upload data Response";
        if (responseCallback) {
            responseCallback(responseData);
        }
    });
});
