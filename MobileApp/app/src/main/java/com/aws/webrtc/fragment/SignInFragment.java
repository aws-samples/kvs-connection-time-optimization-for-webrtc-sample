//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc.fragment;

import static com.amplifyframework.auth.result.step.AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import com.amplifyframework.auth.cognito.exceptions.service.PasswordResetRequiredException;
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult;
import com.amplifyframework.core.Amplify;
import com.aws.webrtc.MainActivity;
import com.aws.webrtc.R;
import com.aws.webrtc.interfaces.NavigationHost;
import com.aws.webrtc.manager.UserStateManager;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.util.ThreadUtil;
import com.aws.webrtc.util.ToastUtil;
import com.aws.webrtc.view.SplitBackgroundDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

/**
 * A simple {@link BaseFragment} subclass.
 * This fragment is to show Login page and handle the user's login request.
 * Include:
 * 1. Input username and password
 * 2. Pop up box to prompt the user when the login fails
 */
public class SignInFragment extends BaseFragment {

    public final String TAG = ConstantUtil.PREFIX + this.getClass().getSimpleName();

    private TextInputEditText mInputUsername;
    private TextInputEditText mInputPassword;
    private MaterialButton mSignInButton;
    private MaterialButton mSignUpButton;
    private MaterialButton mForgetPasswordButton;

    private boolean mIsSessionExpired;

    public SignInFragment() {

    }

    public SignInFragment(boolean isSessionExpired) {
        this.mIsSessionExpired = isSessionExpired;
    }

    @Override
    protected int setLayoutResourceID() {
        return R.layout.fragment_sign_in;
    }

    @Override
    protected void initView() {
        Log.d(TAG, "initView");
        mInputUsername = requireView().findViewById(R.id.username_edit_text);
        mInputPassword = requireView().findViewById(R.id.password_edit_text);
        mSignInButton = requireView().findViewById(R.id.sign_in_button);
        mSignUpButton = requireView().findViewById(R.id.sign_up_button);
        mForgetPasswordButton = requireView().findViewById(R.id.forget_password_button);

        requireView().findViewById(R.id.sign_in_scrollView).
                setBackground(new SplitBackgroundDrawable(getResources().getDisplayMetrics().heightPixels / 2, Color.DKGRAY));
    }

    @Override
    protected void initEvent() {
        Log.d(TAG, "initEvent");
        mSignInButton.setOnClickListener(view -> onSignInClicked());
        mSignUpButton.setOnClickListener(view -> onSignUpClicked());
        mForgetPasswordButton.setOnClickListener((view -> onForgetPasswordClicked()));
    }

    private void onSignInClicked() {
        Log.d(TAG, "onSignInClicked");
        String username = Objects.requireNonNull(mInputUsername.getText()).toString();
        String password = Objects.requireNonNull(mInputPassword.getText()).toString();
        Log.d(TAG, "userName = " + username);
        if (username.isEmpty() || password.isEmpty()) {
            ToastUtil.showDialog(getActivity(), getString(R.string.title_activity_sign_in), getString(R.string.sign_in_info_missing), null);
        } else {
            signIn(username, password);
        }
    }

    private void onSignUpClicked() {
        Log.d(TAG, "onSignUpClicked");
        ((NavigationHost) requireActivity()).navigateTo(new SignUpFragment(), R.id.fragment_container, false);
    }

    private void onForgetPasswordClicked() {
        Log.d(TAG, "onForgetPasswordClicked");
        String username = Objects.requireNonNull(mInputUsername.getText()).toString();
        if (username.isEmpty()) {
            ToastUtil.showDialog(requireContext(), getString(R.string.title_activity_sign_in), getString(R.string.sign_in_username_missing), null);
        } else {
            resetPassword(username);
        }
    }

    private void resetPassword(String userName) {
        Amplify.Auth.resetPassword(
                userName,
                result -> {
                    Log.d(TAG, "value = " + result.getNextStep());
                    if (CONFIRM_RESET_PASSWORD_WITH_CODE.equals(result.getNextStep().getResetPasswordStep())) {
                        ((NavigationHost) requireActivity()).navigateTo(
                                new ResetPasswordFragment(), R.id.fragment_container, false);
                    }
                },
                resetPasswordError -> {
                    Log.d(TAG, "resetPasswordError = " + resetPasswordError);
                    String body = resetPasswordError.getMessage() + resetPasswordError.getRecoverySuggestion();
                    ToastUtil.showDialog(getActivity(), "", body, null);
                }
        );
    }

    private void onUserSignIn(String userName, String passWord) {
        mSignInButton.setEnabled(false);
        Amplify.Auth.signIn(
                userName,
                passWord,
                result -> {
                    if (result.isSignedIn()) {
                        Log.i(TAG, "Sign in succeeded");
                        ToastUtil.showToast(getContext(), getString(R.string.prompt_sign_in_succeed), Toast.LENGTH_LONG);
                        UserStateManager.getInstance().initCurrentUser(userName);
                        Intent intent = new Intent(requireContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        Log.i(TAG, "sign in not complete");
                        Log.i(TAG, result.getNextStep().toString());
                    }
                },
                signInError -> {
                    Log.e(TAG, "signInError = " + signInError);
                    if (signInError instanceof PasswordResetRequiredException) {
                        resetPassword(userName);
                    } else {
                        ThreadUtil.runOnUiThread(() -> mSignInButton.setEnabled(true));
                        String body = signInError.getMessage() + signInError.getRecoverySuggestion();
                        Log.d(TAG, "message = " + body);
                        ToastUtil.showDialog(getActivity(), getString(R.string.title_activity_sign_in), body, null);
                    }
                }
        );
    }

    private void signIn(String usrName, String passWord) {
        if (mIsSessionExpired) {
            Amplify.Auth.signOut(signOutResult -> {
                if (signOutResult instanceof AWSCognitoAuthSignOutResult.CompleteSignOut) {
                    // Sign Out completed fully and without errors.
                    Log.d(TAG, "Signed out successfully");
                    ThreadUtil.runOnUiThread(() -> onUserSignIn(usrName, passWord));
                    // clear cache for current user
                    String userName = UserStateManager.getInstance().getCurrentUserName();
                    if (!userName.isEmpty()) {
                        SharedPreferences sharedPref = requireContext().getSharedPreferences(userName, Context.MODE_PRIVATE);
                        if (sharedPref != null) {
                            sharedPref.edit().clear().apply();
                        }
                    }
                } else if (signOutResult instanceof AWSCognitoAuthSignOutResult.FailedSignOut) {
                    AWSCognitoAuthSignOutResult.FailedSignOut failedSignOutResult =
                            (AWSCognitoAuthSignOutResult.FailedSignOut) signOutResult;
                    // Sign Out failed with an exception, leaving the user signed in.
                    Log.e(TAG, "Sign out Failed", failedSignOutResult.getException());
                    ToastUtil.showDialog(getActivity(), getString(R.string.title_activity_sign_in),
                            getString(R.string.prompt_sign_in_failed), null);
                }
            });
        } else {
            onUserSignIn(usrName, passWord);
        }
    }
}