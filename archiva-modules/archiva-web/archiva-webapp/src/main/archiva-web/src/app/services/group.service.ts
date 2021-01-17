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
import {ArchivaRequestService} from "@app/services/archiva-request.service";
import {GroupMapping} from "@app/model/group-mapping";
import {catchError} from "rxjs/operators";
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {Observable, throwError} from 'rxjs';
import {PagedResult} from "@app/model/paged-result";
import {Group} from '@app/model/group';

@Injectable({
    providedIn: 'root'
})
export class GroupService {

    constructor(private rest: ArchivaRequestService) {

    }

    getGroupMappings(): Observable<GroupMapping[]> {
        return this.rest.executeRestCall<GroupMapping[]>("get", "redback", "groups/mappings", null).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        )
    }

    assignGroup(groupName: string, roleId: string): Observable<HttpResponse<any>> {
        return this.rest.executeResponseCall<any>("put", "redback", "groups/mappings/" + encodeURI(groupName) + "/roles/" + encodeURI(roleId), null)
            .pipe(catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            }));
    }

    public query(searchTerm: string, offset: number = 0, limit: number = 10, orderBy: string[] = ['name'], order: string = 'asc'): Observable<PagedResult<Group>> {
        if (searchTerm == null) {
            searchTerm = ""
        }
        if (orderBy == null || orderBy.length == 0) {
            orderBy = ['id'];
        }
        return this.rest.executeRestCall<PagedResult<Group>>("get", "redback", "groups", {
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

}
