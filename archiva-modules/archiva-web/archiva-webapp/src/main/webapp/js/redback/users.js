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
define("redback.users",["jquery","utils","i18n","jquery.validate","knockout","knockout.simpleGrid","jquery.ui","typeahead"],
function(jquery,utils,i18n,jqueryValidate,ko,koSimpleGrid,jqueryui,typeahead) {

  /**
   * view model used for users grid
   */
  UsersViewModel=function() {
    var self = this;
    this.users = ko.observableArray([]);
    this.originalUsers=ko.observableArray([]);

    this.gridViewModel = new ko.simpleGrid.viewModel({
      data: this.users,
      viewModel: this,
      columns: [
        {
          headerText: "User Name",
          rowText: "username",
          filter: true
        },
        {
          headerText: "Full Name",
          rowText: "fullName",
          filter: true
        },
        {
          headerText: "Email",
          rowText: "email",
          filter: true
        }
      ],
      pageSize: 10,
      gridUpdateCallBack: function(){
       $.log("gridUpdateCallBack users result");
        applyAutocompleteOnHeaders(self);
      }
    });
    clearFilters=function(){
      self.users(self.originalUsers());

    };
    filterLocked=function(){
      var founds=[];
      $(self.originalUsers()).each(function(idx,user){
        if(user.locked()){
          founds.push(user);
        }
      });
      self.users(founds);
    }
    filterNonLocked=function(){
      var founds=[];
      $(self.originalUsers()).each(function(idx,user){
        if(user.locked()==false){
          founds.push(user);
        }
      });
      self.users(founds);
    }

    filterPasswordChangeRequired=function(){
      var founds=[];
      $(self.originalUsers()).each(function(idx,user){
        if(user.passwordChangeRequired()){
          founds.push(user);
        }
      });
      self.users(founds);
    }
    filterPasswordChangeNotRequired=function(){
      var founds=[];
      $(self.originalUsers()).each(function(idx,user){
        if(user.passwordChangeRequired()==false){
          founds.push(user);
        }
      });
      self.users(founds);
    }


    this.addUser=function() {
      clearUserMessages();
      var mainContent = $("#main-content");
      mainContent.find("#createUserForm").empty();
      mainContent.find("#user-edit").remove();
      mainContent.find("#user-create").show();
      var viewModel = new UserViewModel(new User(),false,self);
      $.log("UsersViewModel#addUser");
      var createUserForm = mainContent.find("#createUserForm");
      createUserForm.html(smallSpinnerImg());
      createUserForm.attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');
      ko.applyBindings(viewModel,createUserForm.get(0));
      mainContent.find("#createUserForm #user-create-form-cancel-button").on( "click", function(e) {
        e.preventDefault();
        activateUsersGridTab();
      });
      mainContent.find("#user-create").validate({
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
      mainContent.find("#edit_user_details_pills_headers").hide();

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
                displaySuccessMessage( $.i18n.prop("user.deleted", currentUser.username()));
                self.users.remove(currentUser);
              },
              complete: function() {
                closeDialogConfirm();
              }
            }
          );
        }
        ,"Ok", $.i18n.prop("cancel"), $.i18n.prop("user.delete.message", currentUser.username()),
        $("#user-delete-warning-tmpl" ).tmpl(currentUser));

    }

    editUserBox=function(user) {
      $.log("editUserBox");
      clearUserMessages();
      activateUsersEditTab();
      var mainContent = $("#main-content");
      var viewModel = new UserViewModel(user,true,self);

      mainContent.find("#user-edit-roles-view" ).append(smallSpinnerImg());
      $.ajax("restServices/redbackServices/roleManagementService/getEffectivelyAssignedRoles/"+encodeURIComponent(user.username()), {
          type: "GET",
          dataType: 'json',
          success: function(data) {
            var mappedRoles = $.map(data, function(item) {
              return item.name;
            });
            user.assignedRoles = ko.observableArray(mappedRoles.sort());

            // user form binding
            var createUserForm = mainContent.find("#createUserForm");
            createUserForm.html(smallSpinnerImg());
            createUserForm.attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');
            ko.applyBindings(viewModel,createUserForm.get(0));

            mainContent.find("#users-view-tabs-li-user-edit a").html($.i18n.prop("edit"));

            mainContent.find("#user-create" ).find("#user-create-form-cancel-button").on("click", function(e) {
              e.preventDefault();
              activateUsersGridTab();
            });

            mainContent.find("#user-create").validate({
              rules: {
                confirmPassword: {
                  equalTo: "#password"
                }
              },
              showErrors: function(validator, errorMap, errorList) {
                customShowError("#main-content #user-create",validator,errorMap,errorMap);
              }
            });
            mainContent.find("#createUserForm #user-create #user-create-form-register-button").on("click", function(e) {
              e.preventDefault();
            });

            // user roles binding
            mainContent.find("#user-edit-roles-view").attr("data-bind",'template: {name:"user_view_roles_list_tmpl"}');
            ko.applyBindings(viewModel,mainContent.find("#user-edit-roles-view").get(0));
            mainContent.find("#edit_user_details_pills_headers a:first").tab('show');

            mainContent.find("#edit_user_details_pills_headers").bind('click', function (e) {
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
    var mainContent = $("#main-content");
    mainContent.find("#user-edit-roles-edit").html(smallSpinnerImg());
    $.ajax("restServices/redbackServices/roleManagementService/getApplicationRoles/"+encodeURIComponent(user.username()), {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          var mappedApplicationRoles = $.map(data, function(item) {
            return mapApplicationRoles(item);
          });
          viewModel.applicationRoles=ko.observableArray(mappedApplicationRoles);
          mainContent.find("#user-edit-roles-edit").attr("data-bind",'template: {name:"user_edit_roles_tmpl"}');
          ko.applyBindings(viewModel,mainContent.find("#user-edit-roles-edit").get(0));
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
      var valid = $("#main-content").find("#user-create").valid();
      if (valid==false) {
        return;
      }
      if (self.updateMode==false){
        return user.create(function(){self.usersViewModel.users.push(user)});
      } else {
        return user.update();
      }
    }

  }

  applyAutocompleteOnHeader=function(property,usersViewModel){

    $("#main-content").find("#users-grid-filter-auto-"+property ).autocomplete({
      minLength: 0,
      source: function(request, response){
        var founds=[];
        $.log("source:"+request.term+",users:"+usersViewModel.users().length)
        $(usersViewModel.users()).each(function(idx,user){
          if(user[property] && user[property]() && user[property]().indexOf(request.term)>=0){
            founds.push(user[property]());
          }
        });
        response(unifyArray(founds,true));
      },
      select: function( event, ui ) {
        $.log("property:"+property+','+ui.item.value);
        var users=[];
        $(usersViewModel.users()).each(function(idx,user){
          if(user[property] && user[property]() && user[property]().indexOf(ui.item.value)>=0){
            users.push(user);
          }
        });
        $.log("property:"+property+','+ui.item.value+",size:"+users.length);
        usersViewModel.users(users);
        return false;
      }
    });

    /*
    olamy: doesn't work????
    var values=[];
    $(usersViewModel.users()).each(function(idx,user){
      var value=user[property];
      if(value!=null && $.inArray(value, values)<0){
        values.push(value);
      }
    });
    var jid = "#users-grid-filter-auto-"+property;
    $.log("applyAutocompleteOnHeader:"+values.length+" for " + jid);

    var box = $("#main-content").find(jid);

    box.typeahead( { local: values } );
    box.bind('typeahead:selected', function(obj, datum, name) {
      var users=[];
      $(usersViewModel.users()).each(function(idx,user){
        if(user[property] && user[property]() && user[property]().indexOf(datum.value)>=0){
          users.push(user);
        }
      });
      usersViewModel.users(users);
    });*/
  }

  /**
   * called from the menu to display tabs with users grid
    */
  displayUsersGrid=function() {
    screenChange();
    var mainContent = $("#main-content");
    mainContent.html(mediumSpinnerImg());


    $.ajax("restServices/redbackServices/userService/getUsers", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          var mappedUsers = $.map(data, function(item) {
            return mapUser(item);
          });
          var usersViewModel = new UsersViewModel();
          usersViewModel.users(mappedUsers);
          usersViewModel.originalUsers(mappedUsers);
          mainContent.attr("data-bind",'template: {name:"usersGrid"}');
          $.log("before applyBindings");
          ko.applyBindings(usersViewModel,mainContent.get(0));//mainContent.find("#usersTable" ).get(0));//
          $.log("first applyBindings");
          mainContent.find("#users-view-tabs a:first").tab('show');
          mainContent.find("#users-view-tabs a[data-toggle='tab']").on('show', function (e) {
            if ($(e.target).attr("href")=="#createUserForm") {
              usersViewModel.addUser();
            }
            if ($(e.target).attr("href")=="#users-view") {
              mainContent.find("#users-view-tabs-li-user-edit a").html($.i18n.prop("add"));
            }
          })
          mainContent.find("#users-view-tabs-content #users-view").addClass("active");
          mainContent.find("#usersTable").find('.dropdown-toggle').dropdown();
        }
      }
    );

  }

  applyAutocompleteOnHeaders=function(usersViewModel){
    applyAutocompleteOnHeader("username",usersViewModel);
    applyAutocompleteOnHeader("fullName",usersViewModel);
    applyAutocompleteOnHeader("email",usersViewModel);
  }

  activateUsersGridTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#users-view-tabs li").removeClass("active");
    mainContent.find("#users-view-tabs-content div").removeClass("active");
    // activate users grid tab
    mainContent.find("#users-view-tabs-content #users-view").addClass("active");
    mainContent.find("#users-view-tabs-li-users-grid").addClass("active");
    mainContent.find("#users-view-tabs-li-user-edit a").html($.i18n.prop("add"));
  }

  activateUsersEditTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#users-view-tabs li").removeClass("active");
    mainContent.find("#users-view-tabs-content div").removeClass("active");
    // activate users edit tab
    mainContent.find("#users-view-tabs-content #createUserForm").addClass("active");
    mainContent.find("#users-view-tabs-li-user-edit").addClass("active");
  }

});


