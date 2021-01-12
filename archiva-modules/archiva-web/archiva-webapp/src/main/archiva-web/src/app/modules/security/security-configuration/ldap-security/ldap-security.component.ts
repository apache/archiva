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

import {Component, OnInit} from '@angular/core';
import {EditBaseComponent} from "@app/modules/shared/edit-base.component";
import {LdapConfiguration} from "@app/model/ldap-configuration";
import {ActivatedRoute} from "@angular/router";
import {AbstractControl, FormBuilder, ValidatorFn, Validators} from "@angular/forms";
import {SecurityService} from "@app/services/security.service";
import {ToastService} from "@app/services/toast.service";
import {ErrorResult} from "@app/model/error-result";
import {Observable} from 'rxjs';
import {debounceTime, distinctUntilChanged, map} from 'rxjs/operators';
import {HttpResponse} from "@angular/common/http";
import {PropertyMap} from "@app/model/property-map";

@Component({
    selector: 'app-ldap-security',
    templateUrl: './ldap-security.component.html',
    styleUrls: ['./ldap-security.component.scss']
})
export class LdapSecurityComponent extends EditBaseComponent<LdapConfiguration> implements OnInit {

    authenticationMethods = ['none', 'simple', 'strong']
    formFields = ['host_name', 'port', 'ssl_enabled', 'context_factory',
        'base_dn', 'groups_base_dn', 'bind_dn', 'bind_password', 'authentication_method', 'bind_authenticator_enabled',
        'use_role_name_as_group', 'writable'];
    availableContextFactories = [];
    ldapProperties : PropertyMap = new PropertyMap();
    checkResult = null;
    submitError = null;
    checkProgress=false;

    constructor(private route: ActivatedRoute,
                public fb: FormBuilder, private securityService: SecurityService, private toastService: ToastService) {
        super(fb);
        super.init(fb.group({
            host_name: [''],
            port: ['', [Validators.min(1), Validators.max(65535)]],
            ssl_enabled: [false],
            context_factory: [''],
            base_dn: ['', [dnValidator()]],
            groups_base_dn: ['', [dnValidator()]],
            bind_dn: [''],
            bind_password: [''],
            authentication_method: ['none'],
            bind_authenticator_enabled: [false],
            use_role_name_as_group: [true],
            writable: [false],
            prop_key:[''],
            prop_value:['']
        }, {}));
    }

    ngOnInit(): void {
        this.securityService.getLdapConfiguration().subscribe(ldapConfiguration => {
                this.copyToForm(this.formFields, ldapConfiguration);
                if ((ldapConfiguration.context_factory == null || ldapConfiguration.context_factory == '') && ldapConfiguration.available_context_factories.length == 1) {
                    this.userForm.controls['context_factory'].setValue(ldapConfiguration.available_context_factories[0]);
                }
                if (ldapConfiguration.authentication_method == '') {
                    this.userForm.controls['authentication_method'].setValue('none');
                }
                this.availableContextFactories = ldapConfiguration.available_context_factories;
            console.log("Props: " + ldapConfiguration.properties + " " + typeof (ldapConfiguration.properties));
                if (ldapConfiguration.properties) {
                    this.ldapProperties = ldapConfiguration.properties as PropertyMap;
                } else {
                    this.ldapProperties = new PropertyMap();
                }
            }
        )
        this.userForm.controls['bind_dn'].valueChanges.subscribe(selectedValue => {
            if (selectedValue != '' && this.userForm.controls['authentication_method'].value == 'none') {
                this.userForm.controls['authentication_method'].setValue('simple', {emitEvent: false})
            } else if (selectedValue == '' && this.userForm.controls['authentication_method'].value != 'none') {
                this.userForm.controls['authentication_method'].setValue('none', {emitEvent: false})
            }
        })
        this.userForm.valueChanges.subscribe(() => {
            this.checkResult = null;
        })

    }

    createEntity(): LdapConfiguration {
        return new LdapConfiguration();
    }

    onSubmit() {
        console.log("Saving configuration");
        let config = this.copyFromForm(this.formFields)
        if (this.ldapProperties) {
            config.properties = this.ldapProperties;
        }
        this.securityService.updateLdapConfiguration(config).subscribe((response: HttpResponse<LdapConfiguration>)=> {
                this.toastService.showSuccessByKey('ldap-security', 'security.config.ldap.submit_success');
                this.userForm.reset();
                this.submitError=null;
                this.checkResult=null;
                this.copyToForm(this.formFields, response.body)
        },
            (error: ErrorResult) =>{
                this.toastService.showSuccessByKey('ldap-security', 'security.config.ldap.submit_error', {error:error.toString()});
                this.submitError = error;
                this.checkResult=null;
            }

        );


    }

    searchContextFactory = (text$: Observable<string>) =>
        text$.pipe(
            debounceTime(200),
            distinctUntilChanged(),
            map(term => term.length < 1 ? []
                : this.availableContextFactories.filter(v => v.toLowerCase().indexOf(term.toLowerCase()) > -1).slice(0, 10))
        )

    getInputClasses(field: string): string[] {
        let csClasses = super.isValid(field);
        if (csClasses.length == 1 && csClasses[0] == '') {
            csClasses = [];
        }
        csClasses.push('form-control');
        if (field=='port') {
            csClasses.push('text-right')
        }
        return csClasses;
    }

    checkLdapConnection() {
        console.log("Checking LDAP connection");
        let config = this.copyFromForm(this.formFields)
        if (this.ldapProperties) {
            config.properties = this.ldapProperties;
        }
        this.checkProgress=true;
        this.securityService.verifyLdapConfiguration(config).subscribe(() => {
                this.toastService.showSuccessByKey('ldap-security', 'security.config.ldap.check_success');
                this.checkResult = 'success';
                this.checkProgress=false;
            },
            (error: ErrorResult) => {
                this.toastService.showErrorByKey('ldap-security', error.firstMessageString());
                this.checkResult = 'error';
                this.checkProgress=false;
            }
        );
    }

    addProperty() {
        let key = this.userForm.controls['prop_key'].value
        let value = this.userForm.controls['prop_value'].value

        console.log("Prop " + key + " = " + value);
        if (key && key!='') {
            setTimeout(() => {
                this.ldapProperties.set(key, value);
                this.userForm.markAsDirty();
            });
        }
        this.userForm.controls['prop_key'].setValue('')
        this.userForm.controls['prop_value'].setValue('')
    }

    removeProperty(key:string) {
        setTimeout(()=>{
            this.ldapProperties.delete(key);
            this.userForm.markAsDirty();
        })
    }
}

/**
 * This validator checks the DN names for valid RDN segments
 */
export function dnValidator(): ValidatorFn {
    return (control: AbstractControl): { [key: string]: any } | null => {
        let parts = []
        if (control.value==null) {
            return null;
        }
        let value = control.value.toString()
        if (value == '') {
            return null;
        }
        let escape = false;
        let partKey: string = ''
        let partValue: string = ''
        let key = true;
        for (let i = 0; i < value.length; i += 1) {
            let c = value.charAt(i);
            if (c == '\\' && !escape) {
                escape = true;
            } else if (c == ',' && !escape) {
                parts.push([partKey, partValue]);
                if (partKey.length == 0) {
                    return {'invalidDnBadKey': {value: value, index: i}}
                }
                if (partValue.length == 0) {
                    return {'invalidDnBadValue': {value: value, index: i}}
                }
                partKey = '';
                partValue = '';
                key = true;
                continue;
            } else if (c == '=' && !escape) {
                if (!key) {
                    return {'invalidDnBadEquals': {value: value, index: i}}
                }
                key = false;
                continue;
            } else if (escape) {
                escape = false;
            }
            if (key) {
                partKey = partKey + c;
            } else {
                partValue = partValue + c;
            }

        }
        if (partKey == '' || partValue == '') {
            return {'invalidDnBadRdn': {value: value, index: value.length - 1}}
        }
        return null;
    };
}
