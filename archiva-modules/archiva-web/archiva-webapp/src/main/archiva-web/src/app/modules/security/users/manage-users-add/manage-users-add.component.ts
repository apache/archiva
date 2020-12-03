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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import {Component, OnInit} from '@angular/core';
import {AbstractControl, FormBuilder, FormControl, FormGroup, ValidationErrors, ValidatorFn} from '@angular/forms';
import {UserService} from "../../../../services/user.service";
import {ErrorResult} from "../../../../model/error-result";
import {catchError} from "rxjs/operators";
import {UserInfo} from "../../../../model/user-info";
import {ManageUsersBaseComponent} from "../manage-users-base.component";

@Component({
    selector: 'app-manage-users-add',
    templateUrl: './manage-users-add.component.html',
    styleUrls: ['./manage-users-add.component.scss']
})
export class ManageUsersAddComponent extends ManageUsersBaseComponent implements OnInit {

    constructor(userService: UserService, fb: FormBuilder) {
        super(userService, fb);

    }

    ngOnInit(): void {
    }

    onSubmit() {
        // Process checkout data here
        this.result = null;
        if (this.userForm.valid) {
            let user = this.copyFromForm(this.editProperties);
            console.info('Adding user ' + user);
            this.userService.addUser(user).pipe(catchError((error: ErrorResult) => {
                // console.log("Error " + error + " - " + typeof (error) + " - " + JSON.stringify(error));
                if (error.status == 422) {
                    // console.warn("Validation error");
                    let pwdErrors = {};
                    for (let message of error.error_messages) {
                        if (message.error_key.startsWith('user.password.violation')) {
                            pwdErrors[message.error_key] = message.message;
                        }
                    }
                    this.userForm.get('password').setErrors(pwdErrors);

                }
                this.errorResult = error;
                this.success = false;
                this.error = true;
                return [];
                // return throwError(error);
            })).subscribe((user: UserInfo) => {
                this.result = user;
                this.success = true;
                this.error = false;
                this.userForm.reset(this.formInitialValues);
            });
        }
    }





}



