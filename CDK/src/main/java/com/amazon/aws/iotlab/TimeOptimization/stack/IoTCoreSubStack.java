// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization.stack;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import com.amazon.aws.iotlab.TimeOptimization.constant.Constant;
import com.amazon.aws.iotlab.TimeOptimization.stack.base.SubStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.amazon.aws.iotlab.TimeOptimization.utils.CommonUtils;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.customresources.AwsCustomResource;
import software.amazon.awscdk.customresources.AwsCustomResourcePolicy;
import software.amazon.awscdk.customresources.AwsSdkCall;
import software.amazon.awscdk.customresources.PhysicalResourceId;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.iot.CfnPolicy;
import software.amazon.awscdk.services.iot.CfnTopicRule;

public class IoTCoreSubStack extends SubStack<AwsKvsMetricsCdkStack, IoTCoreSubStack> {
    CfnPolicy mobileRegistrationPolicy;
    String iotAtsEndpoint;
    Role iotRuleEngineRole;
    CfnTopicRule iotRule;

    public IoTCoreSubStack(AwsKvsMetricsCdkStack rootStack) {
        super(rootStack);
    }

    @Override
    public IoTCoreSubStack create() {
        createMobileRegistrationPolicy();
        createRule();
        createEndpointResource();
        return this;
    }

    private void createMobileRegistrationPolicy() {
        // mobile app MQTT policy
        PolicyStatement mobileConnectStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(singletonList("iot:Connect"))
                .resources(singletonList(
                        // This wildcard is required since mobile app client id has a random postfix
                        String.format("arn:aws:iot:%s:%s:client/${cognito-identity.amazonaws.com:sub}/*",
                                rootStack.getRegion(), rootStack.getAccount())))
                .build();

        final String mobileTopicPrefix = String.format(
                "arn:aws:iot:%s:%s:topic/${cognito-identity.amazonaws.com:sub}", rootStack.getRegion(), rootStack.getAccount());
        PolicyStatement mobilePublishStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("iot:Publish"))
                .resources(singletonList(String.format("%s/request/metrics/webrtc", mobileTopicPrefix)))
                .build();

        final PolicyDocument mobileRegistrationPolicyStatement = PolicyDocument.Builder.create()
                .statements(List.of(mobileConnectStatement, mobilePublishStatement))
                .build();

        final String mobileRegistrationPolicyId = "mobile-registration-policy";
        mobileRegistrationPolicy = CfnPolicy.Builder.create(rootStack, mobileRegistrationPolicyId)
                .policyName(CommonUtils.generateName(rootStack, mobileRegistrationPolicyId))
                .policyDocument(mobileRegistrationPolicyStatement)
                .build();
    }

    private List<CfnTopicRule.TimestreamDimensionProperty> generateTimestreamDimensionPropertyList() {
        Map<String, String> dimensionMap = Map.ofEntries(
                Map.entry("ConnectionId", "newuuid()"),
                Map.entry("AppVersion", "AppVersion"),
                Map.entry("UserId", "UserId"),
                Map.entry("UserName", "UserName"),
                Map.entry("NetworkType", "NetworkType"),
                Map.entry("ErrorCode", "Error.ErrorCode"),
                Map.entry("ErrorMessage", "Error.ErrorMessage"),
                Map.entry("ISP", "ISP"),
                Map.entry("PreConnection.RequestEndTime", "PreConnection.RequestEndTime"),
                Map.entry("PreConnection.RequestStartTime", "PreConnection.RequestStartTime"),
                Map.entry("UseTurn", "Streaming.UseTurn"),
                Map.entry("Streaming.RequestEndTime", "Streaming.RequestEndTime"),
                Map.entry("Streaming.RequestStartTime", "Streaming.RequestStartTime"),
                Map.entry("Latitude", "Location.Latitude"),
                Map.entry("Longitude", "Location.Longitude")
        );
        ArrayList<CfnTopicRule.TimestreamDimensionProperty> timestreamDimensionPropertyList = new ArrayList<>();

        for (var dimensionSet : dimensionMap.entrySet()) {
            timestreamDimensionPropertyList.add(
                    CfnTopicRule.TimestreamDimensionProperty.builder()
                            .name(dimensionSet.getKey())
                            .value(String.format("${%s}", dimensionSet.getValue()))
                            .build()
            );
        }

        return timestreamDimensionPropertyList;
    }

    private void createRule() {
        PolicyStatement timeStreamListStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(singletonList("timestream:DescribeEndpoints"))
                // This wildcard is required to do timestream operation, see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/using-identity-based-policies.html
                .resources(singletonList("*"))
                .build();

        PolicyStatement timeStreamWriteStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(singletonList("timestream:WriteRecords"))
                .resources(singletonList(rootStack.timestreamSubStack.timestreamDatabaseTable.getAttrArn()))
                .build();

        PolicyDocument policyDocument = PolicyDocument.Builder.create()
                .statements(List.of(timeStreamListStatement, timeStreamWriteStatement))
                .build();

        final String iotRuleRoleId = "iot-rule-role";
        iotRuleEngineRole = Role.Builder.create(rootStack, iotRuleRoleId)
                .roleName(CommonUtils.generateName(rootStack, iotRuleRoleId))
                .assumedBy(new ServicePrincipal("iot.amazonaws.com"))
                .inlinePolicies(singletonMap(iotRuleRoleId, policyDocument))
                .build();

        final String sql = "SELECT\n"
                + "  API,\n"
                + "  Streaming.NominatedCandidatePair AS NominatedCandidatePair,\n"
                + "  ClientIp\n"
                + "FROM '+/request/metrics/webrtc'";

        final String ruleId = "webrtc-metrics-rule";
        iotRule = CfnTopicRule.Builder.create(rootStack, ruleId)
                .ruleName(Fn.join("_", Fn.split("-", CommonUtils.generateName(rootStack, ruleId))))
                .topicRulePayload(
                        CfnTopicRule.TopicRulePayloadProperty.builder()
                                .sql(sql)
                                .awsIotSqlVersion("2016-03-23")
                                .actions(singletonList(
                                                CfnTopicRule.ActionProperty.builder()
                                                        .timestream(
                                                                CfnTopicRule.TimestreamActionProperty.builder()
                                                                        .roleArn(iotRuleEngineRole.getRoleArn())
                                                                        .databaseName(rootStack.timestreamSubStack.timestreamDatabase.getDatabaseName())
                                                                        .tableName(rootStack.timestreamSubStack.timestreamDatabaseTable.getTableName())
                                                                        .dimensions(generateTimestreamDimensionPropertyList())
                                                                        .timestamp(
                                                                                CfnTopicRule.TimestreamTimestampProperty.builder()
                                                                                        .value("${Streaming.RequestStartTime}")
                                                                                        .unit("MILLISECONDS")
                                                                                        .build()
                                                                        )
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                )
                .build();
    }

    private void createEndpointResource() {
        final String describeAtsEndpointId = "describe-ats-endpoint";
        final AwsCustomResource describeAtsEndpointResource = AwsCustomResource.Builder
                .create(rootStack, describeAtsEndpointId)
                .onUpdate(AwsSdkCall.builder()
                        .service(Constant.IOT)
                        .action(Constant.DESCRIBE_ENDPOINT)
                        .parameters(singletonMap("endpointType", "iot:Data-ATS"))
                        .physicalResourceId(PhysicalResourceId.of(describeAtsEndpointId))
                        .build())
                .policy(AwsCustomResourcePolicy.fromStatements(List.of(PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(singletonList("iot:DescribeEndpoint"))
                        // This wildcard is required for describe iot endpoint, see https://docs.aws.amazon.com/iot/latest/developerguide/security_iam_service-with-iam.html#security_iam_service-with-iam-id-based-policies
                        .resources(singletonList("*"))
                        .build())))
                .build();
        iotAtsEndpoint = describeAtsEndpointResource.getResponseField(Constant.ENDPOINT_ADDRESS);
    }
}
