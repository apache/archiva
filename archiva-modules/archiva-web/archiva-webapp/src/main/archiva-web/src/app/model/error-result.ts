/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import {ErrorMessage} from "./error-message";

export class ErrorResult {
    error_messages: Array<ErrorMessage>
    status: number;

    constructor(json:any) {
        if (Array.isArray(json)) {
            this.error_messages = json;
        } else {
            this.error_messages = json.error_messages;
            this.status = json.status;
        }
    }

    hasMessages() :boolean {
        return this.error_messages != null && this.error_messages.length > 0;
    }

    public firstMessage() : ErrorMessage {
        if (this.error_messages!=null && this.error_messages.length>0) {
            return this.error_messages[0];
        } else {
            return new ErrorMessage();
        }
    }

    public firstMessageString() : string {
        if (this.error_messages!=null && this.error_messages.length>0) {
            return this.error_messages[0].message;
        } else {
            return '';
        }
    }

    public toString() : string {
        if (this.error_messages!=null && this.error_messages.length>0) {
            return this.error_messages.join(',');
        } else {
            return 'status=' + this.status;
        }
    }
}
