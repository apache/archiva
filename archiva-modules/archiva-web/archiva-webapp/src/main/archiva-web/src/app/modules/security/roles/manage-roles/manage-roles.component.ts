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
import {merge, Observable} from "rxjs";
import {map, tap} from "rxjs/operators";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-manage-roles',
  templateUrl: './manage-roles.component.html',
  styleUrls: ['./manage-roles.component.scss']
})
export class ManageRolesComponent implements OnInit {

  roleId$:Observable<string>

  constructor(private route : ActivatedRoute) { }

  ngOnInit(): void {
  }

  onChildActivate(componentReference) {
    // console.log("Activating "+componentReference+" - "+JSON.stringify(componentReference,getCircularReplacer()))
    if (componentReference.roleIdEvent!=null) {
      let componentEmit : Observable<string> = componentReference.roleIdEvent.pipe(
          map((userid: string) => this.getSubPath(userid)));
      if (this.roleId$!=null) {
        this.roleId$ = merge(this.roleId$, componentEmit)
      } else {
        this.roleId$ = componentEmit;
      }
    }
  }

  getSubPath(userid:string) {
    if (userid!=null && userid.length>0) {
      return '/' + userid;
    } else {
      return '';
    }
  }

}
