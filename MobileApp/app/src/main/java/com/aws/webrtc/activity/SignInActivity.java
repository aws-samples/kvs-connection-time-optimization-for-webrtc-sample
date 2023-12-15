//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.aws.webrtc.R;
import com.aws.webrtc.interfaces.NavigationHost;
import com.aws.webrtc.fragment.SignInFragment;
import com.aws.webrtc.util.ThreadUtil;

public class SignInActivity extends AppCompatActivity implements NavigationHost {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        setContentView(R.layout.activity_sign_in);
        navigateTo(new SignInFragment(intent.getBooleanExtra("isSessionExpired", false)),
                R.id.fragment_container, false);
    }

    /**
     * Navigate to the given fragment.
     *
     * @param fragment        Fragment to navigate to.
     * @param containerViewId Identifier of the container whose fragment(s) are to be replaced.
     * @param addToBackstack  Whether or not the current fragment should be added to the backstack.
     */
    @Override
    public void navigateTo(Fragment fragment, int containerViewId, boolean addToBackstack) {
        @SuppressLint("CommitTransaction") FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(containerViewId, fragment);
        transaction.disallowAddToBackStack();
        ThreadUtil.runOnUiThread(transaction::commitAllowingStateLoss);
    }
}