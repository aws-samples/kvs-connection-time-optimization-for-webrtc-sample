// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization.utils;

import java.util.Arrays;

import com.amazon.aws.iotlab.TimeOptimization.constant.Constant;
import software.amazon.awscdk.Stack;

public class CommonUtils {

    public static String generateName(Stack stack, String id) {
        return String.join(Constant.RESOURCE_DELIMITER,
                Arrays.asList(Constant.RESOURCE_PREFIX, id, stack.getRegion(), stack.getAccount()));
    }
}
