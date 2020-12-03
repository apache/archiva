import { Component, OnInit } from '@angular/core';
import { Role } from '@app/model/role';
import {UserService} from "@app/services/user.service";
import {ActivatedRoute} from "@angular/router";
import {filter, map, switchMap} from "rxjs/operators";
import {RoleTree} from "@app/model/role-tree";
import {RoleService} from "@app/services/role.service";
import {RoleTemplate} from "@app/model/role-template";
import {Observable} from "rxjs";
import {Util} from "@app/modules/shared/shared.module";

@Component({
  selector: 'app-manage-users-roles',
  templateUrl: './manage-users-roles.component.html',
  styleUrls: ['./manage-users-roles.component.scss']
})
export class ManageUsersRolesComponent implements OnInit {

  baseRoles : Array<Role>
  guest: Role
  registered: Role
  // Map of (resource, [roles])
  templateRoleInstances: Map<string, Array<Role>>
  templateRoles$: Observable<RoleTemplate[]>;

  constructor(private route : ActivatedRoute, private userService : UserService, private roleService : RoleService) {
    this.route.params.pipe(
        map(params => params.userid), filter(userid => userid!=null), switchMap(userid => userService.userRoleTree(userid))).subscribe(roleTree => {
      this.parseRoleTree(roleTree);
    });

  }

  ngOnInit(): void {
    this.templateRoles$ = this.roleService.getTemplates();
  }

  private parseRoleTree(roleTree:RoleTree): void {
    let roleTable = [];
    for(let rootRole of roleTree.root_roles) {
      roleTable = this.recurseRoleTree(rootRole, roleTable, 0);
    }
    this.baseRoles = roleTable;
    let templateMap : Map<string,Array<Role>> = new Map<string, Array<Role>>();
    for (let rootRole of roleTree.root_roles) {
      templateMap = this.recurseTemplates(rootRole, templateMap, 0);
    }
    this.templateRoleInstances = templateMap;
  }

  private recurseRoleTree(role:Role, roles : Array<Role>, level:number) : Array<Role> {
    if (role.id=='guest') {
      this.guest = role;
    } else if (role.id=='registered-user') {
      this.registered = role;
    }
    role.enabled=true;
    let newLevel;
    if (!role.template_instance && role.assignable) {
      role.level=level
      roles.push(role);
      newLevel = level+1;
    } else {
      newLevel = level;
    }
    for(let childRole of role.children) {
      roles = this.recurseRoleTree(childRole, roles, newLevel);
    }
    return roles;
  }

  private recurseTemplates(role:Role, roles : Map<string, Array<Role>>, level:number) : Map<string, Array<Role>> {
    role.enabled=true;
    if (role.template_instance && role.assignable) {
      role.level=level
      let roleList = roles.get(role.resource)
      if (roleList==null) {
        roleList = []
      }
      roleList.push(role);
      roles.set(role.resource, roleList);
    }
    for(let childRole of role.children) {
      roles = this.recurseTemplates(childRole, roles, level+1);
    }
    return roles;
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
    let assignStatus;
    if (role.id==this.guest.id) {
      if (role.assigned) {
        for (let cRole of this.baseRoles) {
          if (cRole.id != this.guest.id) {
            cRole.assigned = false;
            cRole.enabled=true;
          }
        }
        role.enabled = false;
      }
    } else {
      this.guest.enabled = true;
      for (let cRole of this.baseRoles) {
        if (cRole.id == role.id) {
          console.log("Value: " + cRole.assigned);
          cLevel = cRole.level;
          assignStatus = cRole.assigned;
          if (assignStatus) {
            this.guest.assigned = false;
          } else {
            if (!this.baseRoles.find(role=>role.assigned)) {
              this.guest.assigned=true;
            }
          }
        } else {
          console.log("Level " + cLevel);
          if (cLevel >= 0 && cLevel < cRole.level) {
            if (assignStatus) {
              cRole.assigned = true;
              cRole.enabled = false;
            } else {
              cRole.enabled = true;
            }
          } else if (cLevel >= 0) {
            break;
          }
        }
      }
    }
  }

  changeInstAssignment(role : Role, event) {
    console.log("Change " + role.id);
    console.log("Assignment changed "+JSON.stringify(event));
    console.log("Event target "+event.target);
  }

  getInstanceContent(template:RoleTemplate, roles:Array<Role>) : Role {
      return roles.find(role=>role.model_id==template.id)
  }
}
