//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.fragment;

import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.result.step.AuthSignUpStep;
import com.amplifyframework.core.Amplify;
import com.aws.webrtc.R;
import com.aws.webrtc.interfaces.NavigationHost;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.util.ToastUtil;
import com.aws.webrtc.view.SplitBackgroundDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link BaseFragment} subclass.
 * This fragment is to show Register page and handle the user's register request.
 * Including:
 * 1. Input username, password, confirm password, and email address.
 * 2. Click the sign up button to register an account.
 * 3. If the username, password or email are empty, an error message will pop up to the end user.
 * 4. If the password entered for the first time does not match the confirm password, an error message will pop up to the end user.
 * 5. If the user's sign up step is not completed, jump to the verification interface.
 * 6. If the user's sign up step is completed successfully, jump to the sign in interface.
 * 7. If the user sign up fails, an error message will pop up to the end user.
 */
public class SignUpFragment extends BaseFragment {

    public final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    private TextInputEditText mInputUsername;
    private TextInputEditText mInputPassword;
    private TextInputEditText mInputConfirmPassword;
    private TextInputEditText mEmailAddress;
    private MaterialButton mSignUpButton;

    @Override
    protected int setLayoutResourceID() {
        return R.layout.fragment_sign_up;
    }

    @Override
    protected void initView() {
        mSignUpButton = requireView().findViewById(R.id.sign_up_button);
        mInputUsername = requireView().findViewById(R.id.username_edit_text);
        mInputPassword = requireView().findViewById(R.id.password_edit_text);
        mInputConfirmPassword = requireView().findViewById(R.id.password_confirm_edit_text);
        mEmailAddress = requireView().findViewById(R.id.email_edit_text);
        requireView().findViewById(R.id.sign_up_scrollView).
                setBackground(new SplitBackgroundDrawable(getResources().getDisplayMetrics().heightPixels / 2, Color.DKGRAY));

    }

    @Override
    protected void initEvent() {
        mSignUpButton.setOnClickListener(v -> onUserSignUpClicked());
    }

    private void onUserSignUpClicked() {
        String username = Objects.requireNonNull(mInputUsername.getText()).toString();
        String password = Objects.requireNonNull(mInputPassword.getText()).toString();
        String confirmPassword = Objects.requireNonNull(mInputConfirmPassword.getText()).toString();
        String email = Objects.requireNonNull(mEmailAddress.getText()).toString();
        if (!checkInput(username, password, email) || !checkPassword(password, confirmPassword)) {
            return;
        }
        Log.d(TAG, "userName = " + username);
        Log.d(TAG, "email = " + email);
        List<AuthUserAttribute> userAttributes = new ArrayList<>();
        userAttributes.add(new AuthUserAttribute(AuthUserAttributeKey.email(), email));
        userAttributes.add(new AuthUserAttribute(AuthUserAttributeKey.familyName(), username));
        userAttributes.add(new AuthUserAttribute(AuthUserAttributeKey.givenName(), username));
        userAttributes.add(new AuthUserAttribute(AuthUserAttributeKey.preferredUsername(), username));
        AuthSignUpOptions options = AuthSignUpOptions.builder()
                .userAttributes(userAttributes)
                .build();
        Amplify.Auth.signUp(
                username, password, options,
                value -> {
                    Log.d(TAG, "value = " + value);
                    if (!value.isSignUpComplete()) {
                        AuthSignUpStep authSignUpStep = value.getNextStep().getSignUpStep();
                        if (authSignUpStep == AuthSignUpStep.CONFIRM_SIGN_UP_STEP) {
                            ((NavigationHost) requireActivity()).navigateTo(new SignUpConfirmFragment(), R.id.fragment_container, false);
                        }
                    } else {
                        ToastUtil.showToast(getActivity(), getString(R.string.prompt_sign_up_succeed), Toast.LENGTH_LONG);
                        ((NavigationHost) requireActivity()).navigateTo(new SignInFragment(), R.id.fragment_container, false);
                    }
                },
                error -> ToastUtil.showDialog(getActivity(), getString(R.string.title_activity_sign_up), error.getMessage() + error.getRecoverySuggestion(), null)
        );
    }

    private boolean checkPassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            ToastUtil.showDialog(getActivity(), getString(R.string.title_activity_sign_up), getString(R.string.confirm_password_not_same), null);
            return false;
        }
        return true;
    }

    private boolean checkInput(String username, String password, String email) {
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            ToastUtil.showDialog(getActivity(), getString(R.string.title_activity_sign_up), getString(R.string.sign_up_info_missing), null);
            return false;
        }
        return true;
    }
}