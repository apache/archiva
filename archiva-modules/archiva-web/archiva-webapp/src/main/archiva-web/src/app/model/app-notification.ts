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

import {TemplateRef} from "@angular/core";

export class AppNotification {
    origin: string;
    header: string;
    body: string | TemplateRef<any>;
    timestamp: Date;
    classname: string[]=[''];
    delay:number=5000;
    contextData:any;
    type:string='normal'

    constructor(origin: string, body: string|TemplateRef<any>, header:string="", options: any = {}, timestamp:Date = new Date()) {
        this.origin = origin
        this.header = header;
        this.body = body;
        this.timestamp = timestamp;
        if (options.classname) {
            this.classname = options.classname;
        }
        if (options.delay) {
            this.delay = options.delay;
        }
        if (options.contextData) {
            this.contextData = options.contextData;
        }
        if (options.type)  {
            this.type = options.type;
        }
    }

    public toString(): string {
        return this.origin + ',classname:' + this.classname + ", delay:" + this.delay ;
    }

}
