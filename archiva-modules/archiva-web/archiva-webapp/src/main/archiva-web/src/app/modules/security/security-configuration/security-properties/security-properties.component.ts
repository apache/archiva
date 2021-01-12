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

import {Component, OnInit} from '@angular/core';
import {SortedTableComponent} from "@app/modules/shared/sorted-table-component";
import {PropertyEntry} from '@app/model/property-entry';
import {TranslateService} from "@ngx-translate/core";
import {Observable} from "rxjs";
import {PagedResult} from "@app/model/paged-result";
import {SecurityService} from "@app/services/security.service";
import {ToastService} from "@app/services/toast.service";
import {ErrorResult} from "@app/model/error-result";

@Component({
    selector: 'app-security-properties',
    templateUrl: './security-properties.component.html',
    styleUrls: ['./security-properties.component.scss']
})
export class SecurityPropertiesComponent extends SortedTableComponent<PropertyEntry> implements OnInit {

    editProperty:string='';
    propertyValue:string='';
    originPropertyValue:string='';

    constructor(translator: TranslateService, private securityService: SecurityService, private toastService: ToastService) {
        super(translator, function (searchTerm: string, offset: number, limit: number, orderBy: string[], order: string): Observable<PagedResult<PropertyEntry>> {
            // console.log("Retrieving data " + searchTerm + "," + offset + "," + limit + "," + orderBy + "," + order);
            return securityService.queryProperties(searchTerm, offset, limit, orderBy, order);
        });
        super.sortField=['key']
    }

    ngOnInit(): void {
    }

    isEdit(key:string) : boolean {
        return this.editProperty == key;
    }

    updateProperty(key:string, value:string) {
        console.log("Updating "+key+"="+value)
        if (this.propertyValue!=this.originPropertyValue) {
            this.securityService.updateProperty(key, value).subscribe(
                () => {
                    this.toastService.showSuccessByKey('security-properties', 'security.config.properties.edit_success')
                },
                (error: ErrorResult) => {
                    this.toastService.showErrorByKey('security-properties', 'security.config.properties.edit_failure', {error: error.firstMessageString()})
                }
            );
        }
    }

    toggleEditProperty(propertyEntry:PropertyEntry) : void {
        if (this.editProperty==propertyEntry.key) {
            propertyEntry.value=this.propertyValue
            this.editProperty='';
            this.updateProperty(propertyEntry.key, this.propertyValue);
            this.propertyValue = '';
            this.originPropertyValue='';
        } else {
            this.editProperty = propertyEntry.key;
            this.propertyValue = propertyEntry.value;
            this.originPropertyValue = propertyEntry.value;
        }
    }
}
