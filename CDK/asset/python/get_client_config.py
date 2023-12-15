#  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#  SPDX-License-Identifier: MIT-0

import os
import json
from typing import Dict, Final

from request_response import RequestSuccess, InternalServerException, BadRequestException

configMap: Final[Dict[str, str]] = {
    'IotAtsEndpoint': 'IOT_ATS_ENDPOINT'
}


class ClientConfigResponse(RequestSuccess):
    def __init__(self, config: dict):
        super().__init__(body=json.dumps(config))


def lambda_handler(event: dict, context: dict) -> dict:
    try:
        config = {key: os.environ[value] for key, value in configMap.items()}
    except Exception:
        return vars(InternalServerException())

    return vars(ClientConfigResponse(config=config))
