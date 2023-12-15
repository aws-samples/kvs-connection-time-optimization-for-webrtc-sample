//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.datamodel;

/**
 * A client config data model.
 */
public class ClientConfigEntry {
    private final String IotAtsEndpoint;

    public ClientConfigEntry(String iotAtsEndpoint) {
        IotAtsEndpoint = iotAtsEndpoint;
    }

    public String getIotAtsEndpoint() {
        return IotAtsEndpoint;
    }
}