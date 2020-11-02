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
import {ArchivaRequestService} from "./archiva-request.service";
import {AccessToken} from "../model/access-token";
import { environment } from "../../environments/environment";
import {ErrorMessage} from "../model/error-message";
import {ErrorResult} from "../model/error-result";
import {HttpErrorResponse} from "@angular/common/http";

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  constructor(private rest: ArchivaRequestService) { }

  login(userid:string, password:string, resultHandler: (n: string, err?: ErrorMessage[]) => void) {

      const data = { 'grant_type':'authorization_code',
        'client_id':environment.application.client_id,
        'user_id':userid, 'password':password
      };
      let authObserver =  this.rest.executeRestCall<AccessToken>('post','redback', 'auth/authenticate', data );
      let tokenObserver = {
          next: (x: AccessToken) => {
              localStorage.setItem("access_token", x.access_token);
              localStorage.setItem("refresh_token", x.refresh_token);
              if (x.expires_in!=null) {
                  let dt = new Date();
                  dt.setSeconds(dt.getSeconds() + x.expires_in);
                  localStorage.setItem("token_expire", dt.toISOString());
              }
              resultHandler("OK");
          },
          error: ( err: HttpErrorResponse) => {
              console.log("Error " + (JSON.stringify(err)));
              let result = err.error as ErrorResult
              if (result.errorMessages!=null) {
                  for (let msg of result.errorMessages) {
                      console.error('Observer got an error: ' + msg.errorKey)
                  }
                  resultHandler("ERROR", result.errorMessages);
              } else {
                  resultHandler("ERROR", null);
              }

          },
          // complete: () => console.log('Observer got a complete notification'),
      };
      authObserver.subscribe(tokenObserver)

  }

  logout() {
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
      localStorage.removeItem("token_expire");
  }
}
