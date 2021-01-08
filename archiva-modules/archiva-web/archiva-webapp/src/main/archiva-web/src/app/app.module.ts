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
import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {HttpClient, HttpClientModule} from '@angular/common/http';
import {MESSAGE_FORMAT_CONFIG, TranslateMessageFormatCompiler} from 'ngx-translate-messageformat-compiler';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {HomeComponent} from './modules/shared/home/home.component';
import {ContactComponent} from './modules/shared/contact/contact.component';
import {AboutComponent} from './modules/shared/about/about.component';
import {NotFoundComponent} from './modules/shared/not-found/not-found.component';
import {SidemenuComponent} from './modules/shared/sidemenu/sidemenu.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {LoginComponent} from './modules/shared/login/login.component';
import {ViewPermissionDirective} from './directives/view-permission.directive';
import {NavSubgroupDirective} from './directives/nav-subgroup.directive';
import {SearchComponent} from './modules/repo/search/search.component';
import {BrowseComponent} from './modules/repo/browse/browse.component';
import {UploadComponent} from './modules/repo/upload/upload.component';
import {SecurityConfigurationComponent} from './modules/security/security-configuration/security-configuration.component';
import {CoreModule} from "./modules/core/core.module";
import {httpTranslateLoader, SharedModule} from "./modules/shared/shared.module";
import {TranslateCompiler, TranslateLoader, TranslateModule} from "@ngx-translate/core";


@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    ContactComponent,
    AboutComponent,
    NotFoundComponent,
    SidemenuComponent,
    LoginComponent,
    ViewPermissionDirective,
    NavSubgroupDirective,
    SearchComponent,
    BrowseComponent,
    UploadComponent,
  ],
  imports: [
    TranslateModule.forRoot({
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
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,

      CoreModule,
      SharedModule
  ],
  providers: [
    { provide: MESSAGE_FORMAT_CONFIG, useValue: { locales: ['en', 'de'] }}
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }

