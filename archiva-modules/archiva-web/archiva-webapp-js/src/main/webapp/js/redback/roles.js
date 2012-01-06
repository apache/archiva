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

  Role = function(name,description,assignable,childRoleNames,parentRoleNames,users,parentsRolesUsers,permissions,otherUsers){
    this.name = ko.observable(name);
    this.description = ko.observable(description);
    this.assignable = ko.observable(assignable);
    this.childRoleNames = ko.observableArray(childRoleNames);//read only
    this.parentRoleNames = ko.observableArray(parentRoleNames);//read only
    this.users = ko.observableArray(users?users:new Array());
    this.parentsRolesUsers = ko.observableArray(parentsRolesUsers);//read only
    this.permissions = ko.observableArray(permissions);//read only
    // when editing a role other users not assign to this role are populated
    this.otherUsers = ko.observableArray(otherUsers?otherUsers:new Array());
    this.removedUsers= ko.observableArray(new Array());

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
            clearUserMessages();
            displaySuccessMessage($.i18n.prop("role.updated",roleName));
          },
          error: function(data){
            clearUserMessages();
            displayErrorMessage("error updating role description");
          }
        }
      );
    }
    var self=this;
    this.updateUsers=function(){
      var url = "restServices/redbackServices/roleManagementService/updateRoleUsers";
      $.ajax(url,
        {
          type: "POST",
          dataType: 'json',
          contentType: 'application/json',
          data: "{\"role\": " +  ko.toJSON(self)+"}",
          success: function(data) {
            clearUserMessages();
            displaySuccessMessage($.i18n.prop("role.users.updated",this.name));
          },
          error: function(data){
            clearUserMessages();
            displayErrorMessage("error updating users role");
          }
        }
      );
    }

  }

  /**
   * view model used for roles grid
   */
  RolesViewModel=function() {
    this.roles = ko.observableArray([]);

    var self = this;
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


    this.gridViewModel = new ko.simpleGrid.viewModel({
      data: this.roles,
      viewModel: this,
      columns: [
        {
          headerText: $.i18n.prop('name'),
          rowText: "name"
        },
        {
          headerText: $.i18n.prop('description'),
          rowText: "description"
        }
      ],
      pageSize: 10
    });

    this.editRole=function(role){
      $("#main-content #roles-view-tabs-content #role-edit").html(mediumSpinnerImg());
      // load missing attributes
      $.ajax("restServices/redbackServices/roleManagementService/getRole/"+encodeURIComponent(role.name()),
        {
         type: "GET",
         dataType: 'json',
         success: function(data) {
           var mappedRole = mapRole(data.role);
           $("#main-content #roles-view-tabs-content #role-edit").attr("data-bind",'template: {name:"editRoleTab",data: currentRole}');
           role.parentRoleNames=mappedRole.parentRoleNames;
           role.parentsRolesUsers=mappedRole.parentsRolesUsers;
           role.users=mappedRole.users;
           role.otherUsers=mappedRole.otherUsers;
           var viewModel = new RoleViewModel(role);
           ko.applyBindings(viewModel,$("#main-content #roles-view-tabs-content #role-edit").get(0));
           activateRoleEditTab();
           $("#role-edit-users-tabs").tabs();
           $("#role-edit-users-tabs-content #role-view-users").addClass("active");
         }
        }
      );
    }

  }



  displayRolesGrid = function(){
    screenChange();
    $("#main-content").html(mediumSpinnerImg());
    window.redbackModel.rolesViewModel = new RolesViewModel();
    window.redbackModel.rolesViewModel.loadRoles();
    $("#main-content").html($("#rolesTabs").tmpl());
    ko.applyBindings(window.redbackModel.rolesViewModel,jQuery("#main-content").get(0));
    $("#roles-view-tabs").pills();
    activateRolesGridTab();
    removeMediumSpinnerImg();
  }

  RoleViewModel=function(role){
    selectedOtherUsers= ko.observableArray();
    selectedUsers= ko.observableArray();
    currentRole=role;
    var self=this;
    addUser=function(){
      $.log("addUser");
      var removed = currentRole.otherUsers.removeAll(selectedOtherUsers());
      for (var i = 0; i < removed.length; i++) {
        $.log("add user:"+removed[i].username());
        currentRole.users.push(removed[i]);
      }
      selectedOtherUsers([]);
      activateRoleUsersEditTab();
    }

    removeUser=function(){
      var added = currentRole.users.removeAll(selectedUsers());
      for (var i = 0; i < added.length; i++) {
        currentRole.otherUsers.push(added[i]);
        currentRole.removedUsers.push(added[i]);
      }
      selectedUsers([]);
      activateRoleUsersEditTab()
    }
    saveRoleDescription=function(){
      $.log("saveRoleDescription:"+currentRole.description());
      currentRole.updateDescription();
    }
    saveUsers=function(){
      currentRole.updateUsers();
    }

    updateMode=function(){
      $("#main-content #role-list-users").hide();
      $("#main-content #role-edit-users").show();
    }
    viewMode=function(){
      $("#main-content #role-edit-users").hide();
      $("#main-content #role-list-users").show();
    }
  }

  /**
   * @param data Role response from redback rest api
   */
  mapRole=function(data) {
    // olamy this mapping has issues when fields are array or not
    //return ko.mapping.fromJS(data);
    // name, description, assignable,childRoleNames,parentRoleNames,users,parentsRolesUsers,permissions
    //$.log("mapRole:"+data.name+":");
    var childRoleNames = mapStringArray(data.childRoleNames);
    var parentRoleNames = mapStringArray(data.parentRoleNames);
    var users = data.users ? $.isArray(data.users) ? $.map(data.users, function(item) {
      return mapUser(item);
    }):new Array(mapUser(data.users)):null;

    var parentsRolesUsers = data.parentsRolesUsers ? $.isArray(data.parentsRolesUsers)? $.map(data.parentsRolesUsers, function(item) {
      return mapUser(item);
    }):new Array(mapUser(data.parentsRolesUsers)):null;

    var permissions = data.permissions? $.isArray(data.permissions) ? $.map(data.permissions, function(item){
      return mapPermission(item);
    }): new Array(mapPermission(data.permissions)) :null;

    var otherUsers = data.otherUsers ? $.isArray(data.otherUsers)? $.map(data.otherUsers, function(item) {
      return mapUser(item);
    }):new Array(mapUser(data.otherUsers)):null;

    return new Role(data.name, data.description?data.description:"",data.assignable,childRoleNames,parentRoleNames,users,parentsRolesUsers,permissions,otherUsers);
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

  activateRoleUsersListTab=function(){
    $("#main-content #role-edit-users-li").removeClass("active");
    $("#main-content #role-edit-users").removeClass("active");
    // activate roles grid tab
    $("#main-content #role-view-users-li").addClass("active");
    $("#main-content #role-view-users").addClass("active");
  }

  activateRoleUsersEditTab=function(){
    $("#main-content #role-view-users-li").removeClass("active");
    $("#main-content #role-view-users").removeClass("active");
    // activate role edit tab
    $("#main-content #role-edit-users").addClass("active");
    $("#role-edit-users-li").addClass("active");
  }


});