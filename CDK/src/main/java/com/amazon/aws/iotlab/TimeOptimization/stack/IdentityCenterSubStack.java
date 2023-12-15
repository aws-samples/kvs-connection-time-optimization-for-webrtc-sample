// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization.stack;

import com.amazon.aws.iotlab.TimeOptimization.stack.base.SubStack;
import com.amazon.aws.iotlab.TimeOptimization.utils.CommonUtils;

import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.List;

public class IdentityCenterSubStack extends SubStack<AwsKvsMetricsCdkStack, IdentityCenterSubStack> {
    Role grafanaRole;

    public IdentityCenterSubStack(AwsKvsMetricsCdkStack rootStack) {
        super(rootStack);
    }

    @Override
    public IdentityCenterSubStack create() {
        createGrafanaRole();
        return this;
    }

    private void createGrafanaRole() {
        final String grafanaRoleId = "grafana-role";
        grafanaRole = Role.Builder.create(rootStack, grafanaRoleId)
                .roleName(CommonUtils.generateName(rootStack, grafanaRoleId))
                .assumedBy(new ServicePrincipal("grafana.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("AmazonTimestreamReadOnlyAccess")))
                .build();
    }
}
