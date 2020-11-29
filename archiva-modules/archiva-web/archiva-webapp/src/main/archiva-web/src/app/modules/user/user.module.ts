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
import {ManageUsersComponent} from "./users/manage-users/manage-users.component";
import {ManageUsersListComponent} from "./users/manage-users-list/manage-users-list.component";
import {ManageUsersAddComponent} from "./users/manage-users-add/manage-users-add.component";
import {ManageUsersEditComponent} from "./users/manage-users-edit/manage-users-edit.component";
import {SharedModule} from "../shared/shared.module";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {ManageUsersDeleteComponent} from './users/manage-users-delete/manage-users-delete.component';
import {UserRoutingModule} from "./user-routing.module";
import {TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';


@NgModule({
    declarations: [
        ManageUsersComponent,
        ManageUsersListComponent,
        ManageUsersAddComponent,
        ManageUsersEditComponent,
        ManageUsersDeleteComponent
    ],
    exports: [
        ManageUsersComponent,
        ManageUsersListComponent,
        ManageUsersAddComponent,
        ManageUsersEditComponent
    ],
    imports: [
        CommonModule,
        SharedModule,
        FormsModule,
        ReactiveFormsModule,
        UserRoutingModule
    ]
})
export class UserModule {
}
