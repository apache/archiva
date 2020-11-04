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
import {ArchivaRequestService} from "./archiva-request.service";
import {UserInfo} from '../model/user-info';
import {HttpErrorResponse} from "@angular/common/http";
import {ErrorResult} from "../model/error-result";
import {Observable} from "rxjs";
import {Permission} from '../model/permission';

@Injectable({
    providedIn: 'root'
})
export class UserService implements OnInit, OnDestroy {

    userInfo: UserInfo;
    permissions: Permission[];
    guestPermissions: Permission[];
    authenticated: boolean;
    uiPermissionsDefault  = {
        'menu': {
            'repo':{
                'section':true,
                'browse':true,
                'search':true,
                'upload':false
            },
            'admin':{
                'section':false,
                'config':false,
                'status':false,
                'reports':false
            },
            'user':{
                'section':false,
                'manage':false,
                'roles':false,
                'config':false
            }
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
                    console.log("Could not retrieve permissions "+err);
                }
            }
            this.retrievePermissionInfo().subscribe(observer);
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
                        if (result != null && result.errorMessages != null) {
                            for (let msg of result.errorMessages) {
                                console.error('Observer got an error: ' + msg.errorKey)
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
    public retrievePermissionInfo(): Observable<Permission[]> {
        return new Observable<Permission[]>((resultObserver) => {
            let userName = this.authenticated ? "me" : "guest";
            let infoObserver = this.rest.executeRestCall<Permission[]>("get", "redback", "users/" + userName + "/permissions", null);
            let permissionObserver = {
                next: (x: Permission[]) => {
                    this.permissions = x;
                    this.parsePermissions(x);
                    resultObserver.next(this.permissions);
                },
                error: (err: HttpErrorResponse) => {
                    console.log("Error " + (JSON.stringify(err)));
                    let result = err.error as ErrorResult
                    if (result.errorMessages != null) {
                        for (let msg of result.errorMessages) {
                            console.debug('Observer got an error: ' + msg.errorKey)
                        }
                    }
                    this.resetPermissions();
                    resultObserver.error(err);
                },
                complete: () => {
                    resultObserver.complete();
                }
            };
            infoObserver.subscribe(permissionObserver);

        });
    }

    resetPermissions() {
        this.deepCopy(this.uiPermissionsDefault, this.uiPermissions);
    }
    parsePermissions(permissions: Permission[]) {
        this.resetPermissions();
        for ( let perm of permissions) {
            // console.debug("Checking permission for op: " + perm.operation.name);
            switch (perm.operation.name) {
                case "archiva-manage-configuration": {
                    if (perm.resource.identifier=='*') {
                        this.uiPermissions.menu.admin.section = true;
                        this.uiPermissions.menu.admin.config = true;
                        this.uiPermissions.menu.admin.reports = true;
                        this.uiPermissions.menu.admin.status = true;
                    }

                }
                case "archiva-manage-users": {
                    if (perm.resource.identifier=='*') {
                        this.uiPermissions.menu.user.section = true;
                        this.uiPermissions.menu.user.config = true;
                        this.uiPermissions.menu.user.manage = true;
                        this.uiPermissions.menu.user.roles = true;
                    }
                }
                case "redback-configuration-edit": {
                    if (perm.resource.identifier=='*') {
                        this.uiPermissions.menu.user.section = true;
                        this.uiPermissions.menu.user.config = true;
                    }
                }
                case "archiva-upload-file": {
                    this.uiPermissions.menu.repo.upload = true;
                }
            }
        }
        console.log("New permissions: " + JSON.stringify(this.uiPermissions));
    }

    private deepCopy(src: Object, dst: Object) {
        Object.keys(src).forEach((key, idx) => {
            let srcEl = src[key];
            if (typeof(srcEl)=='object' ) {
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

}
