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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import {Component, OnInit, Input, OnDestroy} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {UserService} from "../../../../services/user.service";
import {UserInfo} from "../../../../model/user-info";
import {EntityService} from "../../../../model/entity-service";
import {Observable, of} from "rxjs";
import {PagedResult} from "../../../../model/paged-result";


@Component({
  selector: 'app-manage-users-list',
  templateUrl: './manage-users-list.component.html',
  styleUrls: ['./manage-users-list.component.scss']
})
export class ManageUsersListComponent implements OnInit {

  @Input() heads: any;
  service : EntityService<UserInfo>;


  constructor(private translator: TranslateService, private userService : UserService) {
    this.service = function (searchTerm: string, offset: number, limit: number, orderBy: string, order: string) : Observable<PagedResult<UserInfo>> {
      return userService.query(searchTerm, offset, limit, orderBy, order);
    }

  }

  ngOnInit(): void {
    this.heads = {};
    // We need to wait for the translator initialization and use the init key as step in.
    this.translator.get('init').subscribe(() => {
      // Only table headings for small columns that use icons
      for (let suffix of ['validated', 'locked', 'pwchange']) {
        this.heads[suffix] = this.translator.instant('users.list.table.head.' + suffix);
      }
    });



  }









}
