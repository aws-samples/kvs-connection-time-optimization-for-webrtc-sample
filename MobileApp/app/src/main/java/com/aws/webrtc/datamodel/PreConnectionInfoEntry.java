//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.datamodel;

import androidx.annotation.NonNull;

/**
 * Data model.
 */
public class PreConnectionInfoEntry {

    public long RequestStartTime;
    public long RequestEndTime;

    @NonNull
    @Override
    public String toString() {
        return "{RequestStartTime: " + RequestStartTime
                + ", RequestEndTime: " + RequestEndTime + "}";
    }
}