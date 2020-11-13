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

import { Injectable } from '@angular/core';
import {UserService} from "./user.service";
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router} from "@angular/router";

/**
 * Guard for the routes, that checks permissions by querying the uiPermission map of the UserService.
 * The guard checks the data in the routing definition for a 'perm' entry and uses this as path for the
 * uiPermission map.
 */
@Injectable({
  providedIn: 'root'
})
export class RoutingGuardService implements CanActivate {

  constructor(private userService:UserService, public router: Router) {

  }

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    const permString = route.data.perm;
    if (permString==null || permString=='') {
      console.error("Guard active, but permissions not set for route " + state.url);
      return false;
    }
    let perm = this.userService.uiPermissions;
    for (let permPath of permString.split(/\./)) {
      perm = perm[permPath];
      if (perm==null) {
        perm=false;
        break;
      }
    }
    console.debug("Permission for " + state.url + ": " + perm);
    if (!perm) {
      this.router.navigate(['']);
    }
    return perm;
  }
}
