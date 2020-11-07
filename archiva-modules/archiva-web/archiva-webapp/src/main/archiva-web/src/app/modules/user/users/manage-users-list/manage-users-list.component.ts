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

import { Component, OnInit, Input } from '@angular/core';
import {TranslateService} from "@ngx-translate/core";
import {UserService} from "../../../../services/user.service";
import {Observable, Subject, merge} from 'rxjs';
import { map, pluck, debounceTime, distinctUntilChanged, startWith, mergeMap} from "rxjs/operators";
import {UserInfo} from "../../../../model/user-info";


@Component({
  selector: 'app-manage-users-list',
  templateUrl: './manage-users-list.component.html',
  styleUrls: ['./manage-users-list.component.scss']
})
export class ManageUsersListComponent implements OnInit {
  @Input() heads: any;
  page = 1;
  pageSize = 10;
  total$: Observable<number>;
  items$: Observable<UserInfo[]>;
  searchTerm: string;
  private pageStream: Subject<number> = new Subject<number>();
  private searchTermStream: Subject<string> = new Subject<string>();

  constructor(private translator: TranslateService, private userService : UserService) { }

  ngOnInit(): void {
    this.heads = {};
    // We need to wait for the translator initialization and use the init key as step in.
    this.translator.get('init').subscribe(() => {
      // Only table headings for small columns that use icons
      for (let suffix of ['validated', 'locked', 'pwchange']) {
        this.heads[suffix] = this.translator.instant('users.list.table.head.' + suffix);
      }
    });
    const pageSource = this.pageStream.pipe(map(pageNumber => {
      return {search: this.searchTerm, page: pageNumber}
    }));
    const searchSource = this.searchTermStream.pipe(
        debounceTime(1000),
        distinctUntilChanged(),
        map(searchTerm => {
          this.searchTerm = searchTerm;
          console.log("Search term " + searchTerm);
          return {search: searchTerm, page: 1}
        }));
    const source = merge(pageSource, searchSource).pipe(
    startWith({search: this.searchTerm, page: this.page}),
        mergeMap((params: { search: string, page: number }) => {
          console.log("Executing user list " + params.search);
          return this.userService.getUserList(params.search, params.page*this.pageSize, this.pageSize)
        }));

    this.total$ = source.pipe(pluck('pagination.total'));
    this.items$ = source.pipe(pluck('data'));



    // const pageSource = map(pageNumber => {
    //   this.page = pageNumber
    //   return {search: this.searchTerm, page: pageNumber}
    // })
  }
  search(terms: string) {
    console.log("Keystroke " + terms);
    this.searchTermStream.next(terms)
  }

  changePage(pageNumber : number) {
    console.log("Page change " +typeof(pageNumber) +":" + JSON.stringify(pageNumber));
    this.pageStream.next(pageNumber);
  }

}
