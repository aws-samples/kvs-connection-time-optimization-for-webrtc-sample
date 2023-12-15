//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.datamodel;

import androidx.annotation.NonNull;

public class LocationInfoEntry {

    public double Latitude;
    public double Longitude;

    public LocationInfoEntry() {
    }

    public LocationInfoEntry(double mLatitudeDegrees, double mLongitudeDegrees) {
        this.Latitude = mLatitudeDegrees;
        this.Longitude = mLongitudeDegrees;
    }

    @NonNull
    @Override
    public String toString() {
        return "{LatitudeDegrees: " + Latitude
                + ", LongitudeDegrees: " + Longitude + "}";
    }
}
