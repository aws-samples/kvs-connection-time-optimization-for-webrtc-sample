// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization.stack;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

public class AwsKvsMetricsCdkStack extends Stack {
    CognitoSubStack cognitoSubStack;
    GrafanaSubStack grafanaSubStack;
    TimestreamSubStack timestreamSubStack;
    IoTCoreSubStack iotCoreSubStack;
    IdentityCenterSubStack identityCenterSubStack;

    public AwsKvsMetricsCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        identityCenterSubStack = new IdentityCenterSubStack(this).create();
        timestreamSubStack = new TimestreamSubStack(this).create();
        iotCoreSubStack = new IoTCoreSubStack(this).create();
        grafanaSubStack = new GrafanaSubStack(this).create();
        cognitoSubStack = new CognitoSubStack(this).create();
    }
}
