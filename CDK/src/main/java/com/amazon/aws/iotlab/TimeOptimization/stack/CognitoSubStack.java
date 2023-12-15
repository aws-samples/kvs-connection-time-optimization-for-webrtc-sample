// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization.stack;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import com.amazon.aws.iotlab.TimeOptimization.constant.Constant;
import com.amazon.aws.iotlab.TimeOptimization.utils.CommonUtils;
import com.amazon.aws.iotlab.TimeOptimization.stack.base.SubStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.apigatewayv2.CfnApi;
import software.amazon.awscdk.services.apigatewayv2.CfnIntegration;
import software.amazon.awscdk.services.apigatewayv2.CfnRoute;
import software.amazon.awscdk.services.apigatewayv2.CfnStage;
import software.amazon.awscdk.services.apigatewayv2.CfnStage.AccessLogSettingsProperty;
import software.amazon.awscdk.services.apigatewayv2.CfnStage.RouteSettingsProperty;
import software.amazon.awscdk.services.cognito.AdvancedSecurityMode;
import software.amazon.awscdk.services.cognito.AutoVerifiedAttrs;
import software.amazon.awscdk.services.cognito.CfnIdentityPool;
import software.amazon.awscdk.services.cognito.CfnIdentityPoolRoleAttachment;
import software.amazon.awscdk.services.cognito.CfnUserPoolRiskConfigurationAttachment;
import software.amazon.awscdk.services.cognito.CfnUserPoolRiskConfigurationAttachment.AccountTakeoverActionTypeProperty;
import software.amazon.awscdk.services.cognito.CfnUserPoolRiskConfigurationAttachment.AccountTakeoverActionsTypeProperty;
import software.amazon.awscdk.services.cognito.CfnUserPoolRiskConfigurationAttachment.AccountTakeoverRiskConfigurationTypeProperty;
import software.amazon.awscdk.services.cognito.CfnUserPoolRiskConfigurationAttachment.CompromisedCredentialsActionsTypeProperty;
import software.amazon.awscdk.services.cognito.CfnUserPoolRiskConfigurationAttachment.CompromisedCredentialsRiskConfigurationTypeProperty;
import software.amazon.awscdk.services.cognito.CognitoDomainOptions;
import software.amazon.awscdk.services.cognito.StandardAttribute;
import software.amazon.awscdk.services.cognito.StandardAttributes;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolDomain;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.FederatedPrincipal;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Permission;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;

public class CognitoSubStack extends SubStack<AwsKvsMetricsCdkStack, CognitoSubStack> {
    CfnApi endUserApi;
    Function addTargetFunction;
    Function getClientConfigFunction;
    UserPool endUserPool;
    UserPoolDomain endUserPoolDomain;
    UserPoolClient endUserPoolClient;
    CfnIdentityPool endUserIdentityPool;
    Role endUserCognitoAuthenticatedRole;
    CfnUserPoolRiskConfigurationAttachment endUserRiskConfigurationAttachment;

    public CognitoSubStack(AwsKvsMetricsCdkStack rootStack) {
        super(rootStack);
    }

    @Override
    public CognitoSubStack create() {
        createEndUserPool();
        createEndUserApi();
        createEndUserAuth();
        return this;
    }

    private void createEndUserPool() {
        // create cognito user pool
        final String endUserPoolId = "cognito-end-user-pool";
        endUserPool = UserPool.Builder.create(rootStack, endUserPoolId)
                .userPoolName(CommonUtils.generateName(rootStack, endUserPoolId))
                .selfSignUpEnabled(true)
                .autoVerify(AutoVerifiedAttrs.builder()
                        .email(true)
                        .build())
                .standardAttributes(StandardAttributes.builder()
                        .email(StandardAttribute.builder()
                                .required(true)
                                .mutable(false)
                                .build())
                        .familyName(StandardAttribute.builder()
                                .required(true)
                                .mutable(false)
                                .build())
                        .givenName(StandardAttribute.builder().
                                required(true)
                                .mutable(false)
                                .build())
                        .preferredUsername(StandardAttribute.builder()
                                .required(true)
                                .mutable(true)
                                .build())
                        .build())
                .advancedSecurityMode(AdvancedSecurityMode.ENFORCED)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // create cognito user pool domain
        final String endUserPoolDomainId = "end-user-pool-domain";
        endUserPoolDomain = UserPoolDomain.Builder.create(rootStack, endUserPoolDomainId)
                .cognitoDomain(CognitoDomainOptions.builder()
                        .domainPrefix(CommonUtils.generateName(rootStack, endUserPoolDomainId))
                        .build())
                .userPool(endUserPool)
                .build();

        // create cognito user pool client
        final String endUserPoolClientId = "cognito-end-user-pool-client";
        endUserPoolClient = UserPoolClient.Builder.create(rootStack, endUserPoolClientId)
                .userPoolClientName(CommonUtils.generateName(rootStack, endUserPoolClientId))
                .userPool(endUserPool)
                .build();

        final String endUserRiskConfigurationAttachmentId = "end-user-risk-configuration-attachment";
        endUserRiskConfigurationAttachment = CfnUserPoolRiskConfigurationAttachment.Builder
                .create(rootStack, endUserRiskConfigurationAttachmentId)
                .clientId("ALL")
                .userPoolId(endUserPool.getUserPoolId())
                .accountTakeoverRiskConfiguration(AccountTakeoverRiskConfigurationTypeProperty.builder()
                        .actions(AccountTakeoverActionsTypeProperty.builder()
                                .highAction(AccountTakeoverActionTypeProperty.builder()
                                        .eventAction("BLOCK")
                                        .notify(false)
                                        .build())
                                .build())
                        .build())
                .compromisedCredentialsRiskConfiguration(CompromisedCredentialsRiskConfigurationTypeProperty.builder()
                        .actions(CompromisedCredentialsActionsTypeProperty.builder()
                                .eventAction("BLOCK")
                                .build())
                        .build())
                .build();

        // create cognito identity pool
        final String endUserIdentityPoolId = "cognito-end-user-identity-pool";
        endUserIdentityPool = CfnIdentityPool.Builder.create(rootStack, endUserIdentityPoolId)
                .identityPoolName(CommonUtils.generateName(rootStack, endUserIdentityPoolId))
                .cognitoIdentityProviders(singletonList(
                        CfnIdentityPool.CognitoIdentityProviderProperty.builder()
                                .clientId(endUserPoolClient.getUserPoolClientId())
                                .providerName(endUserPool.getUserPoolProviderName())
                                .serverSideTokenCheck(false)
                                .build()))
                .allowUnauthenticatedIdentities(false)
                .build();

        // create cognito authenticated role
        final String endUserAuthenticatedRoleId = "cognito-auth-role";
        endUserCognitoAuthenticatedRole = Role.Builder.create(rootStack, endUserAuthenticatedRoleId)
                .roleName(CommonUtils.generateName(rootStack, endUserAuthenticatedRoleId))
                .assumedBy(new FederatedPrincipal("cognito-identity.amazonaws.com",
                        Map.of("StringEquals",
                                Map.of("cognito-identity.amazonaws.com:aud", endUserIdentityPool.getRef()),
                                "ForAnyValue:StringLike",
                                Map.of("cognito-identity.amazonaws.com:amr", "authenticated")),
                        "sts:AssumeRoleWithWebIdentity").withSessionTags())
                .build();
    }

    private void createEndUserAuth() {
        // allow to access cognito
        var authenticatedStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                // Those wildcards are required for IAM role with auth, see https://www.cognitobuilders.training/40-lab3/10-create-identity-pool/
                .actions(List.of("mobileanalytics:PutEvents", "cognito-sync:*", "cognito-identity:*"))
                .resources(singletonList("*"))
                .build();

        // allow to access api gateway
        var apiInvokeStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(singletonList("execute-api:Invoke"))
                .resources(singletonList(
                        // This wildcard is required to allow end user access all end user related APIs
                        String.format("arn:aws:execute-api:%s:%s:%s/*", rootStack.getRegion(), rootStack.getAccount(),
                                endUserApi.getRef())))
                .build();

        // allow to access KVS
        var kinesisVideoStreamStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(
                        List.of("kinesisvideo:ConnectAsViewer", "kinesisvideo:DescribeSignalingChannel",
                                "kinesisvideo:GetIceServerConfig", "kinesisvideo:GetSignalingChannelEndpoint"))
                // This wildcard is required since the device serial number is vague
                .resources(singletonList(
                        String.format("arn:aws:kinesisvideo:%s:%s:channel/*", rootStack.getRegion(), rootStack.getAccount())))
                .build();

        // allow to access IoT Core
        var iotConnectStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(singletonList("iot:Connect"))
                .resources(singletonList(
                        // This wildcard is required since mobile app client id has a random postfix
                        String.format("arn:aws:iot:%s:%s:client/${cognito-identity.amazonaws.com:sub}/*",
                                rootStack.getRegion(), rootStack.getAccount())))
                .build();

        final String mobileTopicPrefix = String.format(
                "arn:aws:iot:%s:%s:topic/${cognito-identity.amazonaws.com:sub}", rootStack.getRegion(), rootStack.getAccount());
        var iotPublishStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(singletonList("iot:Publish"))
                .resources(singletonList(String.format("%s/request/metrics/webrtc", mobileTopicPrefix)))
                .build();

        // add statements to role
        endUserCognitoAuthenticatedRole.addToPolicy(authenticatedStatement);
        endUserCognitoAuthenticatedRole.addToPolicy(apiInvokeStatement);
        endUserCognitoAuthenticatedRole.addToPolicy(kinesisVideoStreamStatement);
        endUserCognitoAuthenticatedRole.addToPolicy(iotConnectStatement);
        endUserCognitoAuthenticatedRole.addToPolicy(iotPublishStatement);

        // attach roles to identity pool
        CfnIdentityPoolRoleAttachment.Builder
                .create(rootStack, "end-user-cognito-identity-pool-default-policy")
                .identityPoolId(endUserIdentityPool.getRef())
                .roles(singletonMap("authenticated", endUserCognitoAuthenticatedRole.getRoleArn()))
                .build();

        // create output
        CfnOutput.Builder.create(rootStack, "UserPoolId")
                .value(endUserPool.getUserPoolId())
                .description("User Pool ID")
                .build();
        CfnOutput.Builder.create(rootStack, "UserPoolClientId")
                .value(endUserPoolClient.getUserPoolClientId())
                .description("User Pool Client ID")
                .build();
        CfnOutput.Builder.create(rootStack, "IdentityPoolId")
                .value(endUserIdentityPool.getRef())
                .description("Identity Pool ID")
                .build();
    }

    private void createEndUserApi() {
        // allow to access cloudwatch
        final PolicyStatement cloudWatchStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"))
                // Those wildcard is required for cloud watch log create and upload
                .resources(singletonList("arn:aws:logs:*:*:*"))
                .build();

        // allow to attach IoT Core policy
        final PolicyStatement iotStatement = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(singletonList("iot:AttachPolicy"))
                // This wildcard is required since Cognito identity id does not have arn
                .resources(singletonList("*"))
                .build();

        final PolicyDocument getClientConfigFunctionPolicyDocument = PolicyDocument.Builder.create()
                .statements(List.of(cloudWatchStatement))
                .build();

        // lambda function role
        final String getClientConfigFunctionRoleId = "get-client-config-role";
        final Role getClientConfigFunctionRole = Role.Builder
                .create(rootStack, getClientConfigFunctionRoleId)
                .roleName(CommonUtils.generateName(rootStack, getClientConfigFunctionRoleId))
                .inlinePolicies(
                        singletonMap(getClientConfigFunctionRoleId, getClientConfigFunctionPolicyDocument))
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build();

        // get client config function
        final String getClientConfigFunctionId = "get-client-config";
        getClientConfigFunction = Function.Builder.create(rootStack, getClientConfigFunctionId)
                .functionName(CommonUtils.generateName(rootStack, getClientConfigFunctionId))
                .code(Code.fromAsset(Constant.PYTHON_FUNCTION_LOCAL_PATH))
                .environment(generateGetClientConfigLambdaEnv())
                .architecture(Architecture.X86_64)
                .runtime(Runtime.PYTHON_3_9).handler("get_client_config.lambda_handler")
                .timeout(Duration.seconds(5))
                .role(getClientConfigFunctionRole)
                .build();

        final PolicyDocument addTargetFunctionPolicyDocument = PolicyDocument.Builder.create()
                .statements(List.of(cloudWatchStatement, iotStatement))
                .build();

        final String addTargetFunctionRoleId = "add-target-role";
        final Role addTargetFunctionRole = Role.Builder.create(rootStack, addTargetFunctionRoleId)
                .roleName(CommonUtils.generateName(rootStack, addTargetFunctionRoleId))
                .inlinePolicies(singletonMap(addTargetFunctionRoleId, addTargetFunctionPolicyDocument))
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .build();

        // add target function
        final String addTargetFunctionId = "add-target";
        addTargetFunction = Function.Builder.create(rootStack, addTargetFunctionId)
                .functionName(CommonUtils.generateName(rootStack, addTargetFunctionId))
                .code(Code.fromAsset(Constant.PYTHON_FUNCTION_LOCAL_PATH))
                .environment(generateAddTargetLambdaEnv())
                .architecture(Architecture.X86_64)
                .runtime(Runtime.PYTHON_3_9).handler("add_target.lambda_handler")
                .timeout(Duration.seconds(5))
                .role(addTargetFunctionRole)
                .build();

        // api gateway
        final String endUserApiId = "end-user-api";
        endUserApi = CfnApi.Builder.create(rootStack, endUserApiId)
                .name(CommonUtils.generateName(rootStack, endUserApiId))
                .protocolType("HTTP")
                .corsConfiguration(CfnApi.CorsProperty.builder().allowOrigins(singletonList("*"))
                        .allowMethods(List.of("GET", "PUT", "POST")).allowHeaders(singletonList("*"))
                        .build())
                .build();

        // CloudWatch log group for API access log
        final String endUserApiLogGroupId = "end-user-api-log-group";
        LogGroup endUserApiLogGroup = LogGroup.Builder.create(rootStack, endUserApiLogGroupId)
                .logGroupName(CommonUtils.generateName(rootStack, endUserApiLogGroupId))
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // create stage
        CfnStage.Builder.create(rootStack, "end-user-api-stage")
                .apiId(endUserApi.getRef())
                .stageName("$default")
                .defaultRouteSettings(RouteSettingsProperty.builder()
                        .detailedMetricsEnabled(true)
                        .throttlingBurstLimit(5000)
                        .throttlingRateLimit(10000)
                        .build())
                .accessLogSettings(AccessLogSettingsProperty.builder()
                        .destinationArn(endUserApiLogGroup.getLogGroupArn())
                        .format("$context.identity.sourceIp - - [$context.requestTime] \"$context.httpMethod $context.routeKey $context.protocol\" $context.status $context.responseLength $context.requestId")
                        .build())
                .autoDeploy(true)
                .build();

        // API: GetClientConfig
        final String getClientConfigApiPath = "/client/config";
        var getClientConfigApiIntegration = createEndUserApiIntegration(
                "get-client-config-api-integration", getClientConfigFunction);
        createEndUserApiRoute("get-client-config-api-route", "GET " + getClientConfigApiPath,
                getClientConfigApiIntegration);

        // API: AddTarget
        final String addTargetApiPath = "/add-target";
        var addTargetApiIntegration = createEndUserApiIntegration("add-target-api-integration",
                addTargetFunction);
        createEndUserApiRoute("add-target-api-route", "PUT " + addTargetApiPath,
                addTargetApiIntegration);

        // allow api gateway call lambda
        getClientConfigFunction.addPermission("Invocation", Permission.builder()
                .principal(new ServicePrincipal("apigateway.amazonaws.com"))
                .action("lambda:InvokeFunction")
                .sourceArn(String.format("arn:aws:execute-api:%s:%s:%s/*/*%s", rootStack.getRegion(),
                        rootStack.getAccount(), endUserApi.getRef(), getClientConfigApiPath))
                .build());

        addTargetFunction.addPermission("Invocation", Permission.builder()
                .principal(new ServicePrincipal("apigateway.amazonaws.com"))
                .action("lambda:InvokeFunction")
                .sourceArn(String.format("arn:aws:execute-api:%s:%s:%s/*/*%s", rootStack.getRegion(),
                        rootStack.getAccount(), endUserApi.getRef(), addTargetApiPath))
                .build());

        // generate output
        CfnOutput.Builder.create(rootStack, "end-user-api-endpoint")
                .value(endUserApi.getAttrApiEndpoint())
                .build();
    }

    private CfnIntegration createEndUserApiIntegration(final String id, Function function) {
        return CfnIntegration.Builder.create(rootStack, id)
                .apiId(endUserApi.getRef())
                .integrationType("AWS_PROXY")
                .payloadFormatVersion("2.0")
                .integrationUri(function.getFunctionArn())
                .build();
    }

    private void createEndUserApiRoute(final String id, final String routeKey,
                                       CfnIntegration integration) {
        CfnRoute.Builder.create(rootStack, id)
                .apiId(endUserApi.getRef())
                .routeKey(routeKey)
                .authorizationType("AWS_IAM")
                .target("integrations/" + integration.getRef())
                .build();
    }

    private Map<String, String> generateGetClientConfigLambdaEnv() {
        return Map.of(Constant.LAMBDA_ENV_IOT_ATS_ENDPOINT_KEY,
                rootStack.iotCoreSubStack.iotAtsEndpoint);
    }

    private Map<String, String> generateAddTargetLambdaEnv() {
        final String policyName = Objects.requireNonNull(
                rootStack.iotCoreSubStack.mobileRegistrationPolicy.getPolicyName());

        return Map.of(Constant.LAMBDA_ENV_MOBILE_REGISTRATION_POLICY_KEY, policyName,
                Constant.LAMBDA_ENV_COGNITO_USER_POOL_ID_KEY, endUserPool.getUserPoolId());
    }
}
