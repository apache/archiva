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

import { Component, OnInit } from '@angular/core';
import {EditBaseComponent} from "@app/modules/shared/edit-base.component";
import {LdapConfiguration} from "@app/model/ldap-configuration";
import {ActivatedRoute} from "@angular/router";
import {AbstractControl, FormBuilder, ValidatorFn, Validators} from "@angular/forms";
import {SecurityService} from "@app/services/security.service";
import {ToastService} from "@app/services/toast.service";
import {propertyDescriptorPatch} from "zone.js/lib/browser/property-descriptor";

@Component({
  selector: 'app-ldap-security',
  templateUrl: './ldap-security.component.html',
  styleUrls: ['./ldap-security.component.scss']
})
export class LdapSecurityComponent extends EditBaseComponent<LdapConfiguration> implements OnInit {

  authenticationMethods=['none','simple','strong']

  constructor(private route: ActivatedRoute,
              public fb: FormBuilder, private securityService: SecurityService, private toastService: ToastService) {
    super(fb);
    super.init(fb.group({
      host_name:[''],
      port:['', [Validators.min(1), Validators.max(65535)]],
      ssl_enabled:[false],
      base_dn:['',[dnValidator()]],
      groups_base_dn:['',[dnValidator()]],
      bind_dn:['',[dnValidator()]],
      bind_password:[''],
      authentication_method:['none'],
      bind_authenticator_enabled:[false],
      use_role_name_as_group:[true],
      writable:[false]
    }, {}));
  }

  ngOnInit(): void {
  }

  createEntity(): LdapConfiguration {
    return new LdapConfiguration();
  }

  onSubmit() {
  }

  getInputClasses(field: string) : string[] {
    let csClasses = super.isValid(field);
    if (csClasses.length==1 && csClasses[0]=='') {
      csClasses = [];
    }
    csClasses.push('form-control');
    console.log("Classes "+field+" " + csClasses);
    return csClasses;
  }
}

export function dnValidator(): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} | null => {
    let parts = []
    let value = control.value.toString()
    let escape = false;
    let partKey : string = ''
    let partValue : string = ''
    let key = true;
    for (let i=0; i<value.length; i+=1) {
      let c = value.charAt(i);
      if (c=='\\' && !escape) {
        escape = true;
      } else if (c==',' && !escape) {
        parts.push( [partKey,partValue]);
        if (partKey.length==0) {
          return {'invalidDnBadKey':{value:value,index:i}}
        }
        if (partValue.length==0) {
          return {'invalidDnBadValue':{value:value, index: i}}
        }
        partKey='';
        partValue='';
        key=true;
        continue;
      } else if (c=='=' && !escape) {
        if (!key) {
          return {'invalidDnBadEquals':{value: value, index:i}}
        }
        key=false;
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
    return null;
  };
}
