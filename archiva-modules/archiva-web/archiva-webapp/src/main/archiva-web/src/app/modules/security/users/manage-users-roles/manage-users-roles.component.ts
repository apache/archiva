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

import {AfterViewInit, Component, EventEmitter, OnInit, Output} from '@angular/core';
import {Role} from '@app/model/role';
import {UserService} from "@app/services/user.service";
import {ActivatedRoute} from "@angular/router";
import {catchError, filter, map, share, switchMap, tap} from "rxjs/operators";
import {RoleTree} from "@app/model/role-tree";
import {RoleService} from "@app/services/role.service";
import {RoleTemplate} from "@app/model/role-template";
import {from, Observable} from "rxjs";
import {Util} from "@app/modules/shared/shared.module";
import {RoleResult} from './role-result';
import {ErrorResult} from "@app/model/error-result";
import {HttpResponse} from "@angular/common/http";

@Component({
  selector: 'app-manage-users-roles',
  templateUrl: './manage-users-roles.component.html',
  styleUrls: ['./manage-users-roles.component.scss']
})
export class ManageUsersRolesComponent implements OnInit, AfterViewInit {

  roles$ : Observable<RoleResult>
  currentRoles: RoleResult
  guest: Role
  registered: Role
  templateRoles$: Observable<RoleTemplate[]>;
  userid: string;
  success:boolean=true;
  errors: ErrorResult[]=[];
  saved:boolean=false;

  @Output()
  userIdEvent: EventEmitter<string> = new EventEmitter<string>(true);

  constructor(private route : ActivatedRoute, private userService : UserService, private roleService : RoleService) {
  }

  ngOnInit(): void {
    this.roles$ = this.route.params.pipe(
        map(params => params.userid), filter(userid => userid != null),
        tap(userid => this.userid = userid),
        tap(userid=>{
          this.userIdEvent.emit(userid)
        }),
        switchMap(userid => {
          return this.userService.userRoleTree(userid)
        }),
        map(roleTree=>this.parseRoleTree(roleTree)),
        // This is to avoid multiple userService.userRoleTree() calls for template and base roles
        share()
    );
    this.templateRoles$ = this.roleService.getTemplates();
  }

  private parseRoleTree(roleTree:RoleTree): RoleResult {
      let roleResult = new RoleResult();
      let rootRoles = roleTree.root_roles;
      rootRoles.sort((a, b)=>{
        if (b.id=='guest') {
          return 1;
        } else if (b.id=='registered-user') {
          return 1;
        } else {
          return -1;
        }
      })
      for (let rootRole of rootRoles) {
        this.recurseTree(rootRole, roleResult, 0, null);
      }
      return roleResult;
  }

  private recurseTree(role:Role,roleResult:RoleResult, level:number, parent: Role) : void {
    let newLevel=level;
    if (parent!=null) {
      if (role.root_path==null) {
        role.root_path = (parent.root_path == null ? [] : parent.root_path.slice());
      }
      role.root_path.push(parent.id)
    }
    role.assigned_origin = role.assigned;
    if (role.template_instance) {
      newLevel = this.parseTemplateRole(role,roleResult.templateRoleInstances,newLevel)
    } else {
      newLevel = this.parseBaseRole(role, roleResult.baseRoles, newLevel)
    }
    for(let childRole of role.children) {
      let recurseChild = childRole;
      if (childRole.template_instance) {
        if (roleResult.templateRoleInstances.has(childRole.resource) && roleResult.templateRoleInstances.get(childRole.resource).has(childRole.model_id)) {
          recurseChild = roleResult.templateRoleInstances.get(childRole.resource).get(childRole.model_id)
        }
      } else {
        let existingBaseRole = roleResult.baseRoles.find(role => role.id == childRole.id)
        if (existingBaseRole != null) {
          recurseChild = existingBaseRole;
        }
      }
      this.recurseTree(recurseChild, roleResult, newLevel, role);
    }
  }

  private parseBaseRole(role:Role, roles : Array<Role>, level:number) : number {
    if (role.id=='guest') {
      this.guest = role;
    } else if (role.id=='registered-user') {
      this.registered = role;
    }
    role.enabled=true;
    let newLevel;
    if (role.assignable) {
      role.level=level
      roles.push(role);
      newLevel = level+1;
    } else {
      newLevel = level;
    }
    return newLevel;
  }

  private parseTemplateRole(role:Role, roles : Map<string, Map<string, Role>>, level:number) : number {
    let newLevel=level;
    role.enabled=true;
    if (role.assignable) {
      role.level=level
      let modelRoleMap = roles.get(role.resource)
      if (modelRoleMap==null) {
        modelRoleMap = new Map<string, Role>();
      }
      modelRoleMap.set(role.model_id, role);
      roles.set(role.resource, modelRoleMap);
      newLevel = level + 1;
    } else {
      newLevel = level;
    }
    return newLevel;
  }

  getRoleContent(role:Role) : string {
    let level = role.level
    let result = "";
    for(let _i=0; _i<level; _i++) {
      result = result + "&nbsp;<i class='fas fa-angle-double-right'></i>&nbsp;";
    }
    if (role.child) {
      return result + role.name;
    } else {
      return "<strong>"+result+role.name+"</strong>"
    }
  }

  changeBaseAssignment(role : Role, event) {
    let cLevel=-1
    let assignStatus
    // Guest is special and exclusive
    if (role.id==this.guest.id) {
      if (role.assigned) {
        this.currentRoles.baseRoles.forEach((cRole:Role)=> {
          if (cRole.id != this.guest.id) {
            cRole.assigned = false;
            cRole.enabled=true;
          }

        })
        role.enabled = false;
        this.currentRoles.templateRoleInstances.forEach((value, key)=>{
          value.forEach((templateInstance, modelId) => {
            templateInstance.assigned = false;
            templateInstance.enabled = true;

          });
        })
      }
    } else {
      this.guest.enabled = true;
      this.currentRoles.baseRoles.forEach((cRole)=> {
        if (cRole.id == role.id) {
          cLevel = cRole.level;
          assignStatus = cRole.assigned;
          if (assignStatus) {
            this.guest.assigned = false;
            this.guest.enabled = true;
          } else {
            if (!this.isAnyAssigned()) {
              this.guest.assigned=true;
              this.guest.enabled = false;
            }
          }
        } else {
          if (cLevel >= 0 && cLevel < cRole.level && cRole.root_path.find(pRoleId => pRoleId==role.id)) {
            if (assignStatus) {
              cRole.assigned = true;
              cRole.enabled = false;
            } else {
              cRole.enabled = true;
              cRole.assigned=cRole.assigned_origin
            }
          } else if (cLevel >= 0) {
            return;
          }
        }
      })
      this.currentRoles.templateRoleInstances.forEach((value, key)=>{
        value.forEach((templateInstance, modelId) => {
              if(templateInstance.root_path.find(roleId => roleId==role.id)) {
                if (role.assigned) {
                  templateInstance.assigned = true;
                  templateInstance.enabled = false
                } else {
                  templateInstance.enabled = true;
                  templateInstance.assigned=templateInstance.assigned_origin
                }
              }
            }
        )
      })
    }
  }

  isAnyAssigned() : boolean {
    if (Array.from(this.currentRoles.baseRoles.values()).find(role=>role.assigned)!=null) {
      return true;
    }
    return Array.from(this.currentRoles.templateRoleInstances.values()).map((roleMap: Map<string, Role>) => Array.from(roleMap.values()))
        .find(values=>values.find(role=>role.assigned))!=null
  }




  changeTemplateAssignment(role : Role, event) {
    if (role.assigned) {
      if (this.guest.assigned) {
        this.guest.assigned = false;
        this.guest.enabled = true;
      }
      if (!this.registered.assigned) {
        this.registered.assigned=true;
      }
    } else {
      if (!this.isAnyAssigned()) {
        this.guest.assigned=true;
        this.guest.enabled = false;
      }

    }
  }

  getInstanceContent(template:RoleTemplate, roles:Map<string,Role>) : Role {
      return roles.get(template.id)
  }

  saveAssignments() {
    this.saved=false;
    this.success=true;
    this.errors = [];
    let assignmentMap : Map<string, Role> = new Map(this.currentRoles.baseRoles.filter(role => role.assigned != role.assigned_origin).map(role => [role.id, role]));
    let assignments : Array<Role> =  []
    let unassignments : Array<Role> = []
    assignmentMap.forEach((role, roleId)=>{
      if (role.level>0) {
        for(let parentId of role.root_path) {
          if (assignmentMap.has(parentId) && assignmentMap.get(parentId).assigned) {
            return;
          }
        }
      }
      if (role.assigned) {
        assignments.push(role);
      } else {
        unassignments.push(role);
      }
    })
    this.currentRoles.templateRoleInstances.forEach((templMap, resource)=> {
          templMap.forEach((role, modelId)=> {
            if (role.assigned!=role.assigned_origin) {
              if (role.level>0) {
                for(let parentId of role.root_path) {
                  if (assignmentMap.has(parentId) && assignmentMap.get(parentId).assigned) {
                    return;
                  }
                }
              }
              if (role.assigned) {
                assignments.push(role);
              } else {
                unassignments.push(role);
              }
            }
          })
        }
    )
    from(assignments).pipe(switchMap((role) => this.roleService.assignRole(role.id, this.userid)),
        catchError((err: ErrorResult, caught) => {
              this.success = false;
              this.errors.push(err);
              return [];
            }
        )
    ).subscribe((result:HttpResponse<Role>)=> {
          this.updateRole(result.body, true);
          this.saved=true;
        }
    );
    from(unassignments).pipe(switchMap((role) => this.roleService.unAssignRole(role.id, this.userid)),
        catchError((err: ErrorResult, caught) => {
              this.success = false;
              this.errors.push(err);
              return [];
            }
        )
    ).subscribe(result=>{
      this.updateRole(result.body,false);
      this.saved=true;
    });
    this.saved=true;
  }

  private updateRole(role:Role, assignment:boolean) : void {
    if (role!=null) {
      if (role.template_instance) {
        this.currentRoles.templateRoleInstances.forEach((templMap, resource)=>{
          templMap.forEach((tmplRole, modelId)=> {
                if (tmplRole.id == role.id) {
                  Util.deepCopy(role, tmplRole, false);
                  tmplRole.assigned = assignment;
                }
              }
          )
        })
      } else {
        let target = this.currentRoles.baseRoles.find(baseRole => baseRole.id == role.id);
        Util.deepCopy(role, target, false);
        target.assigned = assignment;
      }
    }
  }

  ngAfterViewInit(): void {
    this.roles$.subscribe(roleResult => this.currentRoles = roleResult);
  }
}
