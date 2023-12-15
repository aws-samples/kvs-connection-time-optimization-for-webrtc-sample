//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.aws.webrtc.R;
import com.aws.webrtc.datamodel.ApiInfoEntry;
import com.aws.webrtc.datamodel.ErrorInfoEntry;
import com.aws.webrtc.datamodel.LocationInfoEntry;
import com.aws.webrtc.datamodel.WebRtcMetrics;
import com.aws.webrtc.manager.JsAndroidCommManager;
import com.aws.webrtc.manager.UserStateManager;
import com.aws.webrtc.manager.WebRtcManager;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.util.ThreadUtil;
import com.aws.webrtc.viewmodel.MainViewModel;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Objects;

/**
 * A simple {@link BaseFragment} subclass.
 * This fragment to show home page and interact with users.
 * Include:
 * 1. Show user's device list
 * 2. A pop-up box asks the user whether to upload a log file
 * 3. Start a chat with Agent to ask for help
 */
public class HomeFragment extends BaseFragment {

    private final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    private Button mWebRtcBtn;
    /**
     * The state of button is On or Off
     * */
    private boolean mFlag = false;

    private MainViewModel mMainViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        stopVideo();
    }

    @Override
    protected int setLayoutResourceID() {
        return R.layout.fragment_home;
    }

    @Override
    protected void initView() {
        mWebRtcBtn = requireView().findViewById(R.id.webrtc_btn);
        FrameLayout mFrameLayout = requireView().findViewById(R.id.fragment_container);

        mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        View view = mMainViewModel.getWebRtcView().getValue();
        if (view != null) {
            Log.i(TAG, "add view");
            mFrameLayout.addView(view);
        } else {
            mMainViewModel.getWebRtcView().observe(this, webRtcView -> {
                if (webRtcView != null) {
                    Log.i(TAG, "add view");
                    ThreadUtil.runOnUiThread(() -> mFrameLayout.addView(webRtcView));
                }
            });
        }
    }

    @SuppressLint({"InflateParams"})
    @Override
    protected void initEvent() {
        mMainViewModel.getIsPreConnectComplete().observe(this, isPreConnectComplete -> {
            if (isPreConnectComplete && mWebRtcBtn != null) {
                mWebRtcBtn.setEnabled(true);
            }
        });
        mMainViewModel.getIsViewerStart().observe(this, isViewerStart -> {
            if (Boolean.FALSE.equals(isViewerStart)) {
                stopVideo();
            }
        });
        mWebRtcBtn.setOnClickListener(v -> {
            mFlag = !mFlag;
            Log.d(TAG, "webrtc button clicked, flag === " + mFlag);
            startView(mFlag);
        });
    }

    private void startView(boolean start) {
        if (start) {
            mWebRtcBtn.setText(R.string.stop_webrtc);
            // clear cached data in memory
            UserStateManager.getInstance().setErrorInfoEntry(null);
            ApiInfoEntry[] apiArray = UserStateManager.getInstance().getWebRtcMetrics().getAPI();
            if (apiArray != null && apiArray.length == ConstantUtil.API_LIST_MAX_NUMBER) {
                apiArray[4] = null;
            }
        } else {
            mWebRtcBtn.setText(R.string.start_webrtc);
            // publish metrics by MQTT
            WebRtcMetrics webRtcMetrics = UserStateManager.getInstance().getWebRtcMetrics();
            if (webRtcMetrics != null) {
                String identityId = UserStateManager.getInstance().getIdentityId().getValue();
                String userName = UserStateManager.getInstance().getUser().getValue();
                String appVersion = UserStateManager.getInstance().getAppVersion();
                webRtcMetrics.setAppVersion(appVersion);
                webRtcMetrics.setUserId(identityId);
                webRtcMetrics.setUserName(userName);
                webRtcMetrics.setNetworkType(UserStateManager.getInstance().getNetWorkType());
                webRtcMetrics.setClientIp(UserStateManager.getInstance().getClientIp());
                LocationInfoEntry locationInfoEntry = UserStateManager.getInstance().getLocation();
                if (locationInfoEntry == null) {
                    locationInfoEntry = new LocationInfoEntry();
                }
                webRtcMetrics.setLocation(locationInfoEntry);
                webRtcMetrics.setISP(UserStateManager.getInstance().getISP());
                if (!UserStateManager.getInstance().isStreamingSuccess()) {
                    Log.d(TAG, "[metrics upload] isStreamingSuccess false");
                    ErrorInfoEntry entry = UserStateManager.getInstance().getErrorInfoEntry();
                    if (entry != null) {
                        webRtcMetrics.setError(entry);
                    } else {
                        webRtcMetrics.setError(
                                new ErrorInfoEntry(ConstantUtil.ERROR_CODE_USER_CANCEL, ConstantUtil.ERROR_MESSAGE_USER_CANCEL));
                    }
                } else {
                    webRtcMetrics.setError(new ErrorInfoEntry());
                }
                Log.d(TAG, "[metrics upload] === " + new Gson().toJson(webRtcMetrics));
                // filter null in API Array
                ApiInfoEntry[] apiArray = Arrays.stream(webRtcMetrics.getAPI()).filter(Objects::nonNull).toArray(ApiInfoEntry[]::new);
                webRtcMetrics.setAPI(apiArray);
                Log.d(TAG, "[metrics upload] api length = " + webRtcMetrics.getAPI().length);
                JsAndroidCommManager.getInstance().publishUploadDataRequest(new Gson().toJson(webRtcMetrics));
            }
        }
        WebRtcManager webRtcManager = mMainViewModel.getWebRtcManager().getValue();
        if (webRtcManager != null) {
            webRtcManager.startVideo(start);
        }
    }

    public void stopVideo() {
        if (mFlag) {
            startView(false);
            mFlag = false;
        }
    }
}