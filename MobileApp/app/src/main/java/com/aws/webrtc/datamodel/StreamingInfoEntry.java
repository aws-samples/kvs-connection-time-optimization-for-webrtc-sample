//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.datamodel;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

/**
 * Data model.
 */
public class StreamingInfoEntry {

    public NominatedCandidateInfoEntry NominatedCandidatePair;
    public long RequestStartTime;
    public long RequestEndTime;
    public boolean UseTurn;

    @NonNull
    @Override
    public String toString() {
        return "StreamingInfoEntry{" +
                "NominatedCandidatePair=" + new Gson().toJson(NominatedCandidatePair) +
                ", RequestStartTime=" + RequestStartTime +
                ", RequestEndTime=" + RequestEndTime +
                ", UseTurn=" + UseTurn +
                '}';
    }
}