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

import {AfterContentInit, Component, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {FormBuilder, Validators} from "@angular/forms";
import {RoleService} from "@app/services/role.service";
import {catchError, concatAll, debounceTime, distinctUntilChanged, filter, map, startWith, switchMap, tap} from "rxjs/operators";
import {Role} from '@app/model/role';
import {ErrorResult} from "@app/model/error-result";
import {EditBaseComponent} from "@app/modules/shared/edit-base.component";
import {EMPTY, forkJoin, fromEvent, Observable, of, zip} from 'rxjs';
import {RoleUpdate} from "@app/model/role-update";
import {EntityService} from "@app/model/entity-service";
import {User} from '@app/model/user';
import {PagedResult} from "@app/model/paged-result";
import {UserService} from "@app/services/user.service";
import {UserInfo} from '@app/model/user-info';
import {HttpResponse} from "@angular/common/http";
import {PaginatedEntitiesComponent} from "@app/modules/shared/paginated-entities/paginated-entities.component";
import {ToastService} from "@app/services/toast.service";
import {GroupMapping} from "@app/model/group-mapping";
import { Group } from '@app/model/group';
import { ElementRef } from '@angular/core';
import { GroupService } from '@app/services/group.service';

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
    roleUserService: EntityService<User>
    roleUserParentService: EntityService<User>;
    userSortField = ["id"];
    userSortOrder = "asc";
    userParentSortField = ["id"];
    userParentSortOrder = "asc";

    userSearching:boolean=false;
    userSearchFailed:boolean=false;
    public userSearchModel:any;

    groupSearching:boolean=false;
    groupSearchFailed:boolean=false;
    public groupSearchModel:any;

    @ViewChild('userSection') roleUserComponent: PaginatedEntitiesComponent<UserInfo>;
    @ViewChild('userParentSection') roleUserParentComponent: PaginatedEntitiesComponent<UserInfo>;

    @Output()
    roleIdEvent: EventEmitter<string> = new EventEmitter<string>(true);

    groupAddEvent: EventEmitter<any> = new EventEmitter<any>(true);

    private roleMappings$;

    constructor(private route: ActivatedRoute, public roleService: RoleService, private userService: UserService,
                private groupService: GroupService,
                public fb: FormBuilder, private toastService: ToastService) {
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
        this.route.queryParams.subscribe(
            params => {
                if (params.editmode) {
                    this.editMode=true;
                }
            }
        )
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
            const fRoleService = this.roleService;
            const roleId = role.id;
            this.roleUserService = function (searchTerm: string, offset: number, limit: number, orderBy: string[], order: string): Observable<PagedResult<User>> {
                return fRoleService.queryAssignedUsers(roleId, searchTerm, offset, limit, orderBy, order);
            };
            this.roleUserParentService = function (searchTerm: string, offset: number, limit: number, orderBy: string[], order: string): Observable<PagedResult<User>> {
                return fRoleService.queryAssignedParentUsers(roleId, searchTerm, offset, limit, orderBy, order, true);
            };
            if (this.roleUserComponent) {
                this.roleUserComponent.changeService(this.roleUserService);
            }
            if (this.roleUserParentComponent) {
                this.roleUserParentComponent.changeService(this.roleUserParentService);
            }
            this.roleMappings$ = this.getMappedGroups(role.id);
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
                    }),catchError((error : ErrorResult) => {
                        this.showError(error, "roles.edit.errors.retrieveFailed")
                        return of(this.createRole(id)); }));
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
        // console.log("Submitting changes " + role);
        this.roleService.updateRole(role).pipe(
            catchError((err: ErrorResult) => {
                this.error = true;
                this.success = false;
                this.errorResult = err;
                this.showError(err, 'roles.edit.errors.updateFailed',{'role_id':role.id})
                return [];
            })
        ).subscribe(roleInfo => {
            this.error = false;
            this.success = true;
            this.errorResult = null;
            this.result = roleInfo;
            this.showSuccess('roles.edit.success.updated',{'role_id':role.id})
            this.editMode = false;
        });

    }

    ngAfterContentInit(): void {
        // console.log("AfterContentInit")
        if (this.originRole) {
            this.editRole = this.originRole;
        }
    }

    searchUser = (text$: Observable<string>) =>
        text$.pipe(
            debounceTime(300),
            distinctUntilChanged(),
            tap(() => this.userSearching = true),
            switchMap(term =>
                this.roleService.queryUnAssignedUsers(this.editRole.id, term, 0, 10).pipe(
                    tap(() => this.userSearchFailed = false),
                    map(pagedResult=>
                    pagedResult.data),
                    catchError(() => {
                        this.userSearchFailed = true;
                        return of([]);
                    }))
            ),
            tap(() => this.userSearching = false)
        )


    getUserId(item:UserInfo) : string {
        return item.user_id;
    }

    searchGroup = (text$: Observable<string>) =>
        text$.pipe(
            debounceTime(300),
            distinctUntilChanged(),
            tap(() => this.groupSearching = true),
            switchMap(term =>
                this.groupService.query( term, 0, 10).pipe(
                    tap(() => this.groupSearchFailed = false),
                    map(( pagedResult : PagedResult<Group>) =>
                        pagedResult.data),
                    catchError(() => {
                        this.groupSearchFailed = true;
                        return of([]);
                    }))
            ),
            tap(() => this.groupSearching = false)
        )

    getGroupName(item:Group) : string {
        return item.name;
    }

    showError(err: ErrorResult, errorKey:string, params:any={}) : void {
        let message = err.error_messages.length>0?err.error_messages[0]:''
        params['message']=message
        this.toastService.showErrorByKey('manage-roles-edit',errorKey,params)
    }

    showSuccess(successKey:string, params:any={}) : void  {
        this.toastService.showSuccessByKey('manage-roles-edit',successKey,params)
    }

    assignUserRole() {
        let userId;
        if (typeof(this.userSearchModel)=='string') {
            userId=this.userSearchModel;
        } else {
            if (this.userSearchModel.user_id) {
                userId = this.userSearchModel.user_id;
            }
        }
        if (this.editRole.id!=null && userId!=null && userId.length>0) {
            this.roleService.assignRole(this.editRole.id, userId).pipe(
                catchError((err: ErrorResult) => {
                    this.error = true;
                    this.success = false;
                    this.errorResult = err;
                    this.showError(err, 'roles.edit.errors.userAssignFailed', {'role_id':this.editRole.id,'user_id':userId})
                    return [];
                })
            ).subscribe((response : HttpResponse<Role>)  => {
                this.error = false;
                this.success = true;
                this.errorResult = null;
                this.result = response.body;
                this.roleUserComponent.changePage(1);
                this.showSuccess('roles.edit.success.assign',{'role_id':this.editRole.id,'user_id':userId})
                this.userSearchModel=''
            });
        }
    }

    unassignUser(user_id:string) {
        // console.log("Unassigning " + this.editRole.id + " - " + user_id);
        if (this.editRole.id!=null && user_id!=null && user_id.length>0) {
            this.roleService.unAssignRole(this.editRole.id, user_id).pipe(
                catchError((err: ErrorResult) => {
                    this.error = true;
                    this.success = false;
                    this.errorResult = err;
                    this.showError(err, 'roles.edit.errors.unassignFailed',{'role_id':this.editRole.id,'user_id':user_id})
                    return [];
                })
            ).subscribe((response : HttpResponse<Role>)  => {
                    // console.log("Deleted ");
                    this.error = false;
                    this.success = true;
                    this.errorResult = null;
                    this.result = response.body;
                    this.roleUserComponent.changePage(1);
                    this.showSuccess('roles.edit.success.unassign',{'role_id':this.editRole.id,'user_id':user_id})
                }
            );
        }
    }

    assignGroupRole() {
        let groupName;
        if (typeof(this.groupSearchModel)=='string') {
            groupName=this.groupSearchModel;
        } else {
            if (this.groupSearchModel.name) {
                groupName = this.groupSearchModel.name;
            }
        }
        if (this.editRole.id!=null && groupName!=null && groupName.length>0) {
            this.groupService.assignGroup(groupName, this.editRole.id).pipe(
                catchError((err: ErrorResult) => {
                    this.error = true;
                    this.success = false;
                    this.errorResult = err;
                    this.showError(err, 'roles.edit.errors.groupAssignFailed', {'role_id':this.editRole.id,'group_name':groupName})
                    return [];
                })
            ).subscribe((response : HttpResponse<any>)  => {
                this.error = false;
                this.success = true;
                this.errorResult = null;
                this.result = response.body;
                this.showSuccess('roles.edit.success.assignGroup',{'role_id':this.editRole.id,'group_name':groupName})
                this.groupSearchModel=''
                this.groupAddEvent.emit(groupName);
            });
        }
    }


    getMappedGroups(roleId:string) : Observable<string[]> {
        console.log("Get mapped groups "+roleId);
        if (roleId!=null && roleId.length>0) {
            return this.groupAddEvent.pipe(
                startWith(0),
                switchMap(()=>
                    this.groupService.getGroupMappings()
                ),
                map((gMapArray: GroupMapping[]) => {
                    console.log("Array " + gMapArray + " - " + gMapArray.length);
                    let result = [];
                    for (let gMap of gMapArray) {
                        if (gMap.roles.includes(roleId)) {
                            result.push(gMap.group_name);
                        }
                    }
                    return result;
                })
            );
        } else {
            console.log("No role id found");
            return EMPTY;
        }
    }

}

