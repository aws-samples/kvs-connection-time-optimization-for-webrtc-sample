//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aws.webrtc.util.ConstantUtil;

/**
 * A simple {@link Fragment} subclass.
 * It is the base class of Fragment,
 * which encapsulates public methods and member variables.
 */
public abstract class BaseFragment extends Fragment {

    private final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    public BaseFragment() {
        // Todo: Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        return inflater.inflate(setLayoutResourceID(), container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        initView();
        initEvent();
    }

    /**
    * This method is used to return the layout file resource ID of the Fragment setting ContentView
    *
    * @return Layout file resource ID
    * */
    protected abstract int setLayoutResourceID();

    /**
     * Some View related operations
     */
    protected abstract void initView();

    /**
     * Some Event related operations
     */
    protected abstract void initEvent();

}