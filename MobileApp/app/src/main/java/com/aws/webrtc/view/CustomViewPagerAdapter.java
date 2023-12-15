//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.view;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.aws.webrtc.fragment.HomeFragment;
import com.aws.webrtc.fragment.UserAccountFragment;

public class CustomViewPagerAdapter extends FragmentStateAdapter {

    private HomeFragment mHomeFragment;

    public CustomViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            mHomeFragment = new HomeFragment();
            return mHomeFragment;
        } else {
            return new UserAccountFragment();
        }
    }

    public HomeFragment getHomeFragment() {
        return mHomeFragment;
    }
}
