//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.datamodel;

import androidx.annotation.NonNull;

/**
 * Data model.
 */
public class ApiInfoEntry {

    public String ApiName;
    public String ApiType;
    public long RequestStartTime;
    public long RequestEndTime;

    public ApiInfoEntry(String apiType, String apiName) {
        ApiName = apiName;
        ApiType = apiType;
    }

    @NonNull
    @Override
    public String toString() {
        return "{ApiName:" + ApiName +
                ", ApiType:" + ApiType +
                ", RequestStartTime:" + RequestStartTime +
                ", RequestEndTime:" + RequestEndTime + "}";
    }
}