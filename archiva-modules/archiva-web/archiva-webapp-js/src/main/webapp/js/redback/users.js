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
      pageLinksId: "usersPagination",
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
      $("#main-content #user-edit").remove();
      $('#main-content #user-create').show();
      ko.renderTemplate("redback/user-edit-tmpl", new user(), null, $("#createUserForm").get(0),"replaceChildren");
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
    };

    this.lock = function(user){
      clearUserMessages();
      user.locked(true);
      window.redbackModel.createUser=false;
      user.save();
    }

    this.unlock = function(user){
      clearUserMessages();
      user.locked(false);
      window.redbackModel.createUser=false;
      user.save();
    }

    this.passwordChangeRequire = function(user,forceChangedPassword){
      clearUserMessages();
      user.passwordChangeRequired(forceChangedPassword);
      window.redbackModel.createUser=false;
      user.save();
    }

    this.sortByName = function() {
      this.users.sort(function(a, b) {
          return a.username < b.username ? -1 : 1;
      });
    };


    this.editUserBox=function(user) {
      window.redbackModel.createUser=false;
      clearUserMessages();
      //$("#main-content #user-edit").remove();
      $("#main-content").append("<div id='user-edit'></div>");
      //$("#main-content #user-edit").attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');
      $("#main-content #createUserForm").attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');

      //$("#main-content #user-create").remove();
      //$("#main-content #user-edit").show();

      var viewModel = new userViewModel(user);

      ko.applyBindings(viewModel,$("#main-content #createUserForm").get(0));

      activateUsersEditTab();

      $("#users-view-tabs-li-user-edit a").html($.i18n.prop("user.edit"));

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

    }

  }

  /**
   * called from the menu to display tabs with users grid
    */
  displayUsersGrid=function() {
    screenChange();
    jQuery("#main-content").attr("data-bind","");
    jQuery("#main-content").html($("#usersGrid").html());
    window.redbackModel.usersViewModel = new usersViewModel();
    window.redbackModel.usersViewModel.loadUsers();
    ko.applyBindings(window.redbackModel.usersViewModel,jQuery("#main-content").get(0));
    $("#users-view-tabs").tabs();
    $("#users-view-tabs").bind('change', function (e) {
      //$.log( $(e.target).attr("href") ); // activated tab
      //e.relatedTarget // previous tab
      if ($(e.target).attr("href")=="#createUserForm") {
        window.redbackModel.usersViewModel.addUser();
      }
      if ($(e.target).attr("href")=="#users-view") {
        $("#users-view-tabs-li-user-edit a").html($.i18n.prop("user.add"));
      }

    })
    $("#users-view-tabs-content #users-view").addClass("active");
  }



  userViewModel=function(user) {
      this.user=user;
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


