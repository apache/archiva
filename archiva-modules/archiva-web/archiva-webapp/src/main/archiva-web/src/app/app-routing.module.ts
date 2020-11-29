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
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { AboutComponent } from './modules/shared/about/about.component';
import { ContactComponent } from './modules/shared/contact/contact.component';
import { HomeComponent } from './modules/shared/home/home.component';
import { NotFoundComponent } from './modules/shared/not-found/not-found.component';
import { LoginComponent } from "./modules/shared/login/login.component";
import { SearchComponent } from './modules/repo/search/search.component';
import {BrowseComponent} from "./modules/repo/browse/browse.component";
import {UploadComponent} from "./modules/repo/upload/upload.component";
import {ManageRolesComponent} from "./modules/user/manage-roles/manage-roles.component";
import {SecurityConfigurationComponent} from "./modules/user/security-configuration/security-configuration.component";
import {RoutingGuardService as Guard} from "./services/routing-guard.service";
import {UserModule} from "./modules/user/user.module";

/**
 * You can use Guard (RoutingGuardService) for permission checking. The service needs data with one parameter 'perm',
 * that gives the path of the uiPermission map of the user service.
 */

const routes: Routes = [
  { path: '', component: HomeComponent,
  children: [
    {path:'repo/search', component: SearchComponent},
    {path:'repo/browse', component: BrowseComponent},
    {path:'repo/upload', component: UploadComponent},
    {path:'', redirectTo:'repo/search', pathMatch:'full'},
  ]},
  { path: 'users', loadChildren: () => import('./modules/user/user.module').then(m => m.UserModule)
  },
  { path: 'user', component: HomeComponent,canActivate:[Guard],data:{perm: 'menu.user.section'},
    children: [
      { path: 'roles', component: ManageRolesComponent },
      { path: 'config', component: SecurityConfigurationComponent},
    ]
  },
  { path: 'contact', component: ContactComponent },
  { path: 'about', component: AboutComponent },
  { path: 'login', component: LoginComponent },
  { path: 'logout', component: HomeComponent },
  { path: '**', component: NotFoundComponent }
  ,
  {
    path: '',
    redirectTo: '',
    pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes), UserModule],
  exports: [RouterModule],
  declarations: []
})
export class AppRoutingModule { }

