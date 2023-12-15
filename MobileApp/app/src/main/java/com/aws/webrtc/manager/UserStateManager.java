//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.manager;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.auth.AWSTemporaryCredentials;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.core.Amplify;
import com.aws.webrtc.callback.DataChangedCallback;
import com.aws.webrtc.datamodel.ClientConfigEntry;
import com.aws.webrtc.datamodel.ErrorInfoEntry;
import com.aws.webrtc.datamodel.LocationInfoEntry;
import com.aws.webrtc.datamodel.WebRtcMetrics;
import com.aws.webrtc.util.ConstantUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Arrays;

/**
 * This class is used to manage the user's state by the UserStateManager singleInstance.
 */
public class UserStateManager {

    private final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    @SuppressLint("StaticFieldLeak")
    private static volatile UserStateManager singleInstance;

    private final MutableLiveData<ClientConfigEntry> mClientConfigEntry = new MutableLiveData<>();
    private final MutableLiveData<String> mIotEndPoint = new MutableLiveData<>();
    private final MutableLiveData<AWSTemporaryCredentials> mCredential = new MutableLiveData<>();

    private final MutableLiveData<Boolean> mAddTarget = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> mIsMqttConnected = new MutableLiveData<>(false);

    private final MutableLiveData<String> mIdentityId = new MutableLiveData<>("");
    private final MutableLiveData<String> mUser = new MutableLiveData<>("");

    private WebRtcMetrics mWebRtcMetrics;
    private boolean mStreamingSuccess = false;
    private ErrorInfoEntry mErrorInfoEntry;
    private String mNetWorkType;
    private String mClientIp;
    private LocationInfoEntry Location;
    private String ISP;

    private String mAppVersion;

    private UserStateManager() {
    }

    public WebRtcMetrics getWebRtcMetrics() {
        return mWebRtcMetrics;
    }

    public void setWebRtcMetrics(WebRtcMetrics mWebRtcMetrics) {
        this.mWebRtcMetrics = mWebRtcMetrics;
    }

    public void setStreamingSuccess(boolean mStreamingSuccess) {
        this.mStreamingSuccess = mStreamingSuccess;
    }

    public boolean isStreamingSuccess() {
        return mStreamingSuccess;
    }

    public ErrorInfoEntry getErrorInfoEntry() {
        return mErrorInfoEntry;
    }

    public void setErrorInfoEntry(ErrorInfoEntry entry) {
        this.mErrorInfoEntry = entry;
    }

    public String getNetWorkType() {
        return mNetWorkType;
    }

    public String getClientIp() {
        return mClientIp;
    }

    public void setClientIp(String mClientIp) {
        this.mClientIp = mClientIp;
    }

    public void setNetWorkType(String mNetWorkType) {
        this.mNetWorkType = mNetWorkType;
    }

    public void setLocation(LocationInfoEntry location) {
        Location = location;
    }

    public LocationInfoEntry getLocation() {
        return Location;
    }

    public String getISP() {
        return ISP;
    }

    public void setISP(String ISP) {
        this.ISP = ISP;
    }

    public String getAppVersion() {
        return mAppVersion;
    }

    public void setAppVersion(String mAppVersion) {
        this.mAppVersion = mAppVersion;
    }

    /** Gets the singleton instance of this class.
     *  Returns:
     *    singleton instance
     * */
    public static UserStateManager getInstance() {
        if (singleInstance == null) {
            synchronized (UserStateManager.class) {
                if (singleInstance == null) {
                    singleInstance = new UserStateManager();
                }
            }
        }
        return singleInstance;
    }

    public static void resetInstance() {
        singleInstance = null;
    }


    public MutableLiveData<String> getIotEndPoint() {
        return mIotEndPoint;
    }

    public MutableLiveData<ClientConfigEntry> getClientConfig() {
        return mClientConfigEntry;
    }

    public MutableLiveData<Boolean> getAddTarget() {
        return mAddTarget;
    }
    public MutableLiveData<AWSTemporaryCredentials> getCredential() {
        return mCredential;
    }

    public String getCurrentUserName() {
        return mUser.getValue();
    }

    public MutableLiveData<String> getIdentityId() {
        return mIdentityId;
    }
    public MutableLiveData<String> getUser() {
        return mUser;
    }

    public MutableLiveData<Boolean> getIsMqttConnected() {
        return mIsMqttConnected;
    }

    public void initCredential(AWSTemporaryCredentials credentials) {
        mCredential.postValue(credentials);
    }

    /**
     * Get the client config from server by calling Api Gateway.
     */
    public void initClientConfig(DataChangedCallback callback) {
        /* call Api Gateway to get the client config. */
        String path = "/client/config";
        RestOptions options = RestOptions.builder()
                .addPath(path)
                .build();

        Log.d(TAG, "options = " + options);

        Amplify.API.get(
                ConstantUtil.CUSTOM_API_NAME,
                options,
                response -> {
                    String json = response.getData().asString();
                    Log.d(TAG, "received config === " + json);
                    if (response.getCode().isSuccessful()) {
                        Gson gson = new Gson();
                        ClientConfigEntry config = gson.fromJson(json, ClientConfigEntry.class);
                        if (config != null) {
                            Log.d(TAG, "IotAtsEndpoint === " + config.getIotAtsEndpoint());
                            this.mClientConfigEntry.postValue(config);
                            if (callback != null) {callback.onDataChanged(config);}
                            mIotEndPoint.postValue(config.getIotAtsEndpoint());
                        } else {
                            Log.e(TAG, "received config is null");
                        }
                    } else {
                        Log.e(TAG, "received code = " + response.getCode());
                    }
                },
                error -> {
                    Log.e(TAG, "get client config failed. " + error);
                    Log.e(TAG, "stackTrace = " + Arrays.toString(error.getStackTrace()));
                    Log.e(TAG, "cause = " + error.getCause());
                }
        );
    }

    /**
     * Add target to Iot Core Policy by calling Api Gateway.
     */
    public void addTarget(String identityId, String userName) {
        if (identityId == null || userName == null) {
            Log.e(TAG, "invalid param");
            return;
        }

        Log.d(TAG, "identityId = " + identityId + ", userName = " + userName);

        String path = "/add-target";
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("IdentityId", identityId);
        jsonObject.addProperty("Username", userName);
        byte[] data = jsonObject.toString().getBytes();
        RestOptions options = RestOptions.builder()
                .addPath(path)
                .addBody(data)
                .build();

        Log.d(TAG, "add target, options = " + options);

        Amplify.API.put(
                ConstantUtil.CUSTOM_API_NAME,
                options,
                response ->
                {
                    Log.d(TAG, "add target succeeded, code = " + response.getCode());
                    mAddTarget.postValue(true);
                },
                error ->
                {
                    Log.e(TAG, "add target failed, error = " + error);
                    mAddTarget.postValue(false);
                }
        );
    }

    public void initCurrentIdentityId(String identityId) {
        if (identityId == null || identityId.isEmpty()) {
            Amplify.Auth.fetchAuthSession(
                    response -> {
                        AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) response;
                        String identityIdValue = cognitoAuthSession.getIdentityIdResult().getValue();
                        if (identityIdValue != null && !identityIdValue.isEmpty()) {
                            this.getIdentityId().postValue(identityIdValue);
                        }
                    },
                    error -> Log.e(TAG, "get current user error === " + error)
            );
        } else {
            mIdentityId.postValue(identityId);
        }
    }

    public void initCurrentUser(String userName) {
        if (userName == null || userName.isEmpty()) {
            Log.i(TAG, "get current user");
            Amplify.Auth.getCurrentUser(
                    response -> {
                        String user = mUser.getValue();
                        if (!response.getUsername().isEmpty()) {
                            if (!response.getUsername().equals(user)) {
                                mUser.postValue(response.getUsername());
                            }
                        }
                    },
                    error -> Log.e(TAG, "get current user error === " + error)
            );
        } else {
            mUser.postValue(userName);
        }
    }
}
