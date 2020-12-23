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

import {environment} from "../../../../environments/environment";
import {ErrorResult} from "../../../model/error-result";
import {UserInfo} from "../../../model/user-info";
import {
    AbstractControl,
    FormControl,
    ValidatorFn,
    Validators,
    FormBuilder,
    ValidationErrors,
    FormGroup
} from "@angular/forms";
import {User} from "../../../model/user";
import {of, timer} from "rxjs";
import {catchError, map, switchMap} from "rxjs/operators";
import { UserService } from 'src/app/services/user.service';

export class ManageUsersBaseComponent {

    editProperties = ['user_id', 'full_name', 'email', 'locked', 'password_change_required',
        'password', 'confirm_password', 'validated'];
    minUserIdSize = environment.application.minUserIdLength;
    success: boolean = false;
    error: boolean = false;
    errorResult: ErrorResult;
    result: UserInfo;
    user: string;
    formInitialValues;

    userForm = this.fb.group({
        user_id: ['', [Validators.required, Validators.minLength(this.minUserIdSize), whitespaceValidator()], this.userUidExistsValidator()],
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
        this.formInitialValues=this.userForm.value
    }


    public copyFromForm(properties: string[]): User {
        let user: any = new User();
        for (let prop of properties) {
            user[prop] = this.userForm.get(prop).value;
        }
        // console.log("User " + user);
        return user;
    }

    public copyToForm(properties: string[], user: User): void {
        let propMap = {};
        for (let prop of properties) {
            let propValue = user[prop] == null ? '' : user[prop];
            propMap[prop] = propValue;
        }
        this.userForm.patchValue(propMap);
        // console.log("User " + user);
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

    public getErrorsFor(formField:string) : string[] {
        let field=this.userForm.get(formField)
        if (field) {
            if (field.errors) {
                return Object.values(field.errors);
            }
        }
        return []
    }

    /**
     * Async validator with debounce time
     * @constructor
     */
    userUidExistsValidator() {

        return (ctrl: FormControl) => {
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
        return (control: AbstractControl): { [key: string]: any } | null => {
            const forbidden = nameRe.test(control.value);
            return forbidden ? {forbiddenName: {value: control.value}} : null;
        };
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