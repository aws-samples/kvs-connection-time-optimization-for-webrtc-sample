// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization.stack;

import com.amazon.aws.iotlab.TimeOptimization.stack.base.SubStack;
import com.amazon.aws.iotlab.TimeOptimization.utils.CommonUtils;
import software.amazon.awscdk.services.timestream.CfnDatabase;
import software.amazon.awscdk.services.timestream.CfnTable;
import software.amazon.awscdk.services.timestream.CfnTable.SchemaProperty;
import software.amazon.awscdk.services.timestream.CfnTable.PartitionKeyProperty;
import software.amazon.awscdk.services.timestream.CfnTable.RetentionPropertiesProperty;
import software.amazon.awscdk.services.timestream.CfnTable.MagneticStoreWritePropertiesProperty;

import java.util.List;

public class TimestreamSubStack extends SubStack<AwsKvsMetricsCdkStack, TimestreamSubStack> {
    CfnDatabase timestreamDatabase;
    CfnTable timestreamDatabaseTable;

    public TimestreamSubStack(AwsKvsMetricsCdkStack rootStack) {
        super(rootStack);
    }

    @Override
    public TimestreamSubStack create() {
        final String timestreamDatabaseId = "timestream-db";
        timestreamDatabase = CfnDatabase.Builder.create(rootStack, timestreamDatabaseId)
                .databaseName(CommonUtils.generateName(rootStack, timestreamDatabaseId))
                .build();

        final String timestreamDatabaseTableId = "timestream-db-table";
        timestreamDatabaseTable = CfnTable.Builder.create(rootStack, timestreamDatabaseTableId)
                .tableName(CommonUtils.generateName(rootStack, timestreamDatabaseTableId))
                .databaseName(timestreamDatabase.getDatabaseName())
                .magneticStoreWriteProperties(MagneticStoreWritePropertiesProperty.builder()
                        .enableMagneticStoreWrites(true)
                        .build())
                .retentionProperties(RetentionPropertiesProperty.builder()
                        .magneticStoreRetentionPeriodInDays("3650")
                        .memoryStoreRetentionPeriodInHours("1")
                        .build())
                .schema(SchemaProperty.builder()
                        .compositePartitionKey(List.of(PartitionKeyProperty.builder()
                                .type("DIMENSION")
                                .enforcementInRecord("REQUIRED")
                                .name("UserId")
                                .build()))
                        .build())
                .build();
        timestreamDatabaseTable.getNode().addDependency(timestreamDatabase);

        return this;
    }
}
