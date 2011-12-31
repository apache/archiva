/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
$(function() {

  role = function(name,description,assignable,childRoleNames,parentRoleNames,users,parentsRolesUsers,permissions){
    this.name = ko.observable(name);
    this.description = ko.observable(description);
    this.assignable = ko.observable(assignable);
    this.childRoleNames = ko.observableArray(childRoleNames);//read only
    this.parentRoleNames = ko.observableArray(parentRoleNames);//read only
    this.users = ko.observableArray(users);
    this.parentsRolesUsers = ko.observableArray(parentsRolesUsers);//read only
    this.permissions = ko.observableArray(permissions);//read only






    this.updateDescription=function(){
      var url = "restServices/redbackServices/roleManagementService/updateRoleDescription?";
      var roleName = this.name();
      url += "roleName="+encodeURIComponent(roleName);
      url += "&roleDescription="+encodeURIComponent(this.description());
      $.ajax(url,
        {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            $.log("role description updated");
            displaySuccessMessage($.i18n.prop("role.updated",roleName));
          },
          error: function(data){
            displayErrorMessage("error updating role description");
          }
        }
      );
    }
  }

  /**
   * view model used for roles grid
   */
  rolesViewModel=function() {
    this.roles = ko.observableArray([]);

    this.loadRoles = function() {
      $.ajax("restServices/redbackServices/roleManagementService/allRoles", {
          type: "GET",
          async: false,
          dataType: 'json',
          success: function(data) {
            var mappedRoles = $.map(data.role, function(item) {
              return mapRole(item);
            });
            self.roles(mappedRoles);
          }
        }
      );
    };

  }


  displayRolesGrid = function(){
    $("#user-messages").html("");
    $("#main-content").html("");
    $.ajax("restServices/redbackServices/roleManagementService/detailledAllRoles",
      {
       type: "GET",
       dataType: 'json',
       success: function(data) {
         var roles = $.map(data.role, function(item) {
           return mapRole(item);
         });
         //$.log(ko.toJSON(roles));
         $("#main-content").html($("#rolesTabs").tmpl());
         var data = {roles: roles};
         $("#main-content #roles-view-tabs-content #roles-view").html($("#rolesGrid").tmpl(data));
         $("#roles-view-tabs").tabs();
         activateRolesGridTab();
       }
      }
    );
  }

  editRole = function(roleName){
    $.log("edit role:"+roleName);
    $.ajax("restServices/redbackServices/roleManagementService/getRole/"+roleName,
      {
       type: "GET",
       dataType: 'json',
       success: function(data) {
         var role = mapRole(data.role);
         $("#main-content #roles-view-tabs-content #role-edit").html($("#editRoleTab").tmpl(data.role));
         activateRoleEditTab();
       }
      }
    );
  }

  saveRoleDescription=function(){
    var roleName = $("#editRoleTable #role-edit-name").html();
    var description = $("#editRoleTable #role-edit-description").val();
    clearUserMessages();
    new role(roleName,description).updateDescription();

  }

  /**
   * @param data Role response from redback rest api
   */
  mapRole=function(data) {
    // name, description, assignable,childRoleNames,parentRoleNames,users,parentsRolesUsers,permissions
    //$.log("mapRole:"+data.name+":");
    var childRoleNames = mapStringArray(data.childRoleNames);
    var parentRoleNames = mapStringArray(data.parentRoleNames);
    var users = data.users ? $.isArray(data.users) ? $.map(data.users, function(item) {
      return mapUser(item);
    }):[mapUser(data.users)]:null;

    var parentsRolesUsers = data.parentsRolesUsers ? $.isArray(data.parentsRolesUsers)? $.map(data.parentsRolesUsers, function(item) {
      return mapUser(item);
    }):[mapUser(data.parentsRolesUsers)]:null;

    var permissions = data.permissions? $.isArray(data.permissions) ? $.map(data.permissions, function(item){
      return mapPermission(item);
    }): [mapPermission(data.permissions)] :null;

    return new role(data.name, data.description,data.assignable,childRoleNames,parentRoleNames,users,parentsRolesUsers,permissions);
  }

  activateRolesGridTab=function(){
    $("#main-content #roles-view-tabs li").removeClass("active");
    $("#main-content #roles-view-tabs-content div").removeClass("active");
    // activate roles grid tab
    $("#main-content #roles-view-tabs-content #roles-view").addClass("active");
    $("#main-content #roles-view-tabs-li-roles-grid").addClass("active");
  }

  activateRoleEditTab=function(){
    $("#main-content #roles-view-tabs li").removeClass("active");
    $("#main-content #roles-view-tabs-content div").removeClass("active");
    // activate role edit tab
    $("#main-content #roles-view-tabs-content #role-edit").addClass("active");
    $("#roles-view-tabs-li-roles-edit").addClass("active");
  }

});