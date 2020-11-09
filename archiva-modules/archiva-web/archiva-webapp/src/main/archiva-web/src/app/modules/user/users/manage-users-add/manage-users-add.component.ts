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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators, FormBuilder} from '@angular/forms';
import {UserService} from "../../../../services/user.service";

@Component({
    selector: 'app-manage-users-add',
    templateUrl: './manage-users-add.component.html',
    styleUrls: ['./manage-users-add.component.scss']
})
export class ManageUsersAddComponent implements OnInit {

    userForm = this.fb.group({
        userId: ['', [Validators.required, Validators.minLength(8)]],
        fullName: ['', Validators.required],
        email: ['', [Validators.required,Validators.email]],
        locked: [false],
      passwordChangeRequired: [true]
    })

    constructor(private userService: UserService, private fb: FormBuilder) {

    }

    ngOnInit(): void {
    }

    onSubmit() {
        // Process checkout data here
        console.warn('Your order has been submitted', JSON.stringify(this.userForm.value));
    }

    get userId() {
      return this.userForm.get('userId');
    }

    valid(field:string) : string {
      let formField = this.userForm.get(field);
      if (formField.dirty||formField.touched) {
        if (formField.valid) {
          return 'is-valid'
        } else {
          return 'is-invalid'
        }
      } else {
        return ''
      }
    }

}
