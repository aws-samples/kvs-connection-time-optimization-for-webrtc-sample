// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization;

import com.amazon.aws.iotlab.TimeOptimization.stack.AwsKvsMetricsCdkStack;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class AwsKvsMetricsCdkApp {

    public static void main(final String[] args) {
        final App app = new App();
        new AwsKvsMetricsCdkStack(app, "AwsKvsMetricsCdkStack", StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("AWS_ACCOUNT_ID"))
                        .region(System.getenv("AWS_REGION"))
                        .build())
                .build());
        app.synth();
    }
}
