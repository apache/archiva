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
define("redback.roles",["jquery","utils","i18n","jquery.validate","knockout","knockout.simpleGrid"],
function(jquery,utils,i18n,jqueryValidate,ko,koSimpleGrid) {

  Role = function(name,description,assignable,childRoleNames,parentRoleNames,users,parentsRolesUsers,permissions,otherUsers){

    var self=this;

    this.name = ko.observable(name);
    this.name.subscribe(function(newValue){self.modified(true)});

    this.description = ko.observable(description);
    this.description.subscribe(function(newValue){self.modified(true)});

    this.assignable = ko.observable(assignable);
    this.assignable.subscribe(function(newValue){self.modified(true)});

    this.childRoleNames = ko.observableArray(childRoleNames);//read only
    this.childRoleNames.subscribe(function(newValue){self.modified(true)});

    this.parentRoleNames = ko.observableArray(parentRoleNames);//read only
    this.parentRoleNames.subscribe(function(newValue){self.modified(true)});

    this.users = ko.observableArray(users?users:new Array());
    this.users.subscribe(function(newValue){self.modified(true)});

    this.parentsRolesUsers = ko.observableArray(parentsRolesUsers);//read only
    this.parentsRolesUsers.subscribe(function(newValue){self.modified(true)});

    this.permissions = ko.observableArray(permissions);//read only
    this.permissions.subscribe(function(newValue){self.modified(true)});

    // when editing a role other users not assign to this role are populated
    this.otherUsers = ko.observableArray(otherUsers?otherUsers:new Array());
    this.otherUsers.subscribe(function(newValue){self.modified(true)});

    this.removedUsers= ko.observableArray(new Array());
    this.removedUsers.subscribe(function(newValue){self.modified(true)});

    this.modified=ko.observable(false);

    this.usersModified=ko.observable(false);

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

    this.updateUsers=function(){
      var url = "restServices/redbackServices/roleManagementService/updateRoleUsers";
      $.ajax(url,
        {
          type: "POST",
          dataType: 'json',
          contentType: 'application/json',
          data: ko.toJSON(self),
          success: function(data) {
            clearUserMessages();
            displaySuccessMessage($.i18n.prop("role.users.updated",self.name()));
            self.usersModified(false);
            self.modified(false);
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

    editRole=function(role){
      var mainContent = $("#main-content");
      mainContent.find("#roles-view-tabs-content #role-edit").html(mediumSpinnerImg());
      // load missing attributes
      $.ajax("restServices/redbackServices/roleManagementService/getRole/"+encodeURIComponent(role.name()),
        {
         type: "GET",
         dataType: 'json',
         success: function(data) {
           var mappedRole = mapRole(data);
           role.parentRoleNames(mappedRole.parentRoleNames());
           role.parentsRolesUsers(mappedRole.parentsRolesUsers());
           role.users(mappedRole.users());
           role.otherUsers(mappedRole.otherUsers());
           role.modified(false);
           var viewModel = new RoleViewModel(role);
           ko.applyBindings(viewModel,mainContent.find("#roles-view-tabs-content #role-edit").get(0));
           activateRoleEditTab();
           activateRoleUsersListTab();
         }
        }
      );
    }

    this.bulkSave=function(){
      $.log("bulkSave");
      return getModifiedRoles().length>0;
    }

    getModifiedRoles=function(){
      var prx = $.grep(self.roles(),
          function (role,i) {
            return role.modified()||role.usersModified();
          });
      return prx;
    }

    updateModifiedRoles=function(){
      var modifiedRoles = getModifiedRoles();
      $.log("modifiedRoles:"+modifiedRoles);
      openDialogConfirm(function(){
                          for(i=0;i<modifiedRoles.length;i++){
                            var modifiedRole=modifiedRoles[i];
                            if (modifiedRole.modified()){
                              modifiedRole.updateDescription();
                              modifiedRole.modified(false);
                            }
                            if (modifiedRole.usersModified()){
                              modifiedRole.updateUsers();
                              modifiedRole.usersModified(false);
                            }
                          }
                          closeDialogConfirm();
                        },
                        $.i18n.prop('ok'),
                        $.i18n.prop('cancel'),
                        $.i18n.prop('roles.bulk.save.confirm.title'),
                        $.i18n.prop('roles.bulk.save.confirm',modifiedRoles.length));


    }

    updateRole=function(modifiedRole){
      if (modifiedRole.modified()){
        modifiedRole.updateDescription();
        modifiedRole.modified(false);
      }
      if (modifiedRole.usersModified()){
        modifiedRole.updateUsers();
        modifiedRole.usersModified(false);
      }
    }

  }

  displayRolesGrid = function(){
    screenChange();
    var mainContent = $("#main-content");
    mainContent.html(mediumSpinnerImg());

    $.ajax("restServices/redbackServices/roleManagementService/allRoles", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          var mappedRoles = $.map(data, function(item) {
            return mapRole(item);
          });
          var rolesViewModel = new RolesViewModel();
          rolesViewModel.roles(mappedRoles);
          mainContent.html($("#rolesTabs").tmpl());
          ko.applyBindings(rolesViewModel,mainContent.find("#roles-view").get(0));
          mainContent.find("#roles-view-tabs #roles-view-tabs-a-roles-grid").tab("show");
          activateRolesGridTab();
          removeMediumSpinnerImg(mainContent);
        }
      }
    );


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
        role.usersModified(true);
      }
      selectedOtherUsers([]);
      $("#role-collapse" ).removeClass("in");
      $("#role-users-collapse" ).addClass("in");
      activateRoleUsersEditTab();
    }

    removeUser=function(){
      $.log("removeUser");
      var added = currentRole.users.removeAll(selectedUsers());
      for (var i = 0; i < added.length; i++) {
        currentRole.otherUsers.push(added[i]);
        currentRole.removedUsers.push(added[i]);
        role.usersModified(true);
      }
      selectedUsers([]);
      $("#role-collapse" ).removeClass("in");
      $("#role-users-collapse" ).addClass("in");
      activateRoleUsersEditTab();
    }

    saveRoleDescription=function(){
      currentRole.updateDescription();
    }
    saveUsers=function(){
      currentRole.updateUsers();
    }

    updateMode=function(){
      var mainContent = $("#main-content");
      mainContent.find("#role-list-users").hide();
      mainContent.find("#role-edit-users").show();
    }
    viewMode=function(){
      var mainContent = $("#main-content");
      mainContent.find("#role-edit-users").hide();
      mainContent.find("#role-list-users").show();
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
    var mainContent = $("#main-content");
    mainContent.find("#roles-view-tabs").find("li").removeClass("active");
    mainContent.find("#roles-view-tabs-content").find("div").removeClass("active");
    // activate roles grid tab
    mainContent.find("#roles-view-tabs-content").find("#roles-view").addClass("active");
    mainContent.find("#roles-view-tabs-li-roles-grid").addClass("active");
  }

  activateRoleEditTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#roles-view-tabs").find("li").removeClass("active");
    mainContent.find("#roles-view-tabs-content").find("div").removeClass("active");
    // activate role edit tab
    mainContent.find("#roles-view-tabs-content").find("#role-edit").addClass("active");
    $("#roles-view-tabs-li-roles-edit").addClass("active");
  }

  activateRoleUsersListTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#role-edit-users-li").removeClass("active");
    mainContent.find("#role-edit-users").removeClass("active");
    // activate roles grid tab
    mainContent.find("#role-view-users-li").addClass("active");
    mainContent.find("#role-view-users").addClass("active");
  }

  activateRoleUsersEditTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#role-view-users-li").removeClass("active");
    mainContent.find("#role-view-users").removeClass("active");
    // activate role edit tab
    mainContent.find("#role-edit-users").addClass("active");
    $("#role-edit-users-li").addClass("active");
  }

  ApplicationRoles = function(name,description,globalRoles,roleTemplates,resources){
    //private String name;
    this.name = ko.observable(name);
    //private String description;
    this.description = description? ko.observable(description):"";
    //private Collection<String> globalRoles;
    this.globalRoles = ko.observableArray(globalRoles);
    //private Collection<RoleTemplate> roleTemplates;
    this.roleTemplates = ko.observableArray(roleTemplates);
    //private Collection<String> resources;
    this.resources = ko.observableArray(resources);
  }

  mapApplicationRoles=function(data){
    var roleTemplates = data.roleTemplates ? $.isArray(data.roleTemplates) ? $.map(data.roleTemplates, function(item) {
      return mapRoleTemplate(item);
    }):new Array(mapRoleTemplate(data.roleTemplates)):null;

    return new ApplicationRoles(data.name,data.description,mapStringArray(data.globalRoles),roleTemplates,mapStringArray(data.resources));
  }

  RoleTemplate = function(id,namePrefix,delimiter,description,resource,roles){
    //private String id;
    this.id = ko.observable(id);
    //private String namePrefix;
    this.namePrefix = ko.observable(namePrefix);
    //private String delimiter = " - ";
    this.delimiter = ko.observable(delimiter);
    //private String description;
    this.description = description? ko.observable(description):"";
    //private String resource;
    this.resource = ko.observable(resource);
    //private List<String> roles;
    this.roles = ko.observableArray(roles);
  }

  mapRoleTemplate = function(data){
    return new RoleTemplate(data.id,data.namePrefix,data.delimiter,data.description,mapStringArray(data.roles));
  }

});