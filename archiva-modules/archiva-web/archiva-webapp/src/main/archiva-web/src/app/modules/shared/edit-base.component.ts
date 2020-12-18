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

import {AbstractControl, FormBuilder, FormControl, FormGroup, ValidationErrors, ValidatorFn} from "@angular/forms";
import {ErrorResult} from '@app/model/error-result';

export abstract class EditBaseComponent<T> {

    editProperties = ['id'];
    success: boolean = false;
    error: boolean = false;
    errorResult: ErrorResult;
    result: T;
    formInitialValues;
    public userForm : FormGroup;
    public editMode: boolean;

    constructor(public fb: FormBuilder) {

    }

    init(userForm: FormGroup) : void {
        this.userForm=userForm;
        this.formInitialValues = userForm.value;
    }

    abstract createEntity() : T;
    abstract onSubmit();

    public copyFromForm(properties: string[]): T {
        let entity: any = this.createEntity();
        for (let prop of properties) {
            entity[prop] = this.userForm.get(prop).value;
        }
        return entity;
    }

    public copyToForm(properties: string[], user: T): void {
        let propMap = {};
        for (let prop of properties) {
            let propValue = user[prop] == null ? '' : user[prop];
            propMap[prop] = propValue;
        }
        this.userForm.patchValue(propMap);
    }


    valid(field: string): string[] {
        if (this.editMode) {
            let classArr = this.isValid(field);
            return classArr.concat('form-control')
        } else {
            return ['form-control-plaintext'];
        }
    }

    isValid(field: string): string[] {
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