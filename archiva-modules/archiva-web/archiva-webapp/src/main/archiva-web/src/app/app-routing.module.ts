/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {AboutComponent} from './modules/shared/about/about.component';
import {ContactComponent} from './modules/shared/contact/contact.component';
import {HomeComponent} from './modules/shared/home/home.component';
import {NotFoundComponent} from './modules/shared/not-found/not-found.component';
import {LoginComponent} from "./modules/shared/login/login.component";
import {SearchComponent} from './modules/repo/search/search.component';
import {BrowseComponent} from "./modules/repo/browse/browse.component";
import {UploadComponent} from "./modules/repo/upload/upload.component";
import {RoutingGuardService as Guard} from "./services/routing-guard.service";
import {SecurityConfigurationComponent} from "./modules/security/security-configuration/security-configuration.component";
import {UserInfoComponent} from "@app/modules/shared/user-info/user-info.component";

/**
 * You can use Guard (RoutingGuardService) for permission checking. The service needs data with one parameter 'perm',
 * that gives the path of the uiPermission map of the user service.
 */

const routes: Routes = [
    {
        path: '', component: HomeComponent,
        children: [
            {path: 'repo/search', component: SearchComponent},
            {path: 'repo/browse', component: BrowseComponent},
            {path: 'repo/upload', component: UploadComponent},
            {path: '', redirectTo: 'repo/search', pathMatch: 'full'},
        ]
    },

    {
        path: 'security', component: HomeComponent,canActivate:[Guard],data:{perm: 'menu.security.section'},
        children: [
            {path: 'users', loadChildren: () => import('@app/modules/security/user.module').then(m => m.UserModule)},
            {path: 'roles', loadChildren: () => import('@app/modules/security/role.module').then(m => m.RoleModule)},
            {path: 'config', loadChildren: () => import('@app/modules/security/security-configuration.module').then(m => m.SecurityConfigurationModule)},
        ]
    },
    {path: 'contact', component: ContactComponent},
    {path: 'me/info', component: UserInfoComponent},
    {path: 'about', component: AboutComponent},
    {path: 'login', component: LoginComponent},
    {path: 'logout', component: HomeComponent},
    {path: '**', component: NotFoundComponent}

];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule],
    declarations: []
})
export class AppRoutingModule {
}

