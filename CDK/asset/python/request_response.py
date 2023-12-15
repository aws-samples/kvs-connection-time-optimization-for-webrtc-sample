#  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#  SPDX-License-Identifier: MIT-0

class RequestResponse():
    """Base class for request response"""

    def __init__(self, statusCode: int, body: str):
        self.statusCode = statusCode
        self.body = body


class RequestSuccess(RequestResponse):
    """RequestSuccess, 200"""

    def __init__(self, body: str = 'Success'):
        super().__init__(statusCode=200, body=body)


class RequestException(RequestResponse, IOError):
    """Base class for request exceptions."""

    def __init__(self, statusCode: int, body: str):
        RequestResponse.__init__(self, statusCode=statusCode, body=body)
        IOError.__init__(self)


class BadRequestException(RequestException):
    """BadRequest, 400"""

    def __init__(self, body: str = 'BadRequest'):
        super().__init__(statusCode=400, body=body)


class InternalServerException(RequestException):
    """InternalServerError, 500"""

    def __init__(self, body: str = 'InternalServerError'):
        super().__init__(statusCode=500, body=body)
