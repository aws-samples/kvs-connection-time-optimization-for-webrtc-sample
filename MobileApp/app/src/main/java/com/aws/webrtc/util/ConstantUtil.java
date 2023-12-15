//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.util;

/**
 * This class is to provide the unified management of constant.
 */
public class ConstantUtil {

    public static final String PREFIX = "Iot.lab.";

    public static final String CUSTOM_API_NAME = "end-user-api";

    public static final String TYPE_API = "API";
    public static final String TYPE_PRECONNECTION = "PreConnection";
    public static final String TYPE_STREAMING = "Streaming";

    public static final String TIMESTAMPS_START = "start";
    public static final String TIMESTAMPS_END = "end";

    public static final String TYPE_REMOTE = "remote";
    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_RELAY = "relay";

    public static final String TYPE_WIFI = "WIFI";
    public static final String TYPE_CELLULAR = "Cellular";

    public static final String CHECK_IP_URL = "https://checkip.amazonaws.com/";

    public static final int ERROR_CODE_SDP_TIMEOUT = 1;
    public static final int ERROR_CODE_USER_CANCEL = 2;
    public static final int ERROR_CODE_NO_NETWORK = 3;
    public static final int ERROR_CODE_CONNECTION_FAILED = 4;

    public static final String ERROR_MESSAGE_SDP_TIMEOUT = "sdp answer time out";
    public static final String ERROR_MESSAGE_USER_CANCEL = "user cancel";
    public static final String ERROR_MESSAGE_NO_NETWORK = "no network";
    public static final String ERROR_MESSAGE_CONNECTION_FAILED = "peer connection failed";

    public static final String UNKNOWN = "unknown";
    public static final int API_LIST_MAX_NUMBER = 5;
    public static final String API_DESCRIBE_SIGNALING_CHANNEL = "describeSignalingChannel";
    public static final String API_GET_SIGNALING_CHANNEL_ENDPOINT = "getSignalingChannelEndpoint";
    public static final String API_GET_ICE_SERVER_CONFIG = "getIceServerConfig";
    public static final String API_CONNECT_AS_VIEWER = "ConnectAsViewer";
    public static final String API_SDP = "SDP";
}
