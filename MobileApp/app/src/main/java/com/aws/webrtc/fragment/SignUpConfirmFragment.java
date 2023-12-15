//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.fragment;

import android.graphics.Color;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.amplifyframework.auth.result.AuthSignUpResult;
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
 * This fragment is to show 'Confirm Account' page and handle the user's confirm code request logic.
 * Include:
 * 1. End users input username and confirm code to confirm the account.
 * 2. If the verification is successful, jump to the sign in interface.
 * 3. If the verification is unsuccessful, or the username and confirm code are empty, a pop-up error box will prompt the user.
 */
public class SignUpConfirmFragment extends BaseFragment {

    public final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    private TextInputEditText mInputUsername;
    private TextInputEditText mInputConfirmCode;
    private Button mSignUpConfirmButton;

    @Override
    protected int setLayoutResourceID() {
        return R.layout.fragment_sign_up_confirm;
    }

    @Override
    protected void initView() {
        mInputUsername = requireView().findViewById(R.id.username_edit_text);
        mInputConfirmCode = requireView().findViewById(R.id.confirm_code_edit_text);
        mSignUpConfirmButton = requireView().findViewById(R.id.confirm_account_button);
        requireView().findViewById(R.id.sign_up_confirm_layout).
                setBackground(new SplitBackgroundDrawable(getResources().getDisplayMetrics().heightPixels / 2, Color.DKGRAY));
    }

    @Override
    protected void initEvent() {
        mSignUpConfirmButton.setOnClickListener(v -> onSignUpConfirmClicked());
    }

    private void onSignUpConfirmClicked() {
        final String username = Objects.requireNonNull(mInputUsername.getText()).toString();
        final String confirmCode = Objects.requireNonNull(mInputConfirmCode.getText()).toString();
        Log.d(TAG, "username = " + username);
        Log.d(TAG, "confirm code = " + confirmCode);

        if (!checkInput(username, confirmCode)) {
            return;
        }

        Amplify.Auth.confirmSignUp(
                username,
                confirmCode,
                this::onSignUpConfirmSuccess,
                error -> ToastUtil.showDialog(getActivity(),
                        getString(R.string.title_activity_sign_up_confirm),
                        error.getMessage() + error.getRecoverySuggestion(), null)
        );
    }

    protected void onSignUpConfirmSuccess(AuthSignUpResult result) {
        Log.i(TAG, result.isSignUpComplete() ? getString(R.string.prompt_sign_up_succeed) : getString(R.string.prompt_sign_up_failed));
        ToastUtil.showToast(getActivity(), getString(R.string.prompt_sign_up_succeed), Toast.LENGTH_LONG);
        ((NavigationHost) requireActivity()).navigateTo(new SignInFragment(), R.id.fragment_container, false);
    }

    private boolean checkInput(String username, String confirmCode) {
        if (username.isEmpty() || confirmCode.isEmpty()) {
            ToastUtil.showDialog(getActivity(), getString(R.string.title_activity_sign_up_confirm),
                    getString(R.string.sign_up_confirm_info_missing), null);
            return false;
        }
        return true;
    }

}