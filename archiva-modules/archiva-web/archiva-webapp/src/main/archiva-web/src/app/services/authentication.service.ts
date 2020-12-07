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
import {EventEmitter, Injectable} from '@angular/core';
import {ArchivaRequestService} from "./archiva-request.service";
import {AccessToken} from "../model/access-token";
import {environment} from "../../environments/environment";
import {ErrorMessage} from "../model/error-message";
import {ErrorResult} from "../model/error-result";
import {HttpErrorResponse} from "@angular/common/http";
import {UserService} from "./user.service";
import {UserInfo} from "../model/user-info";

/**
 * The AuthenticationService handles user authentication and stores user data after successful login
 */
@Injectable({
    providedIn: 'root'
})
export class AuthenticationService {
    authenticated: boolean;
    authenticating: boolean;

    /**
     * The LoginEvent is emitted, when a successful login happened. And the corresponding user info was retrieved.
     */
    public LoginEvent: EventEmitter<UserInfo> = new EventEmitter<UserInfo>();

    /**
     * The LogoutEvent is emitted, when the user has been logged out.
     */
    public LogoutEvent: EventEmitter<any> = new EventEmitter<any>();


    constructor(private rest: ArchivaRequestService,
                private userService: UserService) {
        this.authenticated = false;
        this.LoginEvent.subscribe((info)=>{
            this.authenticating=false;
        })
        this.LogoutEvent.subscribe(()=>{
            this.authenticating=false;
        })
        this.restoreLoginData();
    }

    private restoreLoginData() {
        console.debug("Restoring login data");
        let accessToken = localStorage.getItem("access_token");
        if (accessToken != null) {
            let expirationDate = localStorage.getItem("token_expire");
            if (expirationDate != null) {
                let expDate = new Date(expirationDate);
                let currentDate = new Date();
                if (currentDate < expDate) {
                    console.debug("Retrieving user information");
                    this.authenticating=true;
                    let observer = this.userService.retrieveUserInfo();
                    observer.subscribe({
                            next: (userInfo: UserInfo) => {
                                if (userInfo != null) {
                                    let permObserver = this.userService.retrievePermissionInfo();
                                    permObserver.subscribe({
                                            next: () => {
                                                this.authenticated = true;
                                                this.LoginEvent.emit(userInfo)
                                            },
                                            error: (err) => {
                                                console.debug("Error retrieving perms: " + JSON.stringify(err));
                                            }
                                        }
                                    )
                                }
                            },
                            error: (err: HttpErrorResponse) => {
                                console.debug("Error retrieving user info: " + JSON.stringify(err));
                                this.logout();
                            },
                            complete: () => {
                                this.authenticating=false;
                            }
                        }
                    );
                } else {
                    this.logout();
                }
            } else {
                this.logout();
            }
        }


    }


    /**
     * Tries to login by sending the login data to the REST service. If the login was successful the access
     * and refresh token is stored locally.
     *
     * @param userid The user id for the login
     * @param password The password
     * @param resultHandler A result handler that is executed, after calling the login service
     */
    login(userid: string, password: string, resultHandler: (n: string, err?: ErrorMessage[]) => void) {
        this.authenticating=true;
        const data = {
            'grant_type': 'authorization_code',
            'client_id': environment.application.client_id,
            'user_id': userid, 'password': password
        };
        let authObserver = this.rest.executeRestCall<AccessToken>('post', 'redback', 'auth/authenticate', data);
        let tokenObserver = {
            next: (x: AccessToken) => {
                localStorage.setItem("access_token", x.access_token);
                localStorage.setItem("refresh_token", x.refresh_token);
                if (x.expires_in != null) {
                    let dt = new Date();
                    dt.setSeconds(dt.getSeconds() + x.expires_in);
                    localStorage.setItem("token_expire", dt.toISOString());
                }
                let userObserver = this.userService.retrieveUserInfo();
                this.authenticated = true;
                userObserver.subscribe(userInfo => {
                    if (userInfo != null) {
                        let permObserver = this.userService.retrievePermissionInfo();
                        permObserver.subscribe((perms) => {
                                this.LoginEvent.emit(userInfo);
                            }
                        )
                    }
                });
                resultHandler("OK");
            },
            error: (err: HttpErrorResponse) => {
                this.authenticating = false;
                console.log("Error " + (JSON.stringify(err)));
                let result = err.error as ErrorResult
                if (result.error_messages != null) {
                    for (let msg of result.error_messages) {
                        console.debug('Observer got an error: ' + msg.error_key)
                    }
                    resultHandler("ERROR", result.error_messages);
                } else {
                    resultHandler("ERROR", null);
                }

            },
            complete: () => {
                this.authenticating = false;
            }
        };
        authObserver.subscribe(tokenObserver)

    }

    /**
     * Resets the stored user data
     */
    logout() {
        localStorage.removeItem("access_token");
        localStorage.removeItem("refresh_token");
        localStorage.removeItem("token_expire");
        this.authenticated = false;
        this.userService.resetUser();
        this.rest.resetToken();
        this.LogoutEvent.emit();
    }
}
