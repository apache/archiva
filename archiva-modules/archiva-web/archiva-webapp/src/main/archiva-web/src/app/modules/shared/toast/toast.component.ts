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

import {Component, OnInit, TemplateRef, ViewEncapsulation} from '@angular/core';
import {ToastService} from "@app/services/toast.service";
import {AppNotification} from "@app/model/app-notification";

@Component({
  selector: 'app-toasts',
  template: `
    <ngb-toast
        *ngFor="let toast of toastService.toasts"
        [ngClass]="toast.classname"
        [autohide]="autohide"
        [delay]="toast.delay || 5000"
        (hidden)="toastService.remove(toast); autohide=true;"
        (mouseenter)="autohide = false"
        (mouseleave)="autohide = true"
    >
      <ng-template ngbToastHeader  >
        <i class="fas fa-exclamation-triangle" *ngIf="toast.type=='error'" ></i>
        <i class="fas fa-thumbs-up" *ngIf="toast.type=='success'" ></i>
        <i class="fas fa-info" *ngIf="toast.type!='success'&&toast.type!='error'" ></i>
      </ng-template>
      <ng-template [ngIf]="isTemplate(toast)" [ngIfElse]="text">
        <ng-template [ngTemplateOutlet]="toast.body" [ngTemplateOutletContext]="toast.contextData" ></ng-template>
      </ng-template>

      <ng-template #text>{{ toast.body }}</ng-template>
    </ngb-toast>
  `,
  styleUrls:['./toast.component.scss'],
  // Needed for styling the components
  encapsulation: ViewEncapsulation.None
})
export class ToastComponent implements OnInit {

  autohide:boolean=true;

  constructor(public toastService:ToastService) { }

  ngOnInit(): void {
  }

  isTemplate(toast:AppNotification) {
    return toast.body instanceof TemplateRef; }

}
