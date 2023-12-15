//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.aws.webrtc.R;
import com.aws.webrtc.datamodel.ApiInfoEntry;
import com.aws.webrtc.datamodel.CandidateInfoEntry;
import com.aws.webrtc.datamodel.ErrorInfoEntry;
import com.aws.webrtc.datamodel.NominatedCandidateInfoEntry;
import com.aws.webrtc.datamodel.PreConnectionInfoEntry;
import com.aws.webrtc.datamodel.StreamingInfoEntry;
import com.aws.webrtc.datamodel.WebRtcMetrics;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.util.ThreadUtil;
import com.aws.webrtc.util.ToastUtil;
import com.aws.webrtc.viewmodel.MainViewModel;
import com.github.lzyzsd.jsbridge.BridgeWebView;

import java.util.Arrays;
import java.util.List;

public class WebRtcManager {

    private final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    private final Context mContext;
    private final BridgeWebView mBridgeWebView;
    private final ProgressBar mLoading;
    private final MainViewModel mMainViewModel;

    private ApiInfoEntry mApiInfoEntry;
    private final NominatedCandidateInfoEntry mNominatedCandidatePair = new NominatedCandidateInfoEntry();

    @SuppressLint("InflateParams")
    public WebRtcManager(Context context) {
        Log.i(TAG, "WebRtcManager");
        mContext = context;
        View mRootView = LayoutInflater.from(context).inflate(R.layout.fragment_web_cam, null);
        mBridgeWebView = mRootView.findViewById(R.id.webrtc_webView);
        mLoading = mRootView.findViewById(R.id.video_loading);

        mMainViewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(MainViewModel.class);
        mMainViewModel.getWebRtcView().postValue(mRootView);
        mMainViewModel.getWebRtcBridgeWebView().postValue(mBridgeWebView);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void loadWebView() {
        Log.i(TAG, "loadWebView");
        mBridgeWebView.setWebChromeClient(new WebViewChromeClient());
        WebSettings settings = mBridgeWebView.getSettings();
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setDomStorageEnabled(true);
        mBridgeWebView.loadUrl("file:///android_asset/kvs-webrtc/index.html");
    }

    private class WebViewChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            Log.d(TAG, "newProgress (webrtc) === " + newProgress);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d(TAG, "[webrtc web-view]" + consoleMessage.message());
            if (consoleMessage.message().contains("[metrics]")) {
                String message = consoleMessage.message();
                String[] messageArray = message.split(",");
                if (messageArray.length >= 5) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (ConstantUtil.TYPE_API.equals(messageArray[1])) {
                        if (ConstantUtil.TIMESTAMPS_START.equals(messageArray[4])) {
                            mApiInfoEntry = new ApiInfoEntry(messageArray[2], messageArray[3]);
                            mApiInfoEntry.RequestStartTime = currentTimeMillis;
                            updateApiList(mApiInfoEntry);
                        } else if (ConstantUtil.TIMESTAMPS_END.equals(messageArray[4])) {
                            mApiInfoEntry.RequestEndTime = currentTimeMillis;
                            Log.d(TAG, "[metrics] apiInfoEntry === " + mApiInfoEntry);
                            if (UserStateManager.getInstance().getWebRtcMetrics() == null) {
                                UserStateManager.getInstance().setWebRtcMetrics(new WebRtcMetrics());
                            }
                            updateApiList(mApiInfoEntry);
                        }
                    } else if (ConstantUtil.TYPE_PRECONNECTION.equals(messageArray[1])) {
                        if (ConstantUtil.TIMESTAMPS_START.equals(messageArray[4])) {
                            PreConnectionInfoEntry mPreConnectionInfoEntry = new PreConnectionInfoEntry();
                            mPreConnectionInfoEntry.RequestStartTime = currentTimeMillis;
                            if (UserStateManager.getInstance().getWebRtcMetrics() == null) {
                                UserStateManager.getInstance().setWebRtcMetrics(new WebRtcMetrics());
                            }
                            UserStateManager.getInstance().getWebRtcMetrics().setPreConnection(mPreConnectionInfoEntry);
                        } else if (ConstantUtil.TIMESTAMPS_END.equals(messageArray[4])) {
                            mMainViewModel.getIsPreConnectComplete().postValue(true);
                            if (UserStateManager.getInstance().getWebRtcMetrics() == null ||
                                    UserStateManager.getInstance().getWebRtcMetrics().getPreConnection() == null) {
                                Log.e(TAG, "start time is null");
                            } else {
                                UserStateManager.getInstance().getWebRtcMetrics().getPreConnection().RequestEndTime = currentTimeMillis;
                                Log.d(TAG, "[metrics] mPreConnectionInfoEntry === " + UserStateManager.getInstance().getWebRtcMetrics().getPreConnection());
                            }
                        }
                    } else if (ConstantUtil.TYPE_STREAMING.equals(messageArray[1])) {
                        if (ConstantUtil.TIMESTAMPS_END.equals(messageArray[4])) {
                            if (UserStateManager.getInstance().getWebRtcMetrics() == null ||
                                    UserStateManager.getInstance().getWebRtcMetrics().getStreaming() == null) {
                                Log.e(TAG, "start time is null");
                            } else {
                                UserStateManager.getInstance().getWebRtcMetrics().getStreaming().RequestEndTime = currentTimeMillis;
                                Log.d(TAG, "[metrics] mStreamingInfoEntry === " + UserStateManager.getInstance().getWebRtcMetrics().getStreaming());
                            }
                        } else if (messageArray.length >= 7) {
                            if (ConstantUtil.TYPE_REMOTE.equals(messageArray[2])) {
                                CandidateInfoEntry candidateInfoEntry = new CandidateInfoEntry();
                                candidateInfoEntry.ipProtocal = messageArray[3];
                                candidateInfoEntry.addr = messageArray[4];
                                candidateInfoEntry.raddr = messageArray[5];
                                candidateInfoEntry.type = messageArray[6];
                                mNominatedCandidatePair.remote = candidateInfoEntry;
                            } else if (ConstantUtil.TYPE_LOCAL.equals(messageArray[2])) {
                                CandidateInfoEntry candidateInfoEntry = new CandidateInfoEntry();
                                candidateInfoEntry.ipProtocal = messageArray[3];
                                candidateInfoEntry.addr = messageArray[4];
                                candidateInfoEntry.raddr = messageArray[5];
                                candidateInfoEntry.type = messageArray[6];
                                mNominatedCandidatePair.local = candidateInfoEntry;
                                if (UserStateManager.getInstance().getWebRtcMetrics() == null ||
                                        UserStateManager.getInstance().getWebRtcMetrics().getStreaming() == null) {
                                    Log.e(TAG, "start time is null");
                                } else {
                                    UserStateManager.getInstance().getWebRtcMetrics().getStreaming().NominatedCandidatePair = mNominatedCandidatePair;
                                    String localType = UserStateManager.getInstance().getWebRtcMetrics().getStreaming().NominatedCandidatePair.local.type;
                                    String remoteType = UserStateManager.getInstance().getWebRtcMetrics().getStreaming().NominatedCandidatePair.remote.type;
                                    if (ConstantUtil.TYPE_RELAY.equals(localType) ||
                                            ConstantUtil.TYPE_RELAY.equals(remoteType)) {
                                        UserStateManager.getInstance().getWebRtcMetrics().getStreaming().UseTurn = true;
                                    }
                                    Log.d(TAG, "[metrics] mNominatedCandidatePair === " + mNominatedCandidatePair);
                                }
                            }
                        }
                    }
                }
            } else if (consoleMessage.message().contains("video play")) {
                UserStateManager.getInstance().setStreamingSuccess(true);
                mLoading.setVisibility(View.GONE);
            } else if (consoleMessage.message().contains("toast")) {
                String[] array = consoleMessage.message().split(":");
                String message = array[1];
                // show dialog
                ToastUtil.showDialog(mContext, "", message, null);
                mMainViewModel.getIsViewerStart().postValue(false);
                ThreadUtil.runOnUiThread(() -> {
                    if (mLoading != null) { mLoading.setVisibility(View.GONE);}
                });
                String type = array[2].strip();
                Log.d(TAG, "error type = " + type);
                switch (type) {
                    case "1":
                        Log.d(TAG, "error message = " + ConstantUtil.ERROR_MESSAGE_SDP_TIMEOUT);
                        UserStateManager.getInstance().setErrorInfoEntry(
                                new ErrorInfoEntry(ConstantUtil.ERROR_CODE_SDP_TIMEOUT, ConstantUtil.ERROR_MESSAGE_SDP_TIMEOUT));
                        break;
                    case "2":
                        Log.d(TAG, "error message = " + ConstantUtil.ERROR_MESSAGE_CONNECTION_FAILED);
                        UserStateManager.getInstance().setErrorInfoEntry(
                                new ErrorInfoEntry(ConstantUtil.ERROR_CODE_CONNECTION_FAILED, ConstantUtil.ERROR_MESSAGE_CONNECTION_FAILED));
                        break;
                    default:
                        break;
                }
            }
            return super.onConsoleMessage(consoleMessage);
        }

        @Nullable
        @Override
        public Bitmap getDefaultVideoPoster() {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
    }

    private void updateApiList(ApiInfoEntry mApiInfoEntry) {
        ApiInfoEntry[] apiList = UserStateManager.getInstance().getWebRtcMetrics().getAPI();
        if (apiList == null) {
            apiList = new ApiInfoEntry[ConstantUtil.API_LIST_MAX_NUMBER];
        } else if (apiList.length < ConstantUtil.API_LIST_MAX_NUMBER) {
            apiList = Arrays.copyOf(apiList, ConstantUtil.API_LIST_MAX_NUMBER);
        }
        switch (mApiInfoEntry.ApiName) {
            case ConstantUtil.API_DESCRIBE_SIGNALING_CHANNEL:
                apiList[0] = mApiInfoEntry;
                break;
            case ConstantUtil.API_GET_SIGNALING_CHANNEL_ENDPOINT:
                apiList[1] = mApiInfoEntry;
                break;
            case ConstantUtil.API_GET_ICE_SERVER_CONFIG:
                apiList[2] = mApiInfoEntry;
                break;
            case ConstantUtil.API_CONNECT_AS_VIEWER:
                apiList[3] = mApiInfoEntry;
                break;
            case ConstantUtil.API_SDP:
                apiList[4] = mApiInfoEntry;
                break;
            default:
                Log.e(TAG, "mApiInfoEntry = " + mApiInfoEntry);
                break;
        }
    }

    public void startVideo(boolean isStart) {
        Log.i(TAG, "startVideo, isStart === " + isStart);
        if (mBridgeWebView != null) {
            if (isStart) {
                UserStateManager.getInstance().setStreamingSuccess(false);
                StreamingInfoEntry mStreamingInfoEntry = new StreamingInfoEntry();
                mStreamingInfoEntry.RequestStartTime = System.currentTimeMillis();
                if (UserStateManager.getInstance().getWebRtcMetrics() == null) {
                    UserStateManager.getInstance().setWebRtcMetrics(new WebRtcMetrics());
                }
                UserStateManager.getInstance().getWebRtcMetrics().setStreaming(mStreamingInfoEntry);
                ThreadUtil.runOnUiThread(() -> {
                    if (mLoading != null) {
                        mLoading.setVisibility(View.VISIBLE);
                    }
                    mBridgeWebView.callHandler("startViewer", "", null);
                });
            } else {
                ThreadUtil.runOnUiThread(() -> {
                    if (mLoading != null) {
                        mLoading.setVisibility(View.GONE);
                    }
                    mBridgeWebView.callHandler("stopViewer", "", null);
                });
            }
        }
    }
}
