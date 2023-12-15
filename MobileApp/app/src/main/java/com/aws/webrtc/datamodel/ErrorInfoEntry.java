//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.datamodel;

/**
 * Data model.
 */
public class ErrorInfoEntry {

    public int ErrorCode;
    public String ErrorMessage;

    public ErrorInfoEntry() {
        this.ErrorMessage = "success";
    }

    public ErrorInfoEntry(int errorCode, String errorMessage) {
        ErrorCode = errorCode;
        ErrorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "ErrorInfoEntry{" +
                "ErrorCode=" + ErrorCode +
                ", ErrorMessage='" + ErrorMessage + '\'' +
                '}';
    }
}