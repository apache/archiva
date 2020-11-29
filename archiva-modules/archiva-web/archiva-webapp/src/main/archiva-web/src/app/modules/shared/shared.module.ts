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
import { CommonModule } from '@angular/common';
import {PaginatedEntitiesComponent} from "./paginated-entities/paginated-entities.component";
import {SortedTableHeaderComponent} from "./sorted-table-header/sorted-table-header.component";
import {SortedTableHeaderRowComponent} from "./sorted-table-header-row/sorted-table-header-row.component";
import {NgbPaginationModule, NgbTooltipModule} from "@ng-bootstrap/ng-bootstrap";
import {TranslateCompiler, TranslateLoader, TranslateModule} from "@ngx-translate/core";
import {TranslateMessageFormatCompiler} from "ngx-translate-messageformat-compiler";
import {HttpClient} from "@angular/common/http";
import {TranslateHttpLoader} from "@ngx-translate/http-loader";
import {RouterModule} from "@angular/router";



@NgModule({
  declarations: [
    PaginatedEntitiesComponent,
    SortedTableHeaderComponent,
    SortedTableHeaderRowComponent
  ],
  exports: [
      CommonModule,
      RouterModule,
      TranslateModule,
      NgbPaginationModule,
      NgbTooltipModule,
      PaginatedEntitiesComponent,
      SortedTableHeaderComponent,
      SortedTableHeaderRowComponent
  ],
  imports: [
    CommonModule,
      RouterModule,
      NgbPaginationModule,
      NgbTooltipModule,
    TranslateModule.forChild({
      compiler: {
        provide: TranslateCompiler,
        useClass: TranslateMessageFormatCompiler
      },
      loader: {
        provide: TranslateLoader,
        useFactory: httpTranslateLoader,
        deps: [HttpClient]
      }
    }),
  ]
})
export class SharedModule { }
export function httpTranslateLoader(http: HttpClient) {
  return new TranslateHttpLoader(http);
}