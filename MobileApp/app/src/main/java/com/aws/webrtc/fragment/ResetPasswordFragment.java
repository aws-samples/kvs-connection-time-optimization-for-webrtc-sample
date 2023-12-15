//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.fragment;

import android.graphics.Color;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.amplifyframework.core.Amplify;
import com.aws.webrtc.R;
import com.aws.webrtc.interfaces.NavigationHost;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.util.ToastUtil;
import com.aws.webrtc.view.SplitBackgroundDrawable;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

/**
 * A simple {@link BaseFragment} subclass.
 * This fragment is to show 'Reset Password' page and handle the user's reset password request logic.
 * Include:
 * 1. End users input username, new password and confirm code to reset password.
 */
public class ResetPasswordFragment extends BaseFragment {

    public final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    private TextInputEditText mInputUsername;
    private TextInputEditText mInputPassword;
    private TextInputEditText mInputConfirmCode;
    private Button mSetPassWordButton;

    @Override
    protected int setLayoutResourceID() {
        return R.layout.fragment_reset_password;
    }

    @Override
    protected void initView() {
        mInputUsername = requireView().findViewById(R.id.username_edit_text);
        mInputPassword = requireView().findViewById(R.id.password_edit_text);
        mInputConfirmCode = requireView().findViewById(R.id.confirm_code_edit_text);
        mSetPassWordButton = requireView().findViewById(R.id.set_password_button);
        requireView().findViewById(R.id.forget_password_layout).
                setBackground(new SplitBackgroundDrawable(getResources().getDisplayMetrics().heightPixels / 2, Color.DKGRAY));
    }

    @Override
    protected void initEvent() {
        mSetPassWordButton.setOnClickListener(v -> onSetPassWordBtnClicked());
    }

    private void onSetPassWordBtnClicked() {
        final String username = Objects.requireNonNull(mInputUsername.getText()).toString();
        final String password = Objects.requireNonNull(mInputPassword.getText()).toString();
        final String confirmCode = Objects.requireNonNull(mInputConfirmCode.getText()).toString();
        Log.d(TAG, "username = " + username);
        Log.d(TAG, "confirm code = " + confirmCode);

        if (!checkInput(username, password, confirmCode)) {
            return;
        }

        Amplify.Auth.confirmResetPassword(
                username,
                password,
                confirmCode,
                this::onSetPasswordConfirmSuccess,
                error -> Log.d(TAG, "error = " + error)
        );
    }

    protected void onSetPasswordConfirmSuccess() {
        ToastUtil.showToast(getActivity(), getString(R.string.password_change_success), Toast.LENGTH_LONG);
        ((NavigationHost) requireActivity()).navigateTo(new SignInFragment(), R.id.fragment_container, false);
    }

    private boolean checkInput(String username, String password, String confirmCode) {
        if (username.isEmpty() || password.isEmpty() || confirmCode.isEmpty()) {
            ToastUtil.showDialog(getActivity(), getString(R.string.title_activity_forgot_password),
                    getString(R.string.sign_up_confirm_info_missing), null);
            return false;
        }
        return true;
    }

}