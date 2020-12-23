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

import {Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {FormBuilder} from '@angular/forms';
import {UserService} from "@app/services/user.service";
import {ErrorResult} from "@app/model/error-result";
import {catchError} from "rxjs/operators";
import {UserInfo} from "@app/model/user-info";
import {ManageUsersBaseComponent} from "../manage-users-base.component";
import {ToastService} from "@app/services/toast.service";
import {ErrorMessage} from "@app/model/error-message";

@Component({
    selector: 'app-manage-users-add',
    templateUrl: './manage-users-add.component.html',
    styleUrls: ['./manage-users-add.component.scss']
})
export class ManageUsersAddComponent extends ManageUsersBaseComponent implements OnInit {

    @ViewChild('errorTmpl') public errorTmpl: TemplateRef<any>;
    @ViewChild('successTmpl') public successTmpl: TemplateRef<any>;

    constructor(userService: UserService, fb: FormBuilder, private toastService: ToastService) {
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
                this.toastService.showError('manage-users-add',this.errorTmpl,{contextData:this.errorResult})

                return [];
                // return throwError(error);
            })).subscribe((user: UserInfo) => {
                this.result = user;
                this.success = true;
                this.error = false;
                this.toastService.showSuccess('manage-users-add',this.successTmpl,{contextData:this.result})
                this.userForm.reset(this.formInitialValues);
            });
        }
    }



    showMessage() {
        this.result=new UserInfo()
        this.result.user_id='XXXXX'
        const errorResult : ErrorResult = new ErrorResult([
            ErrorMessage.of('Not so good'),
            ErrorMessage.of('Completely crap')
        ]);
        console.log(JSON.stringify(errorResult));
        errorResult.status=422;
        this.toastService.showSuccess('manage-users-add',this.successTmpl,{contextData:this.result,delay:1000})
        this.toastService.showError('manage-users-add',this.errorTmpl,{contextData:errorResult,delay:10000})
    }

}



