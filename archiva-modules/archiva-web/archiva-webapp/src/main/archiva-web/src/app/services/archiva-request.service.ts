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
import {Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {Observable} from "rxjs";
import {ErrorMessage} from "../model/error-message";
import {TranslateService} from "@ngx-translate/core";
import {ErrorResult} from "../model/error-result";

@Injectable({
    providedIn: 'root'
})
export class ArchivaRequestService {

    // Stores the access token locally
    accessToken: string;

    constructor(private http: HttpClient, private translator: TranslateService) {
    }

    /**
     * Executes a rest call to the archiva / redback REST services.
     * @param type the type of the call (get, post, update)
     * @param module the module (archiva, redback)
     * @param service the REST service to call
     * @param input the input data, if this is a POST or UPDATE request
     */
    executeRestCall<R>(type: string, module: string, service: string, input: object): Observable<R> {
        let httpArgs = this.getHttpOptions(type, module, service, input);
        httpArgs['options']['observe'] = 'body';
        httpArgs['options']['responseType'] = 'json';

        let lType = type.toLowerCase();
        if (lType == "get") {
            return this.http.get<R>(httpArgs.url, httpArgs.options);
        } else if (lType == "head" ) {
            return this.http.head<R>(httpArgs.url, httpArgs.options);
        } else if (lType == "post") {
            return this.http.post<R>(httpArgs.url, input, httpArgs.options);
        } else if (lType == "delete") {
            return this.http.delete<R>(httpArgs.url, httpArgs.options);
        } else if (lType == "put") {
            return this.http.put<R>(httpArgs.url, input, httpArgs.options);
        } else if (lType == "patch") {
            return this.http.patch<R>(httpArgs.url, input, httpArgs.options);
        }
    }

    private getHttpOptions(type: string, module: string, service: string, input: object) {
        let modulePath = environment.application.servicePaths[module];
        let url = environment.application.baseUrl + environment.application.restPath + "/" + modulePath + "/" + service;
        let token = this.getToken();
        let headers = null;
        if (token != null) {
            headers = {
                "Authorization": "Bearer " + token
            }
        } else {
            headers = {};
        }
        let options = {'headers': headers}
        if (type.toLowerCase()=='get') {
            let params = {}
            if (input!=null) {
                params = input;
            }
            options['params'] = params;
        }
        return {'url':url, 'options':options}
    }

    executeResponseCall<R>(type: string, module: string, service:string, input:object) : Observable<HttpResponse<R>> {
        let httpArgs = this.getHttpOptions(type, module, service, input);
        httpArgs['options']['observe'] = 'response';
        httpArgs['options']['responseType'] = 'json';
        let lType = type.toLowerCase();
        if (lType == "get") {
            return this.http.get<HttpResponse<R>>(httpArgs.url, httpArgs.options);
        } else if (lType=='head') {
            return this.http.head<HttpResponse<R>>(httpArgs.url, httpArgs.options);
        } else if (lType == 'post') {
            return this.http.post<HttpResponse<R>>(httpArgs.url, input, httpArgs.options);
        } else if (lType=='delete') {
            return this.http.delete<HttpResponse<R>>(httpArgs.url, httpArgs.options);
        } else if (lType=='put') {
            return this.http.put<HttpResponse<R>>(httpArgs.url, input, httpArgs.options);
        } else if (lType=='patch') {
            return this.http.patch<HttpResponse<R>>(httpArgs.url, input, httpArgs.options);
        }
    }

    public resetToken() {
        this.accessToken = null;
    }

    private getToken(): string {
        if (this.accessToken != null) {
            return this.accessToken;
        } else {
            let token = localStorage.getItem("access_token");
            if (token != null && token != "") {
                this.accessToken = token;
                return token;
            } else {
                return null;
            }
        }
    }

    /**
     * Translates a given error message to the current set language.
     * @param errorMsg the errorMsg as returned by a REST call
     */
    public translateError(errorMsg: ErrorMessage): string {
        console.log("Translating error "+errorMsg.error_key)
        if (errorMsg.error_key != null && errorMsg.error_key != '') {
            let parms = {};
            if (errorMsg.args != null && errorMsg.args.length > 0) {
                for (let i = 0; i < errorMsg.args.length; i++) {
                    parms['arg' + i] = errorMsg.args[i];
                }
            }
            return this.translator.instant('api.' + errorMsg.error_key, parms);
        }
    }

    public getTranslatedErrorResult(httpError : HttpErrorResponse) : ErrorResult {
        console.log("Error " + httpError);
        if (httpError == null) {
            return new ErrorResult([]);
        }

        let errorResult
        if (httpError.error) {
            errorResult = new ErrorResult(httpError.error);
        } else {
            if (httpError.statusText!=null) {
                errorResult = new ErrorResult([ErrorMessage.of(httpError.statusText)]);
            } else {
                errorResult = new ErrorResult([]);
            }
        }
        console.log("Returning error " + errorResult);
        errorResult.status = httpError.status;
        if (errorResult.error_messages!=null) {
            for (let message of errorResult.error_messages) {
                if (message.message==null || message.message=='') {
                    message.message = this.translateError(message);
                }
            }
        }
        return errorResult;
    }
}
