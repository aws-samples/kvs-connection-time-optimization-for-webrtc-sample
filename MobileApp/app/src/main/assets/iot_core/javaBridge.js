// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

export async function refreshAuthSession() {
    const webViewJavascriptBridge = window.WebViewJavascriptBridge;
    if (webViewJavascriptBridge) {
        webViewJavascriptBridge.callHandler(
          'getAuthSession',
          {'Data': 'js call refreshAuthSession() to Java'},
          function (responseData) {
              const json = JSON.parse(responseData);
              assignCredential(json.accessKeyId, json.secretAccessKey, json.sessionToken);
          }
        );
    }
}

export function syncMQTTConnection(response) {
    console.log("syncMQTTConnection, sync from JS to Java");
    const webViewJavascriptBridge = window.WebViewJavascriptBridge;
    if (webViewJavascriptBridge) {
        webViewJavascriptBridge.callHandler(
          'syncMQTTConnection',
          response,
          null
        );
    }
}

export function syncCredentialRefresh(response) {
    const webViewJavascriptBridge = window.WebViewJavascriptBridge;
    if (webViewJavascriptBridge) {
        webViewJavascriptBridge.callHandler(
          'syncCredentialRefresh',
          response,
          null
        );
    }
}

