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

import {AfterContentInit, Component, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {RoleService} from "@app/services/role.service";
import {UserService} from "@app/services/user.service";
import {FormBuilder} from "@angular/forms";
import {ToastService} from "@app/services/toast.service";
import {EditBaseComponent} from "@app/modules/shared/edit-base.component";
import {SecurityConfiguration} from "@app/model/security-configuration";
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import {SecurityService} from "@app/services/security.service";
import {switchMap, tap, withLatestFrom} from 'rxjs/operators';
import {BeanInformation} from "@app/model/bean-information";
import {zip} from "rxjs";
import {ErrorResult} from "@app/model/error-result";

@Component({
  selector: 'app-base-security',
  templateUrl: './base-security.component.html',
  styleUrls: ['./base-security.component.scss']
})
export class BaseSecurityComponent extends EditBaseComponent<SecurityConfiguration> implements OnInit, AfterContentInit  {

  activeUserManagers : BeanInformation[] = [];
  availableUserManagers: BeanInformation[] = [];
  activeRbacManagers: BeanInformation[] = [];
  availableRbacManagers: BeanInformation[] = [];

  submitting: boolean = false;

  rbacInfo: Map<string,BeanInformation> = new Map<string, BeanInformation>();
  userInfo: Map<string,BeanInformation> = new Map<string, BeanInformation>();

  constructor(private route: ActivatedRoute,
              public fb: FormBuilder, private securityService: SecurityService, private toastService: ToastService) {
    super(fb);
    super.init(fb.group({
      user_cache_enabled:[''],
    }, {}));



  }

  drop(event: CdkDragDrop<string[]>) {
    console.log("Drop " + event);
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      transferArrayItem(event.previousContainer.data,
          event.container.data,
          event.previousIndex,
          event.currentIndex);
    }
  }



  ngOnInit(): void {
    zip(this.securityService.getRbacManagers(),this.securityService.getUserManagers()).pipe(tap(([rbacInfo,userInfo])=>{
      rbacInfo.forEach(info => this.rbacInfo.set(info.id, info));
      userInfo.forEach(info => this.userInfo.set(info.id, info));
      console.log("Rbac info " + JSON.stringify(this.rbacInfo));
      console.log("User info " + JSON.stringify(this.userInfo));
    }),switchMap(()=>this.securityService.getConfiguration())).subscribe(userConfig=>{
      this.activeRbacManagers=[];
      this.activeUserManagers=[];
      this.availableRbacManagers=[];
      this.availableUserManagers=[];
      this.userForm.get('user_cache_enabled').setValue(userConfig.user_cache_enabled);
      let availableRbacManagers : string[] = Array.from(this.rbacInfo.keys())
      let availableUserManagers : string[] = Array.from(this.userInfo.keys());
      userConfig.active_rbac_managers.forEach(rbacId=>{
        this.activeRbacManagers.push(this.rbacInfo.get(rbacId));
        availableRbacManagers = availableRbacManagers.filter(item => item != rbacId);
      });
      userConfig.active_user_managers.forEach(userId=>{
        this.activeUserManagers.push(this.userInfo.get(userId));
        availableUserManagers = availableUserManagers.filter(item => item != userId);
      });
      availableRbacManagers.forEach(rbacId => this.availableRbacManagers.push(this.rbacInfo.get(rbacId)));
      availableUserManagers.forEach(userId => this.availableUserManagers.push(this.userInfo.get(userId)));
    })
  }

  createEntity(): SecurityConfiguration {
    return undefined;
  }

  onSubmit() {
    if (this.activeUserManagers.length>0 && this.activeRbacManagers.length>0) {
      this.submitting=true;
      let sConfig = new SecurityConfiguration()
      sConfig.active_user_managers = [];
      sConfig.active_user_managers = this.activeUserManagers.map(uInfo => uInfo.id);
      sConfig.active_rbac_managers = this.activeRbacManagers.map(rInfo => rInfo.id);
      sConfig.user_cache_enabled = this.userForm.get('user_cache_enabled').value;
      this.securityService.updateConfiguration(sConfig).subscribe(()=>
          {
            this.toastService.showSuccessByKey('base-security','security.config.base.submit_success')
            this.submitting = false;
          }, ( error :ErrorResult) => {
            this.toastService.showErrorByKey('base-security','security.config.base.submit_error',
                {'error':error.toString()})
            this.submitting = false;
          }
      );
    } else {
      this.toastService.showErrorByKey('base-security','security.config.base.submit_active_empty')
    }

  }

  ngAfterContentInit(): void {
  }

  enableSubmit() : boolean {
    return !this.submitting && this.activeUserManagers.length>0 && this.activeRbacManagers.length>0 && this.userForm.valid;
  }

}
