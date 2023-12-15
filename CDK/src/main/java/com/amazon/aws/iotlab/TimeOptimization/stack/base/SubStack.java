// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.iotlab.TimeOptimization.stack.base;

import software.amazon.awscdk.Stack;

public abstract class SubStack<R extends Stack, T extends SubStack> {

    protected R rootStack;

    public SubStack(R rootStack) {
        this.rootStack = rootStack;
    }

    public abstract T create();
}
