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

import { Injectable, TemplateRef } from '@angular/core';
import {AppNotification} from "@app/model/app-notification";
import {not} from "rxjs/internal-compatibility";
import {TranslateService} from "@ngx-translate/core";

@Injectable({
  providedIn: 'root'
})
export class ToastService {

  maxNotifications:number=10
  maxHistory:number=100
  toasts:AppNotification[]=[]
  toastHistory:AppNotification[]=[]

  constructor(private translator: TranslateService) { }

  public show(origin:string, textOrTpl: string | TemplateRef<any>, options: any = {}): void {
    let notification = new AppNotification(origin, textOrTpl, "", options);
    this.toasts.push(notification);
    this.toastHistory.push(notification);
    if (this.toasts.length>this.maxNotifications) {
      this.toasts.splice(0, 1);
    }
    if (this.toastHistory.length>this.maxHistory) {
      this.toastHistory.splice(0, 1);
    }
    console.log("Notification " + notification);
  }

  public showStandard(origin:string,textOrTpl:string|TemplateRef<any>, options:any={}) : void {
    options.classname=['alert','alert-primary']
    if (!options.delay) {
      options.delay=8000
    }
    this.show(origin,textOrTpl,options)
  }

  public showStandardByKey(origin:string,translationKey:string, params:any={}, options:any={} ) : void {
    let message:string;
    if (params) {
      message = this.translator.instant(translationKey, params);
    } else {
      message = this.translator.instant(translationKey);
    }
    this.showStandard(origin, message, options);
  }

  public showError(origin:string,textOrTpl:string|TemplateRef<any>, options:any={}) : void {
    options.classname=['alert','alert-danger']
    options.type='error'
    if (!options.delay) {
      options.delay=10000
    }
    this.show(origin,textOrTpl,options)
  }

  public showErrorByKey(origin:string,translationKey:string, params:any={}, options:any={} ) : void {
    let message:string;
    if (params) {
      message = this.translator.instant(translationKey, params);
    } else {
      message = this.translator.instant(translationKey);
    }
    this.showError(origin, message, options);
  }

  public showSuccess(origin:string,textOrTpl:string|TemplateRef<any>, options:any={}) : void {
    options.classname=['alert','alert-info']
    options.type='success'
    if (!options.delay) {
      options.delay=6000
    }
    this.show(origin,textOrTpl,options)
  }

  public showSuccessByKey(origin:string,translationKey:string, params:any={}, options:any={} ) : void {
    let message:string;
    if (params) {
      message = this.translator.instant(translationKey, params);
    } else {
      message = this.translator.instant(translationKey);
    }
    this.showSuccess(origin, message, options);
  }


  remove(toast) {
    this.toasts = this.toasts.filter(t => t != toast);
  }
}
