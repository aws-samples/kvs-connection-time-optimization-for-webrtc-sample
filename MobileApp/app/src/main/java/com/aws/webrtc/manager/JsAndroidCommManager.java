//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.manager;

import android.annotation.SuppressLint;
import android.util.Log;

import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.auth.AWSTemporaryCredentials;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.auth.options.AuthFetchSessionOptions;
import com.amplifyframework.core.Amplify;
import com.aws.webrtc.datamodel.ClientConfigEntry;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.util.ThreadUtil;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * This class is used to manage the internal communication \
 * between JS SDK and Android (Java) by the JsAndroidCommManager singleInstance.
 */
public class JsAndroidCommManager {

    private final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private static volatile JsAndroidCommManager singleInstance;
    private BridgeWebView bridgeWebView;

    private JsAndroidCommManager() {
    }

    /**
     * Gets the singleton instance of this class.
     * Returns:
     * singleton instance
     */
    public static JsAndroidCommManager getInstance() {
        if (singleInstance == null) {
            synchronized (JsAndroidCommManager.class) {
                if (singleInstance == null) {
                    singleInstance = new JsAndroidCommManager();
                }
            }
        }
        return singleInstance;
    }

    public void setBridgeWebView(BridgeWebView bridgeWebView) {
        this.bridgeWebView = bridgeWebView;
    }

    public BridgeWebView getBridgeWebView() {
        return this.bridgeWebView;
    }

    public final void getAuthSession(final CallBackFunction responseCallback, boolean isForceRefresh) {
        Log.d(TAG, "getAuthSession, isForceRefresh === " + isForceRefresh);
        if (!isForceRefresh) {
            AWSTemporaryCredentials awsCredentials = UserStateManager.getInstance().getCredential().getValue();
            if (awsCredentials != null) {
                final JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("accessKeyId", awsCredentials.getAccessKeyId());
                jsonObject.addProperty("secretAccessKey", awsCredentials.getSecretAccessKey());
                jsonObject.addProperty("sessionToken", awsCredentials.getSessionToken());
                if (responseCallback != null) {
                    ThreadUtil.runOnUiThread(() -> responseCallback.onCallBack(jsonObject.toString()));
                } else {
                    ThreadUtil.runOnUiThread(() -> refreshToken(jsonObject.toString()));
                }
            }
        }
        refreshAuthSession();
    }

    public final void getIdentityId(final CallBackFunction responseCallback) {
        String identityId = UserStateManager.getInstance().getIdentityId().getValue();
        if (identityId != null && !identityId.isEmpty()) {
            if (responseCallback != null) {
                Log.i(TAG, "getIdentityId, identityId === " + identityId);
                ThreadUtil.runOnUiThread(() -> responseCallback.onCallBack(identityId));
            }
        } else {
            Amplify.Auth.fetchAuthSession(
                    response -> {
                        AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) response;
                        String identityIdValue = cognitoAuthSession.getIdentityIdResult().getValue();
                        if (identityIdValue != null && !identityIdValue.isEmpty()) {
                            if (responseCallback != null) {
                                Log.i(TAG, "getIdentityId, identityId === " + identityIdValue);
                                ThreadUtil.runOnUiThread(() -> responseCallback.onCallBack(identityIdValue));
                                UserStateManager.getInstance().initCurrentIdentityId(identityIdValue);
                            }
                        }
                    },
                    error -> Log.e(TAG, "get current user error === " + error)
            );
        }
    }

    private void refreshAuthSession() {
        Log.d(TAG, "refreshAuthSession");
        AuthFetchSessionOptions option = AuthFetchSessionOptions.builder().forceRefresh(true).build();
        Amplify.Auth.fetchAuthSession(
                option,
                value -> {
                    AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) value;
                    AWSTemporaryCredentials awsCredentials = (AWSTemporaryCredentials)
                            cognitoAuthSession.getAwsCredentialsResult().getValue();
                    if (awsCredentials != null) {
                        ThreadUtil.runOnUiThread(() -> UserStateManager.getInstance().getCredential().setValue(awsCredentials));
                    }
                },
                error -> Log.e(TAG, error.toString()));
    }

    public void refreshToken(String token) {
        Log.d(TAG, "refreshToken Request to JS");
        if (bridgeWebView != null) {
            bridgeWebView.callHandler("refreshToken",
                    token, null);
        } else {
            Log.d(TAG, "bridgeWebView is not create");
        }
    }

    /**
     * Get the IotAtsEndpoint from server to webView.
     */
    public void getIotAtsEndpoint(final CallBackFunction responseCallback) {
        String iotEndPoint = UserStateManager.getInstance().getIotEndPoint().getValue();
        if (responseCallback != null) {
            if (iotEndPoint != null) {
                Log.i(TAG, "getIotAtsEndpoint, IotAtsEndpoint === " + iotEndPoint);
                responseCallback.onCallBack(iotEndPoint);
            } else {
                /* call Api Gateway to get the client config. */
                String path = "/client/config";
                RestOptions options = RestOptions.builder()
                        .addPath(path)
                        .build();

                Amplify.API.get(
                        ConstantUtil.CUSTOM_API_NAME,
                        options,
                        response -> {
                            String json = response.getData().asString();
                            Log.d(TAG, "get client config succeeded. " + json + ", code = " + response.getCode());
                            if (response.getCode().isSuccessful()) {
                                Gson gson = new Gson();
                                ClientConfigEntry config = gson.fromJson(json, ClientConfigEntry.class);
                                if (config != null) {
                                    Log.i(TAG, "getIotAtsEndpoint, IotAtsEndpoint === " + config.getIotAtsEndpoint());
                                    responseCallback.onCallBack(config.getIotAtsEndpoint());
                                    UserStateManager.getInstance().getClientConfig().postValue(config);
                                    UserStateManager.getInstance().getIotEndPoint().postValue(config.getIotAtsEndpoint());
                                } else {
                                    Log.e(TAG, "config == null");
                                }
                            }
                        },
                        error -> Log.e(TAG, "get client config failed. " + error)
                );
            }
        }
    }

    public void connectMQTT() {
        Log.i(TAG, "connect MQTT Request");
        if (bridgeWebView != null) {
            ThreadUtil.runOnUiThread(() -> bridgeWebView.callHandler("connectMQTT",
                    "connected from Java", null));
        } else {
            Log.d(TAG, "bridgeWebView is not create");
        }
    }

    /**
     * Request get shadow via webView to JS.
     */
    public void publishUploadDataRequest(String message) {
        if (bridgeWebView != null) {
            Log.i(TAG, "publish upload data Request");
            ThreadUtil.runOnUiThread(() -> bridgeWebView.callHandler("uploadData",
                    message, null));
        } else {
            Log.d(TAG, "bridgeWebView is not create");
        }
    }

}
