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
import {ManageRolesComponent} from "@app/modules/security/roles/manage-roles/manage-roles.component";
import {ManageRolesListComponent} from "@app/modules/security/roles/manage-roles-list/manage-roles-list.component";
import {ManageRolesEditComponent} from "@app/modules/security/roles/manage-roles-edit/manage-roles-edit.component";


/**
 * You can use Guard (RoutingGuardService) for permission checking. The service needs data with one parameter 'perm',
 * that gives the path of the uiPermission map of the user service.
 */

const routes: Routes = [
    {
        path: '', component: ManageRolesComponent, canActivate: [Guard],
        data: {perm: 'menu.user.roles'},
        children: [
            {path: 'list', component: ManageRolesListComponent},
            {path: 'edit/:roleid', component: ManageRolesEditComponent},
            {path: 'edit', component: ManageRolesEditComponent},
            {path: '', redirectTo: 'list', pathMatch: 'full'}
        ]
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [],
    declarations: []
})
export class RoleRoutingModule {
}

