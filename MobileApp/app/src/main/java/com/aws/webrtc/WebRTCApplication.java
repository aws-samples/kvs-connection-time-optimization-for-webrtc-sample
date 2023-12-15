//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc;

import android.app.Application;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.aws.webrtc.util.ConstantUtil;

public class WebRTCApplication extends Application {

    private final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Amplify.addPlugin(new AWSApiPlugin());
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(getApplicationContext());

            Log.i(TAG, "Initialized Amplify.");
        } catch (AmplifyException e) {
            e.printStackTrace();
            Log.e(TAG, "Could not initialize Amplify. " + e);
        }

        Log.d(TAG, "onCreate");
    }
}
