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
  UsersViewModel=function() {
    this.users = ko.observableArray([]);
    var self = this;

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
      $("#createUserForm").html("");
      $("#main-content #user-edit").remove();
      $('#main-content #user-create').show();
      var viewModel = new UserViewModel(new User(),false,self);
      $.log("UsersViewModel#addUser");
      var createUserForm = $("#main-content #createUserForm");
      createUserForm.html(smallSpinnerImg());
      createUserForm.attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');
      ko.applyBindings(viewModel,createUserForm.get(0));
      $("#main-content #createUserForm #user-create-form-cancel-button").on( "click", function(e) {
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
          customShowError("#main-content #user-create",validator,errorMap,errorMap);
        }
      });

      // desactivate roles pill when adding user
      $("#edit_user_details_pills_headers").hide();

    };

    lock = function(user){
      clearUserMessages();
      user.lock();
    }

    unlock = function(user){
      clearUserMessages();
      user.unlock();
    }

    passwordChangeRequire = function(user,forceChangedPassword){
      clearUserMessages();
      user.changePasswordChangeRequired(forceChangedPassword);
    }

    this.sortByName = function() {
      this.users.sort(function(a, b) {
          return a.username < b.username ? -1 : 1;
      });
    };

    deleteUser=function(user){
      clearUserMessages();

      var currentUser = user;
      openDialogConfirm(function(){
        $.ajax("restServices/redbackServices/userService/deleteUser/"+encodeURIComponent(currentUser.username()), {
              type: "GET",
              dataType: 'json',
              success: function(data) {
                // FIXME i18n
                displaySuccessMessage("user " + currentUser.username() + " deleted");
                self.users.remove(currentUser);
              },
              error: function(result) {
               var obj = jQuery.parseJSON(result.responseText);
               displayRedbackError(obj);
              },
              complete: function() {
                closeDialogConfirm();
              }
            }
          );
        }
        ,"Ok", $.i18n.prop("cancel"), $.i18n.prop("user.delete.message") + ": " + currentUser.username());

    }

    editUserBox=function(user) {
      clearUserMessages();
      activateUsersEditTab();

      var viewModel = new UserViewModel(user,true,self);

      $( "#main-content #user-edit-roles-view" ).append(smallSpinnerImg());
      $.ajax("restServices/redbackServices/roleManagementService/getEffectivelyAssignedRoles/"+encodeURIComponent(user.username()), {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            var mappedRoles = $.map(data.role, function(item) {
              return item.name;
            });
            user.assignedRoles = ko.observableArray(mappedRoles);

            // user form binding
            var createUserForm = $("#main-content #createUserForm");
            createUserForm.html(smallSpinnerImg());
            createUserForm.attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');
            ko.applyBindings(viewModel,createUserForm.get(0));

            $("#main-content #users-view-tabs-li-user-edit a").html($.i18n.prop("edit"));

            $("#main-content #user-create #user-create-form-cancel-button").on("click", function(e) {
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
                customShowError("#main-content #user-create",validator,errorMap,errorMap);
              }
            });
            $("#main-content #createUserForm #user-create #user-create-form-register-button").on("click", function(e) {
              e.preventDefault();
            });

            // user roles binding
            $("#main-content #user-edit-roles-view").attr("data-bind",'template: {name:"user_view_roles_list_tmpl"}');
            ko.applyBindings(viewModel,$("#user-edit-roles-view").get(0));
            $("#main-content #edit_user_details_pills_headers a:first").tab('show');

            $("#main-content #edit_user_details_pills_headers").bind('change', function (e) {
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

  UserViewModel=function(user,updateMode,usersViewModel) {
    this.user=user;
    this.applicationRoles = ko.observableArray(new Array());
    this.usersViewModel=usersViewModel;
    this.updateMode=updateMode;
    var self=this;
    updateUserRoles=function(){
      this.user.updateAssignedRoles();
    }

    saveUser=function(){
      $.log("UserViewModel#saveUser");
      var valid = $("#main-content #user-create").valid();
      if (valid==false) {
        $.log("user#save valid:false");
        return;
      } else {
        $.log("user#save valid:true,update:"+self.updateMode);
      }
      if (self.updateMode==false){
        return user.create(function(){self.usersViewModel.users.push(user)});
      } else {
        return user.update();
      }
    }

  }

  /**
   * called from the menu to display tabs with users grid
    */
  displayUsersGrid=function() {
    screenChange();
    $("#main-content").html(mediumSpinnerImg());
    jQuery("#main-content").attr("data-bind",'template: {name:"usersGrid"}');

    $.ajax("restServices/redbackServices/userService/getUsers", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          var mappedUsers = $.map(data.user, function(item) {
            return mapUser(item);
          });
          var usersViewModel = new UsersViewModel();
          usersViewModel.users(mappedUsers);
          ko.applyBindings(usersViewModel,jQuery("#main-content").get(0));
          $("#main-content #users-view-tabs a:first").tab('show');
          $("#main-content #users-view-tabs a[data-toggle='tab']").on('show', function (e) {
            //$.log( $(e.target).attr("href") ); // activated tab
            //e.relatedTarget // previous tab
            $.log("tabs shown");
            if ($(e.target).attr("href")=="#createUserForm") {
              usersViewModel.addUser();
            }
            if ($(e.target).attr("href")=="#users-view") {
              $("#main-content #users-view-tabs-li-user-edit a").html($.i18n.prop("add"));
            }

          })
          $("#main-content #users-view-tabs-content #users-view").addClass("active");
        }
      }
    );

  }

  activateUsersGridTab=function(){

    $("#main-content #users-view-tabs li").removeClass("active");
    $("#main-content #users-view-tabs-content div").removeClass("active");
    // activate users grid tab
    $("#main-content #users-view-tabs-content #users-view").addClass("active");
    $("#users-view-tabs-li-users-grid").addClass("active");
    $("#main-content #users-view-tabs-li-user-edit a").html($.i18n.prop("add"));
  }

  activateUsersEditTab=function(){
    $("#main-content #users-view-tabs li").removeClass("active");
    $("#main-content #users-view-tabs-content div").removeClass("active");
    // activate users edit tab
    $("#main-content #users-view-tabs-content #createUserForm").addClass("active");
    $("#users-view-tabs-li-user-edit").addClass("active");
  }





});


