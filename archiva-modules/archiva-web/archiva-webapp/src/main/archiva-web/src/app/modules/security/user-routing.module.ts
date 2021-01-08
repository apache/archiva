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
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { ManageUsersComponent } from './users/manage-users/manage-users.component';
import { ManageUsersListComponent } from './users/manage-users-list/manage-users-list.component';
import { ManageUsersAddComponent } from './users/manage-users-add/manage-users-add.component';
import { ManageUsersEditComponent } from './users/manage-users-edit/manage-users-edit.component';
import { ManageUsersDeleteComponent } from './users/manage-users-delete/manage-users-delete.component';
import {ManageUsersRolesComponent} from "./users/manage-users-roles/manage-users-roles.component";
import {RoutingGuardService as Guard} from "@app/services/routing-guard.service";


/**
 * You can use Guard (RoutingGuardService) for permission checking. The service needs data with one parameter 'perm',
 * that gives the path of the uiPermission map of the user service.
 */

const routes: Routes = [
      { path: '', component: ManageUsersComponent,canActivate:[Guard],
        data: { perm: 'menu.security.users' },
        children: [
          {path: 'list', component: ManageUsersListComponent},
          {path: 'add', component: ManageUsersAddComponent},
          {path: 'edit/:userid', component: ManageUsersEditComponent},
          {path: 'edit', redirectTo:'edit/guest' },
          {path: 'delete/:userid', component: ManageUsersDeleteComponent},
          {path: 'roles', component:ManageUsersRolesComponent},
          {path: 'roles/:userid', component:ManageUsersRolesComponent},
          {path: '', redirectTo:'list',pathMatch:'full'}
        ]
      }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [],
  declarations: []
})
export class UserRoutingModule { }

