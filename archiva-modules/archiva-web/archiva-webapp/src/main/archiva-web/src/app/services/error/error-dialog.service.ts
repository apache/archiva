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

import {Injectable} from '@angular/core';
import {ErrorMessage} from "../../model/error-message";
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import {TranslateService} from "@ngx-translate/core";

@Injectable({
  providedIn: 'root'
})
export class ErrorDialogService {

  private errors= new Subject<ErrorMessage>();
  private opened = false;

  constructor(private router : Router, private translate : TranslateService) {}


  public addError(messageKey:string, args?:string[]) {
    let msg = new ErrorMessage();
    msg.error_key = messageKey;
    msg.args = args;
    if (msg.message==null||msg.message=='') {
      msg.message = this.translate.instant(msg.error_key, msg.args);
    }
    this.errors.next(msg);
  }


  public getErrors = () =>
      this.errors.asObservable();


  public showError() {

    this.router.navigate(['error'],  { queryParams:{'dialog':true} } );
  }
}
