// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

function preConnect()
{
    console.log('[metrics],PreConnection,PreConnection,PreConnection,start');
    start(false).then();
}

function startVideo()
{
    console.log("start video");
    start(true).then();
}

function stopVideo()
{
    console.log("stopVideo");
    stop();
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

connectWebViewJavascriptBridge(function (bridge) {
    console.log("bridge init finished, bridge = ", bridge);
    bridge.init(function (message, responseCallback) {
        const data = {'Javascript Responds': 'Init!'};
        responseCallback(data);
    });

    preConnect();

    bridge.registerHandler("startViewer", function (data, responseCallback) {
        startVideo();
        const responseData = "start viewer";
        if (responseCallback) {
            responseCallback(responseData);
        }
    });

    bridge.registerHandler("stopViewer", function (data, responseCallback) {
        stopVideo();
        const responseData = "stop viewer";
        if (responseCallback) {
            responseCallback(responseData);
        }
    });
});
