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
import {catchError, map} from "rxjs/operators";
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {BeanInformation} from "@app/model/bean-information";
import {LdapConfiguration} from "@app/model/ldap-configuration";
import {PagedResult} from "@app/model/paged-result";
import {Role} from "@app/model/role";
import {PropertyEntry} from "@app/model/property-entry";
import {CacheConfiguration} from "@app/model/cache-configuration";

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

    updateConfiguration(securityConfiguration : SecurityConfiguration) : Observable<HttpResponse<SecurityConfiguration>> {
        return this.rest.executeResponseCall<SecurityConfiguration>("put", "archiva", "security/config", securityConfiguration).pipe(
            catchError((error: HttpErrorResponse) => {
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
            map((ldapConfig:LdapConfiguration)=>
                new LdapConfiguration(ldapConfig)
            ) ,
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }

    updateLdapConfiguration(ldapConfiguration : LdapConfiguration) : Observable<HttpResponse<LdapConfiguration>> {
        return this.rest.executeResponseCall<LdapConfiguration>("put", "archiva", "security/config/ldap", ldapConfiguration).pipe(
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

    queryProperties(searchTerm: string, offset: number = 0, limit: number = 10, orderBy: string[] = ['key'], order: string = 'asc'): Observable<PagedResult<PropertyEntry>> {
        if (searchTerm == null) {
            searchTerm = ""
        }
        if (orderBy == null || orderBy.length == 0) {
            orderBy = ['key'];
        }
        return this.rest.executeRestCall<PagedResult<PropertyEntry>>("get", "archiva", "security/config/properties", {
            'q': searchTerm,
            'offset': offset,
            'limit': limit,
            'orderBy': orderBy,
            'order': order
        }).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            }));
    }

    updateProperty(propKey:string, propValue:string) : Observable<HttpResponse<any>> {
      return this.rest.executeResponseCall('put', 'archiva','security/config/properties/'+encodeURI(propKey), {key:propKey, value:propValue})
          .pipe(catchError((error: HttpErrorResponse) => {
              return throwError(this.rest.getTranslatedErrorResult(error));
          }))
    }

    getCacheConfiguration() : Observable<CacheConfiguration> {
        return this.rest.executeRestCall<CacheConfiguration>("get", "archiva", "security/config/cache", null).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }

    updateCacheConfiguration(cacheConfig:CacheConfiguration) : Observable<HttpResponse<CacheConfiguration>> {
        return this.rest.executeResponseCall<CacheConfiguration>("put", "archiva", "security/config/cache", cacheConfig).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }

}
