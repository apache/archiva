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
import {EditBaseComponent} from "@app/modules/shared/edit-base.component";
import {CacheConfiguration} from "@app/model/cache-configuration";
import {ActivatedRoute} from "@angular/router";
import {FormBuilder} from "@angular/forms";
import {SecurityService} from "@app/services/security.service";
import {ToastService} from "@app/services/toast.service";
import {HttpResponse} from "@angular/common/http";
import {ErrorResult} from "@app/model/error-result";

@Component({
    selector: 'app-cache-setting',
    templateUrl: './cache-setting.component.html',
    styleUrls: ['./cache-setting.component.scss']
})
export class CacheSettingComponent extends EditBaseComponent<CacheConfiguration> implements OnInit {
    formFields = ['time_to_idle_seconds', 'time_to_live_seconds', 'max_entries_in_memory', 'max_entries_on_disk'];
    submitError: ErrorResult = null;

    constructor(private route: ActivatedRoute,
                public fb: FormBuilder, private securityService: SecurityService, private toastService: ToastService) {
        super(fb);
        super.init(fb.group({
            time_to_idle_seconds: [''],
            time_to_live_seconds: [''],
            max_entries_in_memory: [''],
            max_entries_on_disk: [''],
        }, {}));
    }

    ngOnInit(): void {
        this.securityService.getCacheConfiguration().subscribe(
            (cacheConfig: CacheConfiguration) => {
                this.copyToForm(this.formFields, cacheConfig);
            }
        )
    }

    createEntity(): CacheConfiguration {
        return new CacheConfiguration();
    }

    onSubmit() {
        if (this.userForm.valid && this.userForm.dirty) {
            let cacheConfig = this.copyFromForm(this.formFields)
            this.securityService.updateCacheConfiguration(cacheConfig).subscribe(
                (httpCacheConfig: HttpResponse<CacheConfiguration>) => {
                    this.userForm.reset();
                    if (httpCacheConfig.body!=null) {
                        this.copyToForm(this.formFields, httpCacheConfig.body);
                    }
                    this.toastService.showSuccessByKey('cache-settings','security.config.cache.submit_success');
                },
                (error: ErrorResult) => {
                    this.submitError=error;
                    this.toastService.showErrorByKey('cache-settings','security.config.cache.submit_error',{error:error.toString()});
                }
            );
        } else {
            console.log("No changes to update");
        }
    }

}
