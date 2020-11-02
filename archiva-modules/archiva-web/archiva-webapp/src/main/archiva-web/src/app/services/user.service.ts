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
import {ArchivaRequestService} from "./archiva-request.service";
import {UserInfo} from '../model/user-info';
import {HttpErrorResponse} from "@angular/common/http";
import {ErrorResult} from "../model/error-result";
import {Observable} from "rxjs";

@Injectable({
    providedIn: 'root'
})
export class UserService {

    userInfo: UserInfo;

    constructor(private rest: ArchivaRequestService) {
        this.userInfo = new UserInfo()
        this.loadPersistedUserInfo();
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
                        resultObserver.next(this.userInfo);
                    },
                    error: (err: HttpErrorResponse) => {
                        console.log("Error " + (JSON.stringify(err)));
                        let result = err.error as ErrorResult
                        if (result.errorMessages != null) {
                            for (let msg of result.errorMessages) {
                                console.error('Observer got an error: ' + msg.errorKey)
                            }
                        }
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
    }

}
