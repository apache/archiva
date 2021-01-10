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

import { Injectable } from '@angular/core';
import {ArchivaRequestService} from "@app/services/archiva-request.service";
import {SecurityConfiguration} from "@app/model/security-configuration";
import {Observable, throwError} from "rxjs";
import {catchError} from "rxjs/operators";
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {BeanInformation} from "@app/model/bean-information";
import {LdapConfiguration} from "@app/model/ldap-configuration";

@Injectable({
  providedIn: 'root'
})
export class SecurityService {

  constructor(private rest:ArchivaRequestService) {

  }

  getConfiguration() : Observable<SecurityConfiguration> {
    return this.rest.executeRestCall<SecurityConfiguration>("get", "archiva", "security/config", null).pipe(
        catchError((error: HttpErrorResponse) => {
          return throwError(this.rest.getTranslatedErrorResult(error));
        })
    );
  }

    updateConfiguration(securityConfiguration : SecurityConfiguration) : Observable<HttpResponse<any>> {
        return this.rest.executeResponseCall<any>("put", "archiva", "security/config", securityConfiguration).pipe(
            catchError((error: HttpErrorResponse) => {
                console.log("Error thrown " + typeof (error));
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }

    getRbacManagers() : Observable<BeanInformation[]> {
        return this.rest.executeRestCall<BeanInformation[]>("get", "archiva", "security/rbac_managers", null).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }

    getUserManagers() : Observable<BeanInformation[]> {
        return this.rest.executeRestCall<BeanInformation[]>("get", "archiva", "security/user_managers", null).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }

    getLdapConfiguration() : Observable<LdapConfiguration> {
        return this.rest.executeRestCall<LdapConfiguration>("get", "archiva", "security/config/ldap", null).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }

    verifyLdapConfiguration(ldapConfig : LdapConfiguration) : Observable<HttpResponse<any>> {
        return this.rest.executeResponseCall<any>("post", "archiva", "security/config/ldap/verify", ldapConfig).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }


}
