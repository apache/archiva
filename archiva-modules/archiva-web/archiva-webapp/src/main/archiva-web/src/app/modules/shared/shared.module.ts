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
import {PaginatedEntitiesComponent} from "./paginated-entities/paginated-entities.component";
import {SortedTableHeaderComponent} from "./sorted-table-header/sorted-table-header.component";
import {SortedTableHeaderRowComponent} from "./sorted-table-header-row/sorted-table-header-row.component";
import {
    NgbAccordionModule,
    NgbModalModule,
    NgbPaginationModule,
    NgbTooltipModule,
    NgbTypeaheadModule,
    NgbToastModule
} from "@ng-bootstrap/ng-bootstrap";
import {TranslateCompiler, TranslateLoader, TranslateModule} from "@ngx-translate/core";
import {TranslateMessageFormatCompiler} from "ngx-translate-messageformat-compiler";
import {HttpClient} from "@angular/common/http";
import {TranslateHttpLoader} from "@ngx-translate/http-loader";
import {RouterModule} from "@angular/router";
import { WithLoadingPipe } from './with-loading.pipe';
import { StripLoadingPipe } from './strip-loading.pipe';
import { ToastComponent } from './toast/toast.component';
import { UserInfoComponent } from './user-info/user-info.component';
import {FormsModule} from "@angular/forms";
import {DragDropModule} from "@angular/cdk/drag-drop";

export { LoadingValue } from './model/loading-value';
export { PageQuery } from './model/page-query';

@NgModule({
    declarations: [
        PaginatedEntitiesComponent,
        SortedTableHeaderComponent,
        SortedTableHeaderRowComponent,
        WithLoadingPipe,
        StripLoadingPipe,
        ToastComponent,
        UserInfoComponent
    ],
    exports: [
        CommonModule,
        RouterModule,
        TranslateModule,
        NgbPaginationModule,
        NgbTooltipModule,
        NgbAccordionModule,
        NgbModalModule,
        NgbTypeaheadModule,
        NgbToastModule,
        PaginatedEntitiesComponent,
        SortedTableHeaderComponent,
        SortedTableHeaderRowComponent,
        WithLoadingPipe,
        StripLoadingPipe,
        ToastComponent,
        DragDropModule
    ],
    imports: [
        DragDropModule,
        CommonModule,
        RouterModule,
        NgbPaginationModule,
        NgbTooltipModule,
        NgbToastModule,
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
        FormsModule,
    ]
})
export class SharedModule {
}

export function httpTranslateLoader(http: HttpClient) {
    return new TranslateHttpLoader(http);
}

export const Util = {
    deepCopy(src: Object, dst: Object, overwriteWithEmptyString:boolean=true) {
        Object.keys(src).forEach((key, idx) => {
            let srcEl = src[key];
            if (typeof (srcEl) == 'object') {
                let dstEl;
                if (!dst.hasOwnProperty(key)) {
                    dst[key] = {}
                }
                dstEl = dst[key];
                this.deepCopy(srcEl, dstEl);
            } else if (typeof(srcEl)=='string') {
                if (overwriteWithEmptyString) {
                    dst[key]=srcEl
                } else {
                    if ((srcEl as string).length>0) {
                        dst[key]=srcEl
                    }
                }
            } else {
                // console.debug("setting " + key + " = " + srcEl);
                dst[key] = srcEl;
            }
        });
    }
}