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
import {Component, OnInit, ViewChild} from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { AuthenticationService } from "../../../services/authentication.service";
import {AccessToken} from "../../../model/access-token";
import {ErrorMessage} from "../../../model/error-message";
import {Router} from "@angular/router";
import {ArchivaRequestService} from "../../../services/archiva-request.service";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  @ViewChild("closebutton") closebutton;

  loginForm;
  userid;
  password;
  errorMessages : string[];

  constructor(    private authenticationService: AuthenticationService,
                  private formBuilder: FormBuilder,
                  private router: Router,
                  private archivaRequest : ArchivaRequestService ) {
    this.loginForm = this.formBuilder.group({
      userid: '',
      password: '',
    });
    this.errorMessages = [];

  }

  ngOnInit(): void {
  }


  login(customerData) {
    this.errorMessages = [];
    let resultHandler = (result: string, err?: ErrorMessage[] ) =>  {
      if (result=="OK") {
        this.closebutton.nativeElement.click();
        this.router.navigate(["/"]);
      } else if (result=="ERROR") {
        if (err != null) {
          this.errorMessages = [];
          for (let msg of err) {
            console.log("Error "+msg.error_key);
            this.errorMessages.push(this.archivaRequest.translateError(msg));
          }
        }
      }
    }
    // Process checkout data here
    this.loginForm.reset();
    this.authenticationService.login(customerData.userid, customerData.password, resultHandler);
  }


  
}
