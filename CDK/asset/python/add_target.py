#  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#  SPDX-License-Identifier: MIT-0

import os
import json
import boto3

from request_response import RequestSuccess, InternalServerException, BadRequestException


def lambda_handler(event: dict, context: dict) -> dict:
    iot = boto3.client('iot')

    try:
        body = json.loads(event['body'])
        identity_id = body['IdentityId']
    except Exception:
        return vars(BadRequestException('Missing parameter in request body'))

    try:
        iot.attach_policy(
            policyName=os.environ['MOBILE_REGISTRATION_POLICY'],
            target=identity_id
        )
    except Exception as e:
        return vars(InternalServerException(e))

    return vars(RequestSuccess())
