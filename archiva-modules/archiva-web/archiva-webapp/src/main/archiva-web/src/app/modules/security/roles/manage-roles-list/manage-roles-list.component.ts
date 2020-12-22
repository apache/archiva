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

import {Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {Role} from "@app/model/role";
import {Observable} from "rxjs";
import {PagedResult} from "@app/model/paged-result";
import {RoleService} from "@app/services/role.service";
import {SortedTableComponent} from "@app/modules/shared/sorted-table-component";
import {delay} from "rxjs/operators";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'app-manage-roles-list',
  templateUrl: './manage-roles-list.component.html',
  styleUrls: ['./manage-roles-list.component.scss']
})
export class ManageRolesListComponent extends SortedTableComponent<Role> implements OnInit {

  @ViewChild('content') public spinnerTemplate: TemplateRef<any>;


  constructor(translator: TranslateService, roleService : RoleService, private ngbModal:NgbModal) {
    super(translator, function (searchTerm: string, offset: number, limit: number, orderBy: string[], order: string): Observable<PagedResult<Role>> {
      // console.log("Retrieving data " + searchTerm + "," + offset + "," + limit + "," + orderBy + "," + order);
      return roleService.query(searchTerm, offset, limit, orderBy, order);
    });
  }

  ngOnInit(): void {
  }




}
