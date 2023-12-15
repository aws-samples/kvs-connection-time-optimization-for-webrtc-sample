//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import com.aws.webrtc.R;

/**
 * This class is to provide the unified management of Toast.
 * It can easily and quickly pop up Toast.
 * There are the following advantages:
 * 1. There is only one Toast instance globally to avoid frequent pop-ups
 * 2. Sub-threads can directly call
 */
public final class ToastUtil {

    private static Toast mToast;

    public static void showToast(Context context, CharSequence msg, int duration) {
        ThreadUtil.runOnUiThread(() -> obtainAndShowToast(context, msg, duration));
    }

    private static void obtainAndShowToast(final Context context, final CharSequence msg, final int duration) {
        if (!((Activity) context).isFinishing()) {
            if (mToast == null) {
                mToast = Toast.makeText(context.getApplicationContext(), msg, duration);
            } else {
                mToast.setText(msg);
                mToast.setDuration(duration);
            }
            mToast.show();
        }
    }

    /**
     * Displays a modal dialog with an OK button.
     * Params:
     *
     * @context – context
     * @title – title to display for the dialog
     * @body – content of the dialog
     * @listener – listener for button click event
     */
    public static void showDialog(
            final Context context,
            final String title,
            final String body,
            DialogInterface.OnClickListener listener) {
        ThreadUtil.runOnUiThread(() -> {
            if (null == context) {
                return;
            }

            if (!((Activity) context).isFinishing()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(body);
                builder.setNeutralButton(context.getString(R.string.confirm), listener);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }
}
