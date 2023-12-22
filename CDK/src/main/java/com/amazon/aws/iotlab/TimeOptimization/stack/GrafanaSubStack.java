// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization.stack;

import com.amazon.aws.iotlab.TimeOptimization.stack.base.SubStack;
import com.amazon.aws.iotlab.TimeOptimization.utils.CommonUtils;

import java.util.List;

import software.amazon.awscdk.services.grafana.CfnWorkspace;

public class GrafanaSubStack extends SubStack<AwsKvsMetricsCdkStack, GrafanaSubStack> {
    CfnWorkspace workspace;

    public GrafanaSubStack(AwsKvsMetricsCdkStack rootStack) {
        super(rootStack);
    }

    @Override
    public GrafanaSubStack create() {
        final String workspaceId = "grafana-workspace";
        workspace = CfnWorkspace.Builder.create(rootStack, workspaceId)
                .name(CommonUtils.generateName(rootStack, workspaceId))
                .accountAccessType("CURRENT_ACCOUNT")
                .authenticationProviders(List.of("AWS_SSO"))
                .dataSources(List.of("TIMESTREAM"))
                .permissionType("SERVICE_MANAGED")
                .pluginAdminEnabled(true)
                .roleArn(rootStack.identityCenterSubStack.grafanaRole.getRoleArn())
                .notificationDestinations(List.of("SNS"))
                .stackSetName("Grafana")
                .build();
        return this;
    }
}
