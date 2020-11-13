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
import { ActivatedRoute } from '@angular/router';
import {UserService} from "../../../../services/user.service";
import {FormBuilder, FormControl} from "@angular/forms";
import {map, switchMap} from 'rxjs/operators';
import {ManageUsersBaseComponent} from "../manage-users-base.component";

@Component({
  selector: 'app-manage-users-edit',
  templateUrl: './manage-users-edit.component.html',
  styleUrls: ['./manage-users-edit.component.scss']
})
export class ManageUsersEditComponent extends ManageUsersBaseComponent implements OnInit {

  editProperties = ['user_id', 'full_name', 'email', 'locked', 'password_change_required',
    'password', 'confirm_password', 'validated'];
  editUser;
  originUser;
  editMode:boolean=false;
  minUserIdSize=0;

  constructor(private route: ActivatedRoute, public userService: UserService, public fb: FormBuilder) {
    super(userService, fb);
    this.editUser = this.route.params.pipe(map (params => params.userid ),  switchMap(userid => userService.getUser(userid))  ).subscribe(user => {
      this.editUser = user;
      this.originUser = user;
      this.copyToForm(this.editProperties, this.editUser);});
  }

  ngOnInit(): void {
    this.userForm.setControl('user_id', new FormControl());
  }

  valid(field: string): string[] {
    if (this.editMode) {
      let classArr  = super.valid(field);
      return classArr.concat('form-control')
    } else {
      return ['form-control-plaintext'];
    }
  }


  onSubmit() {
    this.copyFromForm(this.editProperties)

  }


}
