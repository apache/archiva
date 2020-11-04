/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import {Component, OnDestroy, OnInit} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AuthenticationService } from "./services/authentication.service";
import {UserService} from "./services/user.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy{
  title = 'archiva-web';
  version = 'Angular version 10.0.2';

  constructor(
      public translate: TranslateService,
      public auth: AuthenticationService,
      public user: UserService
  ) {
    translate.addLangs(['en', 'de']);
    translate.setDefaultLang('en');
  }

  switchLang(lang: string) {
    this.translate.use(lang);
    this.user.userInfo.language = lang;
    this.user.persistUserInfo();
  }

  langIcon() : string {
    switch (this.translate.currentLang) {
      case "de":
        return "flag-icon-de";
      case "en":
        return "flag-icon-gb";
      default:
        return "flag-icon-" + this.translate.currentLang;
    }
  }

  ngOnDestroy(): void {
    this.auth.LoginEvent.unsubscribe();
  }


  ngOnInit(): void {
    if (this.user.userInfo!=null && this.user.userInfo.language!=null ) {
      this.translate.use(this.user.userInfo.language);
    } else {
      this.translate.use('en');
    }
    // Subscribe to login event in authenticator to switch the language
    this.auth.LoginEvent.subscribe(userInfo => {
      if (userInfo.language != null) {
        this.switchLang(userInfo.language);
      }
      // console.log("Permissions: " + JSON.stringify(this.user.permissions));
    })

  }
}
