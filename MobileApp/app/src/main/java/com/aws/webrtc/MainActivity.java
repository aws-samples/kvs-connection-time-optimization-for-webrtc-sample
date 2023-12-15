//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.aws.webrtc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.amplifyframework.auth.AWSTemporaryCredentials;
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession;
import com.amplifyframework.auth.exceptions.SessionExpiredException;
import com.amplifyframework.auth.result.AuthSessionResult;
import com.amplifyframework.core.Amplify;
import com.aws.webrtc.activity.SignInActivity;
import com.aws.webrtc.datamodel.ClientConfigEntry;
import com.aws.webrtc.datamodel.ErrorInfoEntry;
import com.aws.webrtc.datamodel.LocationInfoEntry;
import com.aws.webrtc.fragment.HomeFragment;
import com.aws.webrtc.manager.MqttManager;
import com.aws.webrtc.manager.UserStateManager;
import com.aws.webrtc.manager.WebRtcManager;
import com.aws.webrtc.util.APKVersionInfoUtils;
import com.aws.webrtc.util.ConstantUtil;
import com.aws.webrtc.util.ThreadUtil;
import com.aws.webrtc.view.CustomViewPagerAdapter;
import com.aws.webrtc.viewmodel.MainViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = ConstantUtil.PREFIX + MainActivity.class.getSimpleName();

    private ViewPager2 mViewPager;
    private ConnectivityManager mConnectivityManager;
    private ConnectivityManager.NetworkCallback mNetworkCallback;

    private MainViewModel mMainViewModel;
    private CustomViewPagerAdapter mPagerAdapter;

    // for ISP
    private TelephonyManager mTelephonyManager;
    // for Location
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        fetchAuthSession();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        mViewPager = findViewById(R.id.pager);
        mPagerAdapter = new CustomViewPagerAdapter(this);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setUserInputEnabled(false);
        mViewPager.setCurrentItem(0);
        TabLayout mTabLayout = findViewById(R.id.tab_layout);
        final String[] tabs = getResources().getStringArray(R.array.tab_items);
        TabLayoutMediator mediator = new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) -> {
            tab.setText(tabs[position]);
            if (position == 0) {
                tab.setIcon(R.drawable.devices);
            } else {
                tab.setIcon(R.drawable.user);
            }
        });
        mediator.attach();

        requestPermission();
        // register DefaultNetworkCallback to observe current network status.
        registerNetworkCallback();

        // for location
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        requestLocationUpdates();
        getNetworkIp();

        // for app version
        getAppVersion();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    /**
     * It is used to fetch current auth session status.
     */
    private void fetchAuthSession() {
        Amplify.Auth.fetchAuthSession(
                value -> {
                    AWSCognitoAuthSession cognitoAuthSession = (AWSCognitoAuthSession) value;
                    Log.i(TAG, "isSignedIn = " + value.isSignedIn());
                    if (value.isSignedIn()) {
                        ThreadUtil.runOnUiThread(this::loadWebRtcView);
                        ThreadUtil.runOnUiThread(this::onIotEndPointChanged);
                        UserStateManager.getInstance().initClientConfig(this::cacheUserConfig);
                        UserStateManager.getInstance().initCurrentUser(null);
                        String identityId = cognitoAuthSession.getIdentityIdResult().getValue();
                        UserStateManager.getInstance().initCurrentIdentityId(identityId);

                        AWSTemporaryCredentials awsCredentials = (AWSTemporaryCredentials)
                                cognitoAuthSession.getAwsCredentialsResult().getValue();
                        if (awsCredentials != null) {
                            UserStateManager.getInstance().initCredential(awsCredentials);
                        }
                    } else {
                        AuthSessionResult<String> authSessionResult = cognitoAuthSession.getUserSubResult();
                        Log.d(TAG, "userSubResult = " + cognitoAuthSession.getUserSubResult());
                        Intent intent = new Intent(this, SignInActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (authSessionResult.getError() instanceof SessionExpiredException) {
                            intent.putExtra("isSessionExpired", true);
                        }
                        startActivity(intent);
                    }
                },
                error -> {
                    Intent intent = new Intent(this, SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("isSessionExpired", true);
                    startActivity(intent);
                    Log.e(TAG, "error = " + error);
                });
    }

    private void onIotEndPointChanged() {
        String iotEndpoint = UserStateManager.getInstance().getIotEndPoint().getValue();
        onAddTarget();
        if (iotEndpoint != null && !iotEndpoint.isEmpty()) {
            Log.d(TAG, "endpoint = " + iotEndpoint);
            addTarget();
        } else {
            getIotEndpoint();
            UserStateManager.getInstance().getIotEndPoint().observe(this, endpoint -> {
                if (endpoint != null && !endpoint.isEmpty()) {
                    Log.d(TAG, "onIotEndPointChanged, endpoint = " + endpoint);
                    addTarget();
                    UserStateManager.getInstance().getIotEndPoint().removeObservers(this);
                }
            });
        }
    }

    private void onAddTarget() {
        UserStateManager.getInstance().getAddTarget().observe(this, addTarget -> {
            Log.d(TAG, "onAddTarget, addTarget === " + addTarget);
            if (addTarget) {
                startMQTT();
            }
        });
    }

    private void getIotEndpoint() {
        String userName = UserStateManager.getInstance().getCurrentUserName();
        if (userName != null && !userName.isEmpty()) {
            getIotEndpointFromCache(userName);
        }
    }

    private void getIotEndpointFromCache(String userName) {
        Log.d(TAG, "getIotEndpointFromCache, userName === " + userName);
        SharedPreferences sharedPref = getSharedPreferences(userName, Context.MODE_PRIVATE);
        String iotEndPoint = sharedPref.getString("IotAtsEndpoint", "");
        if (!iotEndPoint.isEmpty()) {
            Log.d(TAG, "iotEndpoint === " + iotEndPoint);
            UserStateManager.getInstance().getIotEndPoint().postValue(iotEndPoint);
        }
    }

    private void cacheUserConfig(ClientConfigEntry clientConfigEntry) {
        String userName = UserStateManager.getInstance().getCurrentUserName();
        Log.d(TAG, "cacheUserConfig, userName === " + userName);
        if (userName != null && !userName.isEmpty() && clientConfigEntry != null) {
            Log.d(TAG, "cacheUserConfig, clientConfigEntry === " + clientConfigEntry);
            SharedPreferences sharedPref = getSharedPreferences(userName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("IotAtsEndpoint", clientConfigEntry.getIotAtsEndpoint());
            editor.apply();
        }
    }

    private void addTarget() {
        Log.d(TAG, "addTarget");
        String identityId = UserStateManager.getInstance().getIdentityId().getValue();
        String userName = UserStateManager.getInstance().getCurrentUserName();
        Boolean addTarget = UserStateManager.getInstance().getAddTarget().getValue();
        if (Boolean.FALSE.equals(addTarget)) {
            if (identityId != null && !identityId.isEmpty() && userName != null && !userName.isEmpty()) {
                UserStateManager.getInstance().addTarget(identityId, userName);
            } else {
                onIdentityIdChanged();
            }
        } else {
            Log.d(TAG, "AddTarget has already been finished");
        }
    }

    private void onIdentityIdChanged() {
        String identityId = UserStateManager.getInstance().getIdentityId().getValue();
        Boolean addTarget = UserStateManager.getInstance().getAddTarget().getValue();
        if (identityId != null && !identityId.isEmpty()) {
            if (Boolean.FALSE.equals(addTarget)) {
                onUserChanged();
            } else {
                Log.e(TAG, "has added target");
            }
        } else {
            UserStateManager.getInstance().getIdentityId().observe(this, identityIdValue -> {
                if (identityIdValue != null && !identityIdValue.isEmpty()) {
                    Log.d(TAG, "onIdentityIdChanged, identityId = " + identityIdValue);
                    onUserChanged();
                }
            });
        }
    }

    private void onUserChanged() {
        String userName = UserStateManager.getInstance().getCurrentUserName();
        if (userName != null && !userName.isEmpty()) {
            addTarget();
        } else {
            UserStateManager.getInstance().getUser().observe(this, user -> {
                if (user != null && !user.isEmpty()) {
                    Log.d(TAG, "onUserChanged, user = " + user);
                    addTarget();
                }
            });
        }
    }

    private void startMQTT() {
        Log.d(TAG, "startMQTT");
        MqttManager manager = new MqttManager(getApplicationContext());
        manager.startMqtt();
        UserStateManager.getInstance().getIdentityId().removeObservers(this);
    }

    private void loadWebRtcView() {
        Log.i(TAG, "loadWebRtcView");
        WebRtcManager mWebRtcManager = new WebRtcManager(this);
        mWebRtcManager.loadWebView();
        mMainViewModel.getWebRtcManager().postValue(mWebRtcManager);
    }

    private void registerNetworkCallback() {
        // register DefaultNetworkCallback to observe current network status.
        mConnectivityManager = getSystemService(ConnectivityManager.class);
        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "network is on available");
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.e(TAG, "network is on lost");
                UserStateManager.getInstance().setErrorInfoEntry(
                        new ErrorInfoEntry(ConstantUtil.ERROR_CODE_NO_NETWORK, ConstantUtil.ERROR_MESSAGE_NO_NETWORK));
                HomeFragment fragment = mPagerAdapter.getHomeFragment();
                if (fragment != null) {
                    fragment.stopVideo();
                }
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.d(TAG, "networkCapabilities, cellular");
                    UserStateManager.getInstance().setNetWorkType(ConstantUtil.TYPE_CELLULAR);
                    // get ISP
                    ThreadUtil.runOnUiThread(() -> getSimOperatorName());
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.d(TAG, "networkCapabilities, wifi");
                    UserStateManager.getInstance().setNetWorkType(ConstantUtil.TYPE_WIFI);
                    ThreadUtil.runOnUiThread(() -> UserStateManager.getInstance().setISP(ConstantUtil.UNKNOWN));
                }
            }

            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                Log.d(TAG, "linkProperties === " + linkProperties);
                super.onLinkPropertiesChanged(network, linkProperties);
            }
        };
        mConnectivityManager.registerDefaultNetworkCallback(mNetworkCallback);
    }

    private void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "permission has granted");
        } else {
            Log.i(TAG, "request permission");
            String[] permsInitial = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
            this.requestPermissions(permsInitial, 127);
        }
    }

    public void requestLocationUpdates() {
        Log.d(TAG, "requestLocationUpdates");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "permission not granted");
            return;
        }
        mLocationListener = location -> {
            UserStateManager.getInstance().setLocation(
                    new LocationInfoEntry(location.getLatitude(), location.getLongitude()));
            Log.d(TAG, "location === " + UserStateManager.getInstance().getLocation());
            mLocationManager.removeUpdates(mLocationListener);
        };
        mLocationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000,
                10,
                mLocationListener
        );
    }

    @SuppressLint("SwitchIntDef")
    public void getSimOperatorName() {
        mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String simOperator = mTelephonyManager.getSimOperator();
        if (mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY) {
            UserStateManager.getInstance().setISP(ConstantUtil.UNKNOWN);
            Log.e(TAG, "sim not ready");
        } else {
            Log.i(TAG, "getSimOperator() = " + simOperator);
            Log.i(TAG, "getSimOperatorName() = " + mTelephonyManager.getSimOperatorName());
            UserStateManager.getInstance().setISP(mTelephonyManager.getSimOperator());
        }
    }

    public void getNetworkIp() {
        new Thread(() -> {
            BufferedReader reader;
            HttpURLConnection connection;
            try {
                URL url = new URL(ConstantUtil.CHECK_IP_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                connection.connect();
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                InputStream inputStream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                Log.d(TAG, "network ip === " + builder);
                ThreadUtil.runOnUiThread(() ->
                        UserStateManager.getInstance().setClientIp(String.valueOf(builder)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void getAppVersion() {
        String version = APKVersionInfoUtils.getVersionName(this);
        UserStateManager.getInstance().setAppVersion(version);
    }
}