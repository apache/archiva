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

  /**
   * view model used for users grid
   */
  usersViewModel=function() {
    this.users = ko.observableArray([]);
    var self = this;

    this.loadUsers = function() {
      $.ajax("restServices/redbackServices/userService/getUsers", {
          type: "GET",
          async: false,
          dataType: 'json',
          success: function(data) {
            var mappedUsers = $.map(data.user, function(item) {
              return mapUser(item);
            });
            self.users(mappedUsers);
          }
        }
      );
    };
    this.gridViewModel = new ko.simpleGrid.viewModel({
      data: this.users,
      viewModel: this,
      columns: [
        {
          headerText: "User Name",
          rowText: "username"},
        {
          headerText: "Full Name",
          rowText: "fullName"},
        {
          headerText: "Email",
          rowText: "email"}
      ],
      pageSize: 5
    });

    this.addUser=function() {
      clearUserMessages();
      window.redbackModel.createUser=true;
      $("#createUserForm").html("");
      $("#main-content #user-edit").remove();
      $('#main-content #user-create').show();
      ko.renderTemplate("redback/user-edit-tmpl", new User(), null, $("#createUserForm").get(0),"replaceChildren");
      $("#main-content #createUserForm #user-create").delegate("#user-create-form-cancel-button", "click keydown", function(e) {
        e.preventDefault();
        activateUsersGridTab();
      });
      $("#user-create").validate({
        rules: {
          confirmPassword: {
            equalTo: "#password"
          }
        },
        showErrors: function(validator, errorMap, errorList) {
          customShowError(validator,errorMap,errorMap);
        }
      });
      $("#main-content #createUserForm #user-create").delegate("#user-create-form-register-button", "click keydown", function(e) {
        e.preventDefault();
      });

      // desactivate roles pill when adding user
      $("#edit_user_details_pills_headers").hide();

    };

    this.lock = function(user){
      clearUserMessages();
      user.lock();
    }

    this.unlock = function(user){
      clearUserMessages();
      user.unlock();
    }

    this.passwordChangeRequire = function(user,forceChangedPassword){
      clearUserMessages();
      user.changePasswordChangeRequired(forceChangedPassword);
    }

    this.sortByName = function() {
      this.users.sort(function(a, b) {
          return a.username < b.username ? -1 : 1;
      });
    };


    this.editUserBox=function(user) {
      window.redbackModel.createUser=false;
      clearUserMessages();
      activateUsersEditTab();
      $("#main-content #createUserForm").html(smallSpinnerImg());
      $("#main-content #createUserForm").attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');

      var viewModel = new UserViewModel(user);

      ko.applyBindings(viewModel,$("#main-content #createUserForm").get(0));

      $("#main-content #users-view-tabs-li-user-edit a").html($.i18n.prop("user.edit"));

      $("#main-content #user-create").delegate("#user-create-form-cancel-button", "click keydown", function(e) {
        e.preventDefault();
        activateUsersGridTab();
      });

      $("#main-content #user-create").validate({
        rules: {
          confirmPassword: {
            equalTo: "#password"
          }
        },
        showErrors: function(validator, errorMap, errorList) {
          customShowError(validator,errorMap,errorMap);
        }
      });
      $("#main-content #user-create").delegate("#user-create-form-save-button", "click keydown", function(e) {
        e.preventDefault();
        var valid = $("#user-create").valid();
        if (!valid) {
            return;
        }
        user.update();
      });

      $( "#main-content #user-edit-roles-view" ).append(smallSpinnerImg());
      $.ajax("restServices/redbackServices/roleManagementService/getEffectivelyAssignedRoles/"+encodeURIComponent(user.username()), {
          type: "GET",
          async: false,
          dataType: 'json',
          success: function(data) {
            var mappedRoles = $.map(data.role, function(item) {
              return item.name;
            });
            user.assignedRoles = ko.observableArray(mappedRoles);

            $("#main-content #user-edit-roles-view").attr("data-bind",'template: {name:"user_view_roles_list_tmpl"}');
            ko.applyBindings(viewModel,$("#user-edit-roles-view").get(0));
            $("#main-content #edit_user_details_pills_headers").pills();

            $("#main-content #edit_user_details_pills_headers").bind('change', function (e) {
              //$.log( $(e.target).attr("href") ); // activated tab
              //e.relatedTarget // previous tab
              if ($(e.target).attr("href")=="#user-edit-roles-edit") {
                editUserRoles(user);
              }
            })

          }
        }
      );

    }
  }

  editUserRoles=function(user){
    var viewModel = new UserViewModel(user);
    $("#user-edit-roles-edit").html(smallSpinnerImg());
    $.ajax("restServices/redbackServices/roleManagementService/getApplicationRoles/"+encodeURIComponent(user.username()), {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          var mappedApplicationRoles = $.map(data.applicationRole, function(item) {
            return mapApplicationRoles(item);
          });
          viewModel.applicationRoles=ko.observableArray(mappedApplicationRoles);
          $.log("applicationRoles length:"+mappedApplicationRoles.length);
          $("#main-content #user-edit-roles-edit").attr("data-bind",'template: {name:"user_edit_roles_tmpl"}');
          ko.applyBindings(viewModel,$("#main-content #user-edit-roles-edit").get(0));
          $.log("assignedRoles:"+user.assignedRoles().length);
        }
      }
    );
  }

  UserViewModel=function(user) {
    this.user=user;
    this.applicationRoles = ko.observableArray(new Array());

    updateUserRoles=function(){
      this.user.updateAssignedRoles();
    }

  }

  /**
   * called from the menu to display tabs with users grid
    */
  displayUsersGrid=function() {
    screenChange();
    $("#main-content").html(mediumSpinnerImg());
    jQuery("#main-content").attr("data-bind",'template: {name:"usersGrid"}');
    window.redbackModel.usersViewModel = new usersViewModel();
    window.redbackModel.usersViewModel.loadUsers();
    ko.applyBindings(window.redbackModel.usersViewModel,jQuery("#main-content").get(0));
    $("#main-content #users-view-tabs").tabs();
    $("#main-content #users-view-tabs").bind('change', function (e) {
      //$.log( $(e.target).attr("href") ); // activated tab
      //e.relatedTarget // previous tab
      if ($(e.target).attr("href")=="#createUserForm") {
        window.redbackModel.usersViewModel.addUser();
      }
      if ($(e.target).attr("href")=="#users-view") {
        $("#main-content #users-view-tabs-li-user-edit a").html($.i18n.prop("user.add"));
      }

    })
    $("#main-content #users-view-tabs-content #users-view").addClass("active");
  }

  activateUsersGridTab=function(){
    $("#main-content #users-view-tabs li").removeClass("active");
    $("#main-content #users-view-tabs-content div").removeClass("active");
    // activate users grid tab
    $("#main-content #users-view-tabs-content #users-view").addClass("active");
    $("#users-view-tabs-li-users-grid").addClass("active");
  }

  activateUsersEditTab=function(){
    $("#main-content #users-view-tabs li").removeClass("active");
    $("#main-content #users-view-tabs-content div").removeClass("active");
    // activate users edit tab
    $("#main-content #users-view-tabs-content #createUserForm").addClass("active");
    $("#users-view-tabs-li-user-edit").addClass("active");
  }

  /**
   * not used as we don't have the mapping in web.xml
   * but why to handle such urls which go directly to a view
   */
  $(document).ready(function() {
    // url ends with /users/list
    // and current has archiva-manage-users karma
    // so display users list
    var pathContent = window.location.pathname.split("/");
    var usersIndex = $.inArray("users", pathContent);
    if (usersIndex>=0 && pathContent[usersIndex+1]=="list") {
      if ($.inArray("archiva-manage-users",window.redbackModel.operatioNames)>=0){
        displayUsersGrid();
      }
    }

  });



});


