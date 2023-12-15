//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.util;

import android.os.Handler;
import android.os.Looper;

public class ThreadUtil {

    /**
     * Run a runnable on the Main (UI) Thread.
     * Params:  @runnable – the runnable
    * */
    public static void runOnUiThread(final Runnable runnable) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(runnable);
        } else {
            runnable.run();
        }
    }
}
