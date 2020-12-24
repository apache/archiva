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

import { Component, OnInit } from '@angular/core';
import {ToastService} from "@app/services/toast.service";
import {TemplateRef} from "@angular/core";
import {AppNotification} from "@app/model/app-notification";

@Component({
  selector: 'app-toasts',
  template: `
    <ngb-toast
        *ngFor="let toast of toastService.toasts"
        [class]="toast.classname"
        [autohide]="autohide"
        [delay]="toast.delay || 5000"
        (hidden)="toastService.remove(toast); autohide=true;"
        (mouseenter)="autohide = false"
        (mouseleave)="autohide = true"
    >
      <i *ngIf="toast.type=='error'" class="fas fa-exclamation-triangle"></i>
      <ng-template [ngIf]="isTemplate(toast)" [ngIfElse]="text">
        <ng-template [ngTemplateOutlet]="toast.body" [ngTemplateOutletContext]="toast.contextData" ></ng-template>
      </ng-template>

      <ng-template #text>{{ toast.body }}</ng-template>
    </ngb-toast>
  `,
  styles: [".ngb-toasts{margin:.5em;padding:0.5em;position:fixed;right:2px;top:20px;z-index:1200}"
  ],
  host: {'[class.ngb-toasts]': 'true'}
})
export class ToastComponent implements OnInit {

  autohide:boolean=true;

  constructor(public toastService:ToastService) { }

  ngOnInit(): void {
  }

  isTemplate(toast:AppNotification) {
    console.log("Context data: "+JSON.stringify(toast.contextData))
    return toast.body instanceof TemplateRef; }

}
