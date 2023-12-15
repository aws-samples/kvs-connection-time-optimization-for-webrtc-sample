//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult;
import com.amplifyframework.core.Amplify;
import com.aws.webrtc.R;
import com.aws.webrtc.activity.SignInActivity;
import com.aws.webrtc.manager.UserStateManager;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.util.ToastUtil;

import java.util.List;

public class UserInfoRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    private final Context mContext;
    private List<String> mDataList;
    private final String[] mUserItems;

    public UserInfoRecyclerViewAdapter(Context context, List<String> dataList) {
        this.mContext = context;
        this.mDataList = dataList;
        mUserItems = context.getResources().getStringArray(R.array.user_items);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setDataList(List<String> dataList) {
        Log.d(TAG, "setDataList");
        this.mDataList = dataList;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder");
        View layoutView;
        if (viewType == 0) {
            layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_photo, parent, false);
            return new UserPhotoViewHolder(layoutView);
        } else if (viewType == 1) {
            layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_signout, parent, false);
            return new UserSignOutViewHolder(layoutView);
        } else {
            layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_item, parent, false);
            return new UserInfoViewHolder(layoutView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        Log.d(TAG, "onBindViewHolder");
        if (viewHolder instanceof UserPhotoViewHolder) {
            UserPhotoViewHolder holder = (UserPhotoViewHolder) viewHolder;
            holder.keyTextView.setText(mUserItems[position]);
        } else if (viewHolder instanceof UserSignOutViewHolder) {
            UserSignOutViewHolder holder = (UserSignOutViewHolder) viewHolder;
            holder.signOutBtn.setOnClickListener(v -> {
                onSignOutClicked();
            });
        } else {
            UserInfoViewHolder holder = (UserInfoViewHolder) viewHolder;
            holder.keyTextView.setText(mUserItems[position]);
            if (mDataList != null && mDataList.size() >= position) {
                holder.valueTextView.setText(mDataList.get(position));
            }
        }
    }

    @Override
    public int getItemCount() {
        return mUserItems.length + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0;
        } else if (position == getItemCount() - 1) {
            return 1;
        } else {
            return 2;
        }
    }

    public static class UserInfoViewHolder extends RecyclerView.ViewHolder {

        public TextView keyTextView;
        public TextView valueTextView;

        public UserInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            keyTextView = itemView.findViewById(R.id.user_item_key);
            valueTextView = itemView.findViewById(R.id.user_item_value);
        }
    }

    public static class UserPhotoViewHolder extends RecyclerView.ViewHolder {
        public TextView keyTextView;
        public ImageView imageView;

        public UserPhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            keyTextView = itemView.findViewById(R.id.user_item_key);
            imageView = itemView.findViewById(R.id.user_item_photo);
        }
    }

    public static class UserSignOutViewHolder extends RecyclerView.ViewHolder {
        public Button signOutBtn;

        public UserSignOutViewHolder(@NonNull View itemView) {
            super(itemView);
            signOutBtn = itemView.findViewById(R.id.sign_out_button);
        }
    }

    private void onSignOutClicked() {
        Log.d(TAG, "onSignOutClicked");
        Amplify.Auth.signOut(signOutResult -> {
            if (signOutResult instanceof AWSCognitoAuthSignOutResult.CompleteSignOut) {
                // Sign Out completed fully and without errors.
                Log.i(TAG, "Signed out successfully");
                Intent intent = new Intent(mContext, SignInActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                UserStateManager.resetInstance();

                // clear cache for current user
                String userName = UserStateManager.getInstance().getCurrentUserName();
                SharedPreferences sharedPref = mContext.getSharedPreferences(userName, Context.MODE_PRIVATE);
                Log.d(TAG, "clearUserCache, userName === " + userName);
                if (sharedPref != null) {
                    sharedPref.edit().clear().apply();
                }
            } else if (signOutResult instanceof AWSCognitoAuthSignOutResult.FailedSignOut) {
                AWSCognitoAuthSignOutResult.FailedSignOut failedSignOutResult =
                        (AWSCognitoAuthSignOutResult.FailedSignOut) signOutResult;
                // Sign Out failed with an exception, leaving the user signed in.
                Log.e(TAG, "Sign out Failed", failedSignOutResult.getException());
                ToastUtil.showDialog(mContext, mContext.getString(R.string.title_activity_home),
                        failedSignOutResult.getException().getMessage(), null);
            }
        });
    }
}
