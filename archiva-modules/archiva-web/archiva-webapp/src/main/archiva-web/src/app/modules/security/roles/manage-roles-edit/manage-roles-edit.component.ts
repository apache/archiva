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

import {AfterContentInit, Component, EventEmitter, OnInit, Output} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {FormBuilder, Validators} from "@angular/forms";
import {RoleService} from "@app/services/role.service";
import {catchError, filter, map, switchMap, tap} from "rxjs/operators";
import {Role} from '@app/model/role';
import {ErrorResult} from "@app/model/error-result";
import {EditBaseComponent} from "@app/modules/shared/edit-base.component";
import {forkJoin, iif, Observable, of, pipe, zip} from 'rxjs';
import {RoleUpdate} from "@app/model/role-update";

@Component({
    selector: 'app-manage-roles-edit',
    templateUrl: './manage-roles-edit.component.html',
    styleUrls: ['./manage-roles-edit.component.scss']
})
export class ManageRolesEditComponent extends EditBaseComponent<Role> implements OnInit, AfterContentInit {

    editRole: Role;
    editProperties = ['id', 'name', 'description', 'template_instance', 'resource', 'assignable'];
    originRole;
    roleCache: Map<string, Role> = new Map<string, Role>();


    @Output()
    roleIdEvent: EventEmitter<string> = new EventEmitter<string>(true);

    constructor(private route: ActivatedRoute, private roleService: RoleService, public fb: FormBuilder) {
        super(fb);
        super.init(fb.group({
            id: [''],
            name: ['', Validators.required],
            description: [''],
            resource: [''],
            template_instance: [''],
            assignable: ['']
        }, {}));
    }

    createEntity(): Role {
        return new Role();
    }

    ngOnInit(): void {
        this.route.params.pipe(
            map(params => params.roleid),
            filter(roleid => roleid != null),
            tap(roleid => {
                this.roleIdEvent.emit(roleid)
            }),
            switchMap((roleid: string) => this.roleService.getRole(roleid)),
            switchMap((role: Role) => zip(of(role),
                this.retrieveChildren(role),
                this.retrieveParents(role))),
            map((ra: [Role, Role[], Role[]]) => this.combine(ra))
        ).subscribe(role => {
            this.editRole = role;
            this.originRole = role;
            this.copyToForm(this.editProperties, this.editRole);
        }, error => {
            this.editRole = new Role();
        });
    }



    /**
     * Array of [role, children[], parents[]]
     */
    combine(roleArray: [Role, Role[], Role[]]): Role {
        roleArray[0].children = roleArray[1];
        roleArray[0].parents = roleArray[2];
        return roleArray[0];
    }

    private createRole(id: string): Role {
        let role = new Role();
        role.id = id;
        role.name=''
        return role;
    }

    getCachedRole(id : string) : Observable<Role> {
        return of(id).pipe(
            switchMap(( myId : string )  => {
                if (this.roleCache.has(myId)) {
                    return of(this.roleCache.get(myId));
                } else {
                    return this.roleService.getRole(myId).pipe(tap(role => {
                        this.roleCache.set(role.id, role);
                    }),catchError(() => of(this.createRole(id))));
                }
            }));
    }

    retrieveChildren(role: Role): Observable<Role[]> {
        // ForkJoin does not emit, if one of the observables is failing to emit a object
        // -> we use catchError()
        let children: Array<Observable<Role>> = []
        for (let child_id of role.child_role_ids) {
            children.push(this.getCachedRole(child_id));
        }
        if (children.length>0) {
            return forkJoin(children);
        } else {
            return of([]);
        }
    }

    retrieveParents(role: Role): Observable<Role[]> {
        let parents: Array<Observable<Role>> = []
        for (let parent_id of role.parent_role_ids) {
            parents.push(this.getCachedRole(parent_id));
        }
        if (parents.length>0) {
            return forkJoin(parents);
        } else {
            return of([]);
        }
    }

    onSubmit() {
        let role = new RoleUpdate();
        role.id=this.userForm.get('id').value;
        role.description = this.userForm.get('description').value;
        console.log("Submitting changes " + role);
        this.roleService.updateRole(role).pipe(
            catchError((err: ErrorResult) => {
                this.error = true;
                this.success = false;
                this.errorResult = err;
                return [];
            })
        ).subscribe(roleInfo => {
            this.error = false;
            this.success = true;
            this.errorResult = null;
            this.result = roleInfo;
            this.editMode = false;
        });

    }

    ngAfterContentInit(): void {
        if (this.originRole) {
            this.editRole = this.originRole;
        }
    }

}

