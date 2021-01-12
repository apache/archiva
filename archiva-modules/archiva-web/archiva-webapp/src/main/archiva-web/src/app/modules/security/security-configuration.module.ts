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

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from "@app/modules/shared/shared.module";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {SecurityConfigurationComponent} from "./security-configuration/security-configuration.component";
import {SecurityConfigurationRoutingModule} from "@app/modules/security/security-configuration-routing.module";
import { BaseSecurityComponent } from './security-configuration/base-security/base-security.component';
import { LdapSecurityComponent } from './security-configuration/ldap-security/ldap-security.component';
import { SecurityPropertiesComponent } from './security-configuration/security-properties/security-properties.component';
import { CacheSettingComponent } from './security-configuration/cache-setting/cache-setting.component';


@NgModule({
    declarations: [
        SecurityConfigurationComponent,
        BaseSecurityComponent,
        LdapSecurityComponent,
        SecurityPropertiesComponent,
        CacheSettingComponent
    ],
    exports: [
        SecurityConfigurationComponent
    ],
    imports: [
        CommonModule,
        SharedModule,
        FormsModule,
        ReactiveFormsModule,
        SecurityConfigurationRoutingModule
    ]
})
export class SecurityConfigurationModule {
}
