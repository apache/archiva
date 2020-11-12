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
import {
    FormBuilder,
    FormGroup,
    Validators,
    FormControl,
    AsyncValidator,
    AbstractControl,
    ValidationErrors,
    ValidatorFn
} from '@angular/forms';
import {UserService} from "../../../../services/user.service";
import {User} from "../../../../model/user";
import {ErrorResult} from "../../../../model/error-result";
import {catchError, debounceTime, distinctUntilChanged, map, switchMap} from "rxjs/operators";
import {throwError, Observable, of, pipe, timer} from 'rxjs';
import {environment} from "../../../../../environments/environment";
import {UserInfo} from "../../../../model/user-info";

@Component({
    selector: 'app-manage-users-add',
    templateUrl: './manage-users-add.component.html',
    styleUrls: ['./manage-users-add.component.scss']
})
export class ManageUsersAddComponent implements OnInit {

    editProperties = ['user_id', 'full_name', 'email', 'locked', 'password_change_required',
        'password', 'confirm_password', 'validated'];
    minUserIdSize = environment.application.minUserIdLength;
    success: boolean = false;
    error: boolean = false;
    errorResult: ErrorResult;
    result: UserInfo;
    user: string;

    userForm = this.fb.group({
        user_id: ['', [Validators.required, Validators.minLength(this.minUserIdSize), whitespaceValidator()],this.userUidExistsValidator()],
        full_name: ['', Validators.required],
        email: ['', [Validators.required, Validators.email]],
        locked: [false],
        password_change_required: [true],
        password: [''],
        confirm_password: [''],
        validated: [true]
    }, {
        validator: MustMatch('password', 'confirm_password')
    })

    constructor(public userService: UserService, public fb: FormBuilder) {

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
            });
        }
    }


    public copyFromForm(properties: string[]): User {
        let user: any = new User();
        for (let prop of properties) {
            user[prop] = this.userForm.get(prop).value;
        }
        console.log("User " + user);
        return user;
    }

    public copyToForm(properties: string[], user: User): void {
        let propMap = {};
        for (let prop of properties) {
            let propValue = user[prop] == null ? '' : user[prop];
            propMap[prop] = propValue;
        }
        this.userForm.patchValue(propMap);
        console.log("User " + user);
    }


    valid(field: string): string[] {
        let formField = this.userForm.get(field);
        if (formField.dirty || formField.touched) {
            if (formField.valid) {
                return ['is-valid']
            } else {
                return ['is-invalid']
            }
        } else {
            return ['']
        }
    }

    getAllErrors(formGroup: FormGroup, errors: string[] = []) : string[] {
        Object.keys(formGroup.controls).forEach(field => {
            const control = formGroup.get(field);
            if (control instanceof FormControl && control.errors != null) {
                let keys = Object.keys(control.errors).map(errorKey=>field+'.'+errorKey);
                errors = errors.concat(keys);
            } else if (control instanceof FormGroup) {
                errors = errors.concat(this.getAllErrors(control));
            }
        });
        return errors;
    }

    getAttributeErrors(control:string):string[] {
        return Object.keys(this.userForm.get(control).errors);
    }

    /**
     * Async validator with debounce time
     * @constructor
     */
    userUidExistsValidator() {

        return (ctrl : FormControl) => {
            // debounceTimer() does not work here, as the observable is created with each keystroke
            // but angular does unsubscribe on previous started async observables.
            return timer(500).pipe(
                switchMap((userid) => this.userService.userExists(ctrl.value)),
                catchError(() => of(null)),
                map(exists => (exists ? {userexists: true} : null))
            );
        }
    }

    forbiddenNameValidator(nameRe: RegExp): ValidatorFn {
        return (control: AbstractControl): {[key: string]: any} | null => {
            const forbidden = nameRe.test(control.value);
            return forbidden ? {forbiddenName: {value: control.value}} : null;
        };
    }



}

export function whitespaceValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        const hasWhitespace =  /\s/g.test(control.value);
        return hasWhitespace ? {containsWhitespace: {value: control.value}} : null;
    };
}
export function MustMatch(controlName: string, matchingControlName: string) : ValidatorFn  {
    return (formGroup: FormGroup): ValidationErrors | null => {
        const control = formGroup.controls[controlName];
        const matchingControl = formGroup.controls[matchingControlName];

        if (matchingControl.errors && !matchingControl.errors.mustMatch) {
            // return if another validator has already found an error on the matchingControl
            return;
        }

        // set error on matchingControl if validation fails
        if (control.value !== matchingControl.value) {
            matchingControl.setErrors({mustMatch: true});
        } else {
            matchingControl.setErrors(null);
        }
    }
}

