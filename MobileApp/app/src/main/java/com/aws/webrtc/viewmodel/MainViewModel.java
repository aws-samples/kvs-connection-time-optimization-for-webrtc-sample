//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.viewmodel;

import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aws.webrtc.manager.WebRtcManager;
import com.github.lzyzsd.jsbridge.BridgeWebView;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<WebRtcManager> webRtcManager = new MutableLiveData<>();
    private final MutableLiveData<View> webRtcView = new MutableLiveData<>();
    private final MutableLiveData<BridgeWebView> webRtcBridgeWebView = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPreConnectComplete = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isViewerStart = new MutableLiveData<>();

    public MutableLiveData<WebRtcManager> getWebRtcManager() {
        return webRtcManager;
    }

    public MutableLiveData<View> getWebRtcView() {
        return webRtcView;
    }

    public MutableLiveData<BridgeWebView> getWebRtcBridgeWebView() {
        return webRtcBridgeWebView;
    }

    public MutableLiveData<Boolean> getIsPreConnectComplete() {
        return isPreConnectComplete;
    }

    public MutableLiveData<Boolean> getIsViewerStart() {
        return isViewerStart;
    }
}
