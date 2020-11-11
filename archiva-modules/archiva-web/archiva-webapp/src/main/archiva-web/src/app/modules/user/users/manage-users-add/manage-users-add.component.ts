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
import {Validators, FormBuilder, FormGroup} from '@angular/forms';
import {UserService} from "../../../../services/user.service";
import {User} from "../../../../model/user";
import { UserInfo } from 'src/app/model/user-info';
import {HttpErrorResponse} from "@angular/common/http";
import {ErrorResult} from "../../../../model/error-result";
import {catchError} from "rxjs/operators";
import {of, throwError} from 'rxjs';

@Component({
    selector: 'app-manage-users-add',
    templateUrl: './manage-users-add.component.html',
    styleUrls: ['./manage-users-add.component.scss']
})
export class ManageUsersAddComponent implements OnInit {

    minUserIdSize=8;
    success:boolean=false;
    error:boolean=false;
    errorResult:ErrorResult;
    result:string;
    userid:string;

    userForm = this.fb.group({
        user_id: ['', [Validators.required, Validators.minLength(this.minUserIdSize)]],
        full_name: ['', Validators.required],
        email: ['', [Validators.required,Validators.email]],
        locked: [false],
        password_change_required: [true],
        password: [''],
        confirm_password: [''],
    }, {
        validator: MustMatch('password', 'confirm_password')
    })

    constructor(private userService: UserService, private fb: FormBuilder) {

    }

    ngOnInit(): void {
    }

    onSubmit() {
        // Process checkout data here
        this.result=null;
        if (this.userForm.valid) {
            let user = this.copyForm(['user_id','full_name','email','locked','password_change_required',
            'password','confirm_password'])
            console.info('Adding user ' + user);
            this.userService.addUser(user).pipe(catchError((error : ErrorResult)=> {
                console.log("Error " + error + " - " + typeof (error) + " - " + JSON.stringify(error));
                if (error.status==422) {
                    console.warn("Validation error");

                }
                this.errorResult = error;
                this.success=false;
                this.error=true;
                return throwError(error);
            })).subscribe((location : string ) => {
                this.result = location;
                this.success=true;
                this.error = false;
                this.userid = location.substring(location.lastIndexOf('/') + 1);
            });
        }
    }


    private copyForm(properties:string[]) : User {
        let user : any  = new User();
        for (let prop of properties) {
            user[prop] = this.userForm.get(prop).value;
        }
        console.log("User " + user);
        return user;
    }


    valid(field:string) : string {
      let formField = this.userForm.get(field);
      if (formField.dirty||formField.touched) {
        if (formField.valid) {
          return 'is-valid'
        } else {
          return 'is-invalid'
        }
      } else {
        return ''
      }
    }




}

export function MustMatch(controlName: string, matchingControlName: string) {
    return (formGroup: FormGroup) => {
        const control = formGroup.controls[controlName];
        const matchingControl = formGroup.controls[matchingControlName];

        if (matchingControl.errors && !matchingControl.errors.mustMatch) {
            // return if another validator has already found an error on the matchingControl
            return;
        }

        // set error on matchingControl if validation fails
        if (control.value !== matchingControl.value) {
            matchingControl.setErrors({ mustMatch: true });
        } else {
            matchingControl.setErrors(null);
        }
    }
}