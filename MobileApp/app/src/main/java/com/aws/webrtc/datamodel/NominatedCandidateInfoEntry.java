//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.datamodel;

/**
 * Data model.
 */
public class NominatedCandidateInfoEntry {

    public CandidateInfoEntry local;
    public CandidateInfoEntry remote;

    public void setLocal(CandidateInfoEntry local) {
        this.local = local;
    }

    public void setRemote(CandidateInfoEntry remote) {
        this.remote = remote;
    }

    @Override
    public String toString() {
        return "NominatedCandidateInfoEntry{" +
                "local=" + local +
                ", remote=" + remote +
                '}';
    }
}