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

import {Injectable, OnInit} from '@angular/core';
import {UserService} from "./user.service";
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router} from "@angular/router";
import {AuthenticationService} from "./authentication.service";
import {first, timeout, tap, map, take} from "rxjs/operators";
import { Observable } from 'rxjs';
import { UserInfo } from '../model/user-info';

/**
 * Guard for the routes, that checks permissions by querying the uiPermission map of the UserService.
 * The guard checks the data in the routing definition for a 'perm' entry and uses this as path for the
 * uiPermission map.
 */
@Injectable({
  providedIn: 'root'
})
export class RoutingGuardService implements CanActivate, OnInit {

  constructor(private userService:UserService, public router: Router, private authService: AuthenticationService) {

  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const permString = route.data.perm;
    if (permString==null || permString=='') {
      console.error("Guard active, but permissions not set for route " + state.url);
      return false;
    }
    if (this.authService.authenticating) {
      console.debug("Guard: Authentication service is in authentication process");
      return this.authService.LoginEvent.pipe(take(1),timeout(1000), map(()=>{
        const myPerm = this.getPermission(permString);
        if (!myPerm) {
          this.router.navigate(['']);
        }
        return myPerm;
      }));
    }
    let perm = this.getPermission(permString);
    console.debug("Permission for " + state.url + ": " + perm);
    if (!perm) {
      this.router.navigate(['']);
    }
    return perm;
  }

  private getPermission(permString: string) {
    let perm = this.userService.uiPermissions;
    for (let permPath of permString.split(/\./)) {
      perm = perm[permPath];
      if (perm==null) {
        perm=false;
        break;
      }
    }
    return perm;
  }


  ngOnInit(): void {
  }
}

