//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.datamodel;

import com.aws.webrtc.util.ConstantUtil;

/**
 * Data model.
 */
public class WebRtcMetrics {

    private String AppVersion;
    private String UserId;
    private String UserName;
    private String NetworkType;
    private String ClientIp;
    private ErrorInfoEntry Error;
    private LocationInfoEntry Location;
    private String ISP;
    private ApiInfoEntry[] API;
    private PreConnectionInfoEntry PreConnection;
    private StreamingInfoEntry Streaming;

    public WebRtcMetrics() {
        // assign default value
        this.AppVersion = ConstantUtil.UNKNOWN;
        this.UserId = ConstantUtil.UNKNOWN;
        this.NetworkType = ConstantUtil.UNKNOWN;
        this.ClientIp = ConstantUtil.UNKNOWN;
        this.ISP = ConstantUtil.UNKNOWN;
        this.API = new ApiInfoEntry[ConstantUtil.API_LIST_MAX_NUMBER];
        this.PreConnection = new PreConnectionInfoEntry();
        this.Streaming = new StreamingInfoEntry();
    }

    public String getAppVersion() {
        return AppVersion;
    }

    public void setAppVersion(String appVersion) {
        AppVersion = appVersion;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getNetworkType() {
        return NetworkType;
    }

    public void setNetworkType(String networkType) {
        NetworkType = networkType;
    }

    public String getClientIp() {
        return ClientIp;
    }

    public void setClientIp(String clientIp) {
        ClientIp = clientIp;
    }

    public ErrorInfoEntry getError() {
        return Error;
    }

    public void setError(ErrorInfoEntry error) {
        Error = error;
    }
    public LocationInfoEntry getLocation() {
        return Location;
    }

    public void setLocation(LocationInfoEntry location) {
        Location = location;
    }

    public String getISP() {
        return ISP;
    }

    public void setISP(String ISP) {
        this.ISP = ISP;
    }

    public ApiInfoEntry[] getAPI() {
        return API;
    }

    public void setAPI(ApiInfoEntry[] API) {
        this.API = API;
    }

    public PreConnectionInfoEntry getPreConnection() {
        return PreConnection;
    }

    public void setPreConnection(PreConnectionInfoEntry preConnection) {
        PreConnection = preConnection;
    }

    public StreamingInfoEntry getStreaming() {
        return Streaming;
    }

    public void setStreaming(StreamingInfoEntry streaming) {
        Streaming = streaming;
    }
}