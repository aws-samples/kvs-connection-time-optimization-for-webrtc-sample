//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.fragment;

import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aws.webrtc.R;
import com.aws.webrtc.manager.UserStateManager;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.view.UserInfoRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

public class UserAccountFragment extends BaseFragment {

    public final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();
    private UserInfoRecyclerViewAdapter mAdapter;
    private List<String> mDataList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int setLayoutResourceID() {
        return R.layout.fragment_user_account;
    }

    @Override
    protected void initView() {
        RecyclerView mRecyclerView = requireView().findViewById(R.id.user_info_layout);
        String[] items = getResources().getStringArray(R.array.user_items);
        mDataList = new ArrayList<>(getResources().getStringArray(R.array.user_items).length);
        for (int i = 0; i < items.length; i++) {
            mDataList.add("");
        }
        mAdapter = new UserInfoRecyclerViewAdapter(requireContext(), mDataList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    @Override
    protected void initEvent() {
        Log.i(TAG, "initData");
        String user = UserStateManager.getInstance().getCurrentUserName();
        if (user != null && !user.isEmpty()) {
            updateAccount(user);
        } else {
            onUserNameChanged();
        }
        String version = UserStateManager.getInstance().getAppVersion();
        Log.d(TAG, "version = " + version);
        if (version != null && !version.isEmpty()) {
            updateAppVersion(version);
        }
    }

    private void onUserNameChanged() {
        Log.d(TAG, "onUserNameChanged");
        UserStateManager.getInstance().getUser().observe((LifecycleOwner) requireContext(), user -> {
            if (user != null && !user.isEmpty()) {
                updateAccount(user);
            } else {
                Log.e(TAG, "user is null");
            }
        });
    }

    private void updateAccount(String account) {
        Log.d(TAG, "updateAccount, account === " + account);
        if (mDataList != null && mAdapter != null) {
            mDataList.set(1, account);
            mAdapter.setDataList(mDataList);
        }
    }

    private void updateAppVersion(String version) {
        Log.d(TAG, "updateAppVersion, version === " + version);
        if (mDataList != null && mAdapter != null) {
            mDataList.set(2, version);
            mAdapter.setDataList(mDataList);
        }
    }

}