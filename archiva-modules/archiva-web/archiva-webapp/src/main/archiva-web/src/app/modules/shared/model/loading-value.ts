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

export class LoadingValue<T> {
    loading: boolean;
    value: T;
    error: any;

    public hasError() : boolean {
        return this.error != null;
    }

    public hasValue() : boolean {
        return this.value != null;
    }

    static start<T>() : LoadingValue<T> {
        let lv = new LoadingValue<T>();
        lv.loading=true;
        return lv;
    }

    static finish<T>(value: T) : LoadingValue<T> {
        let lv = new LoadingValue<T>();
        lv.loading=false;
        lv.value = value;
        return lv;
    }

    static of<T>(type: string, value: T = null) : LoadingValue<T>{
        let lv = new LoadingValue<T>();
        if (type=='start') {
            lv.loading=true;
        } else if (type=='finish') {
            lv.loading=false;
            lv.value=value;
        }
        return lv;
    }

    static error<T>(error:any) : LoadingValue<T> {
        let lv = new LoadingValue<T>();
        lv.loading=false;
        lv.error=error;
        return lv;
    }
}
