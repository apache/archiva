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

import {Injectable, OnDestroy, OnInit} from '@angular/core';
import {ArchivaRequestService} from "@app/services/archiva-request.service";
import {UserInfo} from '@app/model/user-info';
import {HttpErrorResponse, HttpResponse} from "@angular/common/http";
import {ErrorResult} from "@app/model/error-result";
import {Observable, throwError} from "rxjs";
import {Permission} from '@app/model/permission';
import {PagedResult} from "@app/model/paged-result";
import {User} from '@app/model/user';
import {catchError, map} from "rxjs/operators";
import {RoleTree} from "@app/model/role-tree";

@Injectable({
    providedIn: 'root'
})
export class UserService implements OnInit, OnDestroy {

    userInfo: UserInfo;
    permissions: Permission[];
    guestPermissions: Permission[];
    authenticated: boolean;
    uiPermissionsDefault = {
        'menu': {
            'repo': {
                'section': true,
                'browse': true,
                'search': true,
                'upload': false
            },
            'security':{
                'section': false,
                'roles': false,
                'users': false,
                'config': false
            },
            'admin': {
                'section': false,
                'config': false,
                'status': false,
                'reports': false
            },
        }
    };
    uiPermissions;

    constructor(private rest: ArchivaRequestService) {
        this.userInfo = new UserInfo();
        this.uiPermissions = {};
        this.deepCopy(this.uiPermissionsDefault, this.uiPermissions);
    }

    ngOnDestroy(): void {
        this.resetUser();
    }

    ngOnInit(): void {
        this.userInfo.user_id = "guest";
        this.loadPersistedUserInfo();
        this.authenticated = false;
        this.deepCopy(this.uiPermissionsDefault, this.uiPermissions);
        if (this.guestPermissions == null) {
            let observer = {
                next: (permList: Permission[]) => {
                    this.guestPermissions = permList;
                    if (!this.authenticated) {
                        this.permissions = this.guestPermissions;
                        this.parsePermissions(this.permissions);
                    }
                },
                error: err => {
                    console.log("Could not retrieve permissions " + err);
                }
            }
            this.retrievePermissionInfo("guest").subscribe(observer);
        }
    }

    /**
     * Retrieves the user information from the REST service for the current logged in user.
     * This works only, if a valid access token is present.
     * It returns a observable that can be subscribed to catch the user information.
     */
    public retrieveUserInfo(): Observable<UserInfo> {
        return new Observable<UserInfo>((resultObserver) => {
            let accessToken = localStorage.getItem("access_token");

            if (accessToken != null) {
                let infoObserver = this.rest.executeRestCall<UserInfo>("get", "redback", "users/me", null);
                let userInfoObserver = {
                    next: (x: UserInfo) => {
                        this.userInfo = x;
                        if (this.userInfo.language == null) {
                            this.loadPersistedUserInfo();
                        }
                        this.persistUserInfo();
                        this.authenticated = true;
                        resultObserver.next(this.userInfo);
                    },
                    error: (err: HttpErrorResponse) => {
                        console.log("Error " + (JSON.stringify(err)));
                        let result = err.error as ErrorResult
                        if (result != null && result.error_messages != null) {
                            for (let msg of result.error_messages) {
                                console.error('Observer got an error: ' + msg.error_key)
                            }
                        } else if (err.message != null) {
                            console.error("Bad response from user info call: " + err.message);
                        }
                        this.authenticated = false;
                        resultObserver.error();
                    },
                    complete: () => {
                        resultObserver.complete();
                    }
                };
                infoObserver.subscribe(userInfoObserver);
            }
        });
    }

    /**
     * Retrieves the permission list from the REST service
     */
    public retrievePermissionInfo(userNameParam?:string): Observable<Permission[]> {
        let userName;
        if (userNameParam==null||userNameParam=='') {
            userName = this.authenticated ? "me" : "guest";
        } else {
            userName = userNameParam;
        }
        return this.rest.executeRestCall<Permission[]>("get", "redback", "users/" + userName + "/permissions", null).pipe(
            catchError((err:HttpErrorResponse)=> {
                console.log("Error " + (JSON.stringify(err)));
                let result = err.error as ErrorResult
                if (result.error_messages != null) {
                    for (let msg of result.error_messages) {
                        console.debug('Observer got an error: ' + msg.error_key)
                    }
                }
                this.resetPermissions();
                return [];
            }), map((perm:Permission[])=>{
                this.permissions = perm;
                this.parsePermissions(perm);
                return perm;
                })
        );

    }

    resetPermissions() {
        this.deepCopy(this.uiPermissionsDefault, this.uiPermissions);
    }

    parsePermissions(permissions: Permission[]) {
        this.resetPermissions();
        for (let perm of permissions) {
            // console.debug("Checking permission for op: " + perm.operation.name);
            switch (perm.operation.name) {
                case "archiva-manage-configuration": {
                    if (perm.resource.identifier == '*') {
                        this.uiPermissions.menu.admin.section = true;
                        this.uiPermissions.menu.admin.config = true;
                        this.uiPermissions.menu.admin.reports = true;
                        this.uiPermissions.menu.admin.status = true;
                        this.uiPermissions.menu.security.section = true;
                        this.uiPermissions.menu.security.config = true;
                    }

                }
                case "archiva-manage-users": {
                    if (perm.resource.identifier == '*') {
                        this.uiPermissions.menu.security.section = true;
                        this.uiPermissions.menu.security.users = true;
                        this.uiPermissions.menu.security.roles = true;
                    }
                }
                case "redback-configuration-edit": {
                    if (perm.resource.identifier == '*') {
                        this.uiPermissions.menu.security.section = true;
                        this.uiPermissions.menu.security.config = true;
                    }
                }
                case "archiva-upload-file": {
                    this.uiPermissions.menu.repo.upload = true;
                }
            }
        }
    }

    private deepCopy(src: Object, dst: Object) {
        Object.keys(src).forEach((key, idx) => {
            let srcEl = src[key];
            if (typeof (srcEl) == 'object') {
                let dstEl;
                if (!dst.hasOwnProperty(key)) {
                    dst[key] = {}
                }
                dstEl = dst[key];
                this.deepCopy(srcEl, dstEl);
            } else {
                // console.debug("setting " + key + " = " + srcEl);
                dst[key] = srcEl;
            }
        });
    }


    /**
     * Stores user information persistent. Not the complete UserInfo object, only properties, that
     * are needed.
     */
    public persistUserInfo() {
        if (this.userInfo != null && this.userInfo.user_id != null && this.userInfo.user_id != "") {
            let prefix = "user." + this.userInfo.user_id;
            localStorage.setItem(prefix + ".user_id", this.userInfo.user_id);
            localStorage.setItem(prefix + ".id", this.userInfo.id);
            if (this.userInfo.language != null && this.userInfo.language != "") {
                localStorage.setItem(prefix + ".language", this.userInfo.language);
            }
        }
    }

    /**
     * Loads the persisted user info from the local storage
     */
    public loadPersistedUserInfo() {
        if (this.userInfo.user_id != null && this.userInfo.user_id != "") {
            let prefix = "user." + this.userInfo.user_id;
            this.userInfo.language = localStorage.getItem(prefix + ".language");
        }
    }

    /**
     * Resets the user info to default values.
     */
    resetUser() {
        this.userInfo = new UserInfo();
        this.userInfo.user_id = "guest";
        this.resetPermissions();
        this.authenticated = false;
    }

    public query(searchTerm: string, offset: number = 0, limit: number = 10, orderBy: string[] = ['user_id'], order: string = 'asc'): Observable<PagedResult<UserInfo>> {
        console.log("getUserList " + searchTerm + "," + offset + "," + limit + "," + orderBy + "," + order);
        if (searchTerm == null) {
            searchTerm = ""
        }
        if (orderBy == null || orderBy.length == 0) {
            orderBy = ['user_id'];
        }
        return this.rest.executeRestCall<PagedResult<UserInfo>>("get", "redback", "users", {
            'q': searchTerm,
            'offset': offset,
            'limit': limit,
            'orderBy': orderBy,
            'order': order
        });
    }


    public addUser(user: User): Observable<UserInfo> {
        return this.rest.executeResponseCall<UserInfo>("post", "redback", "users", user).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            }), map((httpResponse: HttpResponse<UserInfo>) => {
                if (httpResponse.status==201) {
                    let user = httpResponse.body;
                    user.location = httpResponse.headers.get('Location');
                    return user;
                } else {
                    throwError(new HttpErrorResponse({headers:httpResponse.headers,status:httpResponse.status,statusText:"Bad response code"}))
                }
            }));
    }

    public getUser(userid: string): Observable<UserInfo> {
        return this.rest.executeRestCall<UserInfo>("get", "redback", "users/" + userid, null).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            }));
    }

    public updateUser(user:User): Observable<UserInfo> {
        return this.rest.executeRestCall<UserInfo>("put", "redback", "users/" + user.user_id, user).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            }));
    }

    public deleteUser(user_id:string): Observable<boolean> {
        return this.rest.executeResponseCall<boolean>("delete", "redback", "users/" + user_id, null).pipe(
            catchError((error: HttpErrorResponse) => {
                return throwError(this.rest.getTranslatedErrorResult(error));
            }),
            map((response) => {
                return response.status == 200;
            }));
    }

    public userExists(userid:string): Observable<boolean> {
        console.log("Checking user " + userid);
        return this.rest.executeResponseCall<string>("head", "redback", "users/" + userid, null).pipe(
            catchError((error: HttpErrorResponse) => {
                if (error.status==404) {
                    console.log("Status 404")
                    return [false];
                } else {
                    return throwError(this.rest.getTranslatedErrorResult(error));
                }
            }), map((httpResponse: HttpResponse<string>) => httpResponse.status == 200));
    }

    public userRoleTree(userid:string): Observable<RoleTree> {
        return this.rest.executeResponseCall<RoleTree>("get", "redback","users/"+userid+"/roletree", null).pipe(
            catchError((error: HttpErrorResponse)=>{
                if (error.status==404) {
                    console.error("User not found: " + userid);
                    return [];
                } else {
                    return throwError(this.rest.getTranslatedErrorResult(error));
                }
            })
        ).pipe(map((httpResponse:HttpResponse<RoleTree>)=>httpResponse.body))
    }

    public changeOwnPassword(current_password:string, password:string, confirm_password:string) {
        let data = {
            "user_id":this.userInfo.user_id,
            "current_password":current_password,
            "new_password":password,
            "new_password_confirmation":confirm_password
        }
        return this.rest.executeRestCall<any>("post", "redback", "users/me/password/update", data).pipe(
            catchError((error: HttpErrorResponse)=>{
                return throwError(this.rest.getTranslatedErrorResult(error));
            })
        );
    }

}
