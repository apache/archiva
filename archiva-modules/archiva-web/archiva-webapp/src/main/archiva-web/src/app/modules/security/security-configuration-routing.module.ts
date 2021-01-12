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
import {RouterModule, Routes} from '@angular/router';
import {RoutingGuardService as Guard} from "@app/services/routing-guard.service";
import {SecurityConfigurationComponent} from "./security-configuration/security-configuration.component";
import {BaseSecurityComponent} from "./security-configuration/base-security/base-security.component";
import {LdapSecurityComponent} from "./security-configuration/ldap-security/ldap-security.component";
import {SecurityPropertiesComponent} from "@app/modules/security/security-configuration/security-properties/security-properties.component";
import {CacheSettingComponent} from "@app/modules/security/security-configuration/cache-setting/cache-setting.component";


/**
 * You can use Guard (RoutingGuardService) for permission checking. The service needs data with one parameter 'perm',
 * that gives the path of the uiPermission map of the user service.
 */

const routes: Routes = [
    {
        path: '', component: SecurityConfigurationComponent, canActivate: [Guard],
        data: {perm: 'menu.security.config'},
        children: [
            {path: 'base', component: BaseSecurityComponent},
            {path: 'properties', component: SecurityPropertiesComponent},
            {path: 'ldap', component: LdapSecurityComponent},
            {path: 'cache', component: CacheSettingComponent},
            {path: '', redirectTo: 'base', pathMatch: 'full'}
        ]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [],
    declarations: []
})
export class SecurityConfigurationRoutingModule {
}

