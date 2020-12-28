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
import {NgForm} from '@angular/forms';
import {UserService} from "@app/services/user.service";
import {ToastService} from "@app/services/toast.service";
import {ErrorResult} from "@app/model/error-result";

@Component({
  selector: 'app-user-info',
  templateUrl: './user-info.component.html',
  styleUrls: ['./user-info.component.scss']
})
export class UserInfoComponent implements OnInit {

  @ViewChild('passwordForm') passwordForm: NgForm;

  formData = {
    current_password:"",
    password: "",
    confirm_password: ""
  }


  constructor(public userService: UserService, private toastService: ToastService) { }

  ngOnInit(): void {
  }

  changePassword() {
    console.log("Submit Password ")
    if (!this.formData.current_password || this.formData.current_password.length==0) {
      this.passwordForm.controls['current_password'].setErrors({'required':'true'})
      this.passwordForm.controls['current_password'].markAsDirty()
    }
    if (!this.formData.password || this.formData.password.length==0) {
      this.passwordForm.controls['password'].setErrors({'required':'true'})
      this.passwordForm.controls['password'].markAsDirty()
    }
    if (this.passwordForm.valid) {
      this.userService.changeOwnPassword(this.formData.current_password, this.formData.password, this.formData.confirm_password).subscribe(
          val => {
              this.toastService.showSuccessByKey('user-info','users.edit.passwordChanged')
            this.formData.password=""
            this.formData.current_password=""
            this.formData.confirm_password=""
            this.passwordForm.reset()
          },
          ( error : ErrorResult)=>{
            console.log("Error " + error.error_messages[0].message);
            if (error.error_messages.length>0) {
              if (error.error_messages[0].error_key.startsWith('user.password.violation')) {
                this.toastService.showError('user-info', error.error_messages[0].message)
                this.passwordForm.controls['password'].setErrors({'invalidpassword':error.error_messages[0].message})
              }
            }
          }
      )
    } else {
      this.toastService.showErrorByKey('user-info','form.error.invaliddata')
    }

  }

  confirmIsValid() : boolean {
    return (this.formData.password && this.formData.password.length >0 &&
        this.formData.confirm_password && this.formData.confirm_password.length>0 && this.formData.password==this.formData.confirm_password )
  }

  confirmIsNotValid() : boolean {
    return (this.formData.password && this.formData.password.length>0 &&
        this.formData.confirm_password && this.formData.confirm_password.length>0 && this.formData.password!=this.formData.confirm_password )
  }

  valid(field:string) {
    if (this.passwordForm) {
      let ctrl = this.passwordForm.controls[field];
      if (ctrl && ( ctrl.dirty || ctrl.touched ) && ctrl.valid) {
        return 'is-valid'
      }
      if (ctrl && ( ctrl.dirty || ctrl.touched  ) && ctrl.invalid) {
        return 'is-invalid'
      }
    }
  }

}
