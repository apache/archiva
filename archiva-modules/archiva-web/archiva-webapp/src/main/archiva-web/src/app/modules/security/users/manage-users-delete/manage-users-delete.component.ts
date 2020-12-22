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

import {AfterViewInit, Component, Input, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {UserService} from "../../../../services/user.service";

@Component({
  selector: 'app-manage-users-delete',
  templateUrl: './manage-users-delete.component.html',
  styleUrls: ['./manage-users-delete.component.scss']
})
export class ManageUsersDeleteComponent implements OnInit, AfterViewInit {

  @ViewChild('userdelete') askModal;

  user_id: string;

  constructor(private route: ActivatedRoute, private modal: NgbModal,
              private userService: UserService, private router : Router) {
    this.route.params.subscribe((params)=>{
      if (params.userid) {
        this.user_id = params.userid;
      }
    })
  }

  ngOnInit(): void {

  }

  private runModal() {
    if (this.user_id!=null && this.user_id!='') {
      let modalInstance = this.modal.open(this.askModal).result.then((result) => {
        // console.log("Result: " + result);
        let userId = this.user_id;
        if (result=='YES' && userId!=null && userId!='') {
          let deleted = this.userService.deleteUser(userId).subscribe();
          if (deleted) {
            this.router.navigate(['/security','users','list']);
          }
        } else if (result=='NO') {
          this.router.navigate(['/security','users','list']);
        }
      }, (reason) => {
        // console.log("Reason: " + reason);
      });
    }
  }

  ngAfterViewInit(): void {
    if (this.user_id!=null) {
      this.runModal();
    }
  }

}
