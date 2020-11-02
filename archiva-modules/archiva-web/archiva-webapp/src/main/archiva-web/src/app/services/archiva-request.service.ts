/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import { Injectable } from '@angular/core';
import {HttpClient, HttpEvent, HttpResponse} from "@angular/common/http";
import { environment } from "../../environments/environment";
import {Observable} from "rxjs";
import {ErrorMessage} from "../model/error-message";
import {TranslateService} from "@ngx-translate/core";

@Injectable({
  providedIn: 'root'
})
export class ArchivaRequestService {

  constructor(private http : HttpClient, private translator : TranslateService) { }

  executeRestCall<R>(type: string, module: string, service: string, input: object ) : Observable<R> {
    let modulePath = environment.application.servicePaths[module];
    let url = environment.application.baseUrl + environment.application.restPath + "/" + modulePath + "/" + service;
    let token = localStorage.getItem("access_token")
    let headers = null;
    if (token != null) {
      headers = {
        "Authorization": "Bearer " + localStorage.getItem("access_token")
      }
    } else {
      headers = {};
    }
    if (type == "get") {
      return this.http.get<R>(url, {"headers":headers});
    } else if ( type == "post") {
      return this.http.post<R>(url, input, {"headers":headers});
    }
  }


  translateError(errorMsg : ErrorMessage) : string {
    if (errorMsg.errorKey!=null && errorMsg.errorKey!='') {
      let parms = {};
      if (errorMsg.args!=null && errorMsg.args.length>0) {
        for ( let i=0; i<errorMsg.args.length; i++) {
          parms['arg' + i] = errorMsg.args[i];
        }
      }
      return this.translator.instant('api.'+errorMsg.errorKey, parms);
    }
  }
}
