//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.callback;

import com.aws.webrtc.datamodel.ClientConfigEntry;

public interface DataChangedCallback {

    void onDataChanged(ClientConfigEntry clientConfigEntry);
}