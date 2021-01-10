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

import {
    HttpHandler,
    HttpRequest,
    HttpEvent,
    HttpErrorResponse,
    HttpInterceptor
} from "@angular/common/http";
import { Observable, throwError } from "rxjs";
import { catchError, finalize } from "rxjs/operators";
import { Injectable } from "@angular/core";
import {ErrorDialogService} from "../../../services/error/error-dialog.service";

/**
 * Checks for generic HTTP errors and adds messages to the error service.
 */
@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
    constructor(private errorService: ErrorDialogService) {}

    intercept(
        request: HttpRequest<any>,
        next: HttpHandler
    ): Observable<HttpEvent<any>> {
        return next.handle(request).pipe(
            catchError((error: HttpErrorResponse) => {
                if (error.url.includes('security/config/ldap/verify')) {
                    return  throwError(error);
                }
                console.error("Error from HTTP error interceptor", error);
                if (error.status==0 && error.statusText=="Unknown Error") {
                    console.log("Unknown error");
                    this.errorService.addError('error.http.unknownError');
                } else if (error.status==403) {
                    console.log("Permission error "+error.message);
                    this.errorService.addError('error.http.permissionDenied');
                }
                return throwError(error);
            }),
            finalize(() => {
            })
        ) as Observable<HttpEvent<any>>;
    }
}