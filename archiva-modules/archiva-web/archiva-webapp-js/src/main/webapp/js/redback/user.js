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
   * object model for user with some function to create/update/delete users
   * @param username
   * @param password
   * @param confirmPassword
   * @param fullName
   * @param email
   * @param permanent
   * @param validated
   * @param timestampAccountCreation
   * @param timestampLastLogin
   * @param timestampLastPasswordChange
   * @param locked
   * @param passwordChangeRequired
   * @param ownerViewModel
   */
  User=function(username, password, confirmPassword,fullName,email,permanent,validated,timestampAccountCreation,
                timestampLastLogin,timestampLastPasswordChange,locked,passwordChangeRequired,ownerViewModel) {
      // Potentially Editable Field.
      this.username = ko.observable(username);
      // Editable Fields.
      this.password = ko.observable(password);
      this.confirmPassword = ko.observable(confirmPassword);
      this.fullName = ko.observable(fullName);
      this.email = ko.observable(email);
      this.permanent = ko.observable(permanent);
      this.validated = ko.observable(validated);
      // Display Only Fields.
      this.timestampAccountCreation = ko.observable(timestampAccountCreation);
      this.timestampLastLogin = ko.observable(timestampLastLogin);
      this.timestampLastPasswordChange = ko.observable(timestampLastPasswordChange);
      // admin only
      this.locked = ko.observable(locked);
      this.passwordChangeRequired = ko.observable(passwordChangeRequired);
      this.assignedRoles = ko.observableArray(new Array());

      this.remove = function() {
        if (ownerViewModel) {
          ownerViewModel.users.destroy(this);
        }
      };
      this.create = function(successFnCallback) {
        if (username == 'admin') {
          this.createAdmin();
        } else {
          this.createUser(successFnCallback);
        }
      };
      this.createUser = function(successFnCallback) {
        $.log("user#createUser");
        var valid = $("#user-create").valid();
        if (!valid) {
            return;
        }
        var currentUser = this;
        $.ajax("restServices/redbackServices/userService/createUser", {
            data: "{\"user\": " +  ko.toJSON(this)+"}",
            contentType: 'application/json',
            type: "POST",
            dataType: 'json',
            success: function(result) {
              var created = JSON.parse(result);
              if (created == true) {
                displaySuccessMessage("user created:"+currentUser.username());
                if (successFnCallback){
                  successFnCallback(currentUser);
                }
                clearForm("#main-content #user-create");
                $("#main-content #user-create").hide();
                activateUsersGridTab();
                return this;
              } else {
                displayErrorMessage("user cannot created");
              }
            },
            error: function(result) {
              var obj = jQuery.parseJSON(result.responseText);
              displayRedbackError(obj);
            }
          });
      };

      this.createAdmin = function() {
        $.log("user.js#createAdmin");
        var valid = $("#user-create").valid();
        $.log("create admin");
        if (!valid) {
            return;
        }
        var currentAdminUser = this;
        $.ajax("restServices/redbackServices/userService/createAdminUser", {
            data: "{\"user\": " +  ko.toJSON(this)+"}",
            contentType: 'application/json',
            type: "POST",
            dataType: 'json',
            success: function(result) {
              var created = JSON.parse(result);
              if (created == true) {
                displaySuccessMessage("admin user created");
                var onSuccessCall=function(){
                  $.log("onSuccessCall after admin creation");
                  reccordLoginCookie(currentAdminUser);
                  screenChange();
                  checkCreateAdminLink();
                  checkSecurityLinks();
                }
                loginCall(currentAdminUser.username(), currentAdminUser.password(),onSuccessCall);
                return this;
              } else {
                displayErrorMessage("admin user not created");
              }
            },
            error: function(result) {
              var obj = jQuery.parseJSON(result.responseText);
              displayRedbackError(obj);
            }
          });
      };


      this.update=function(){
        var currentUser = this;
        $.ajax("restServices/redbackServices/userService/updateUser", {
            data: "{\"user\": " +  ko.toJSON(this)+"}",
            contentType: 'application/json',
            type: "POST",
            dataType: 'json',
            success: function(result) {
              var updated = JSON.parse(result);
              if (updated == true) {
                displaySuccessMessage($.i18n.prop("user.updated",currentUser.username()));
                $("#users-view-tabs-li-user-edit a").html($.i18n.prop("add"));
                clearForm("#main-content #user-create");
                activateUsersGridTab();
                return this;
              } else {
                displayErrorMessage("user cannot be updated");
              }
            },
            error: function(result) {
              var obj = jQuery.parseJSON(result.responseText);
              displayRedbackError(obj);
            }
          });
      }

      this.save=function(){
        $.log("user.save create:"+window.redbackModel.createUser);
        if (window.redbackModel.createUser==true){
          var valid = $("#main-content #user-create").valid();

          if (valid==false) {
            $.log("user#save valid:false");
            return;
          } else {
            $.log("user#save valid:true");
            return this.create();
          }
        } else {
          return this.update();
        }
      }

      this.updateAssignedRoles=function(){
        $.log("user#updateAssignedRoles");
        var curUser = this;
        clearUserMessages();
        $.ajax("restServices/redbackServices/roleManagementService/updateUserRoles", {
            data: "{\"user\": " +  ko.toJSON(this)+"}",
            contentType: 'application/json',
            type: "POST",
            dataType: 'json',
            success: function(result) {
              displaySuccessMessage($.i18n.prop("user.roles.updated",curUser.username()));
            },
            error: function(result) {
              var obj = jQuery.parseJSON(result.responseText);
              displayRedbackError(obj);
            }
          });
      }

      this.lock=function(){
        this.locked(true);
        var curUser = this;
        clearUserMessages();
        $.ajax("restServices/redbackServices/userService/lockUser/"+encodeURIComponent(curUser.username()), {
            type: "GET",
            success: function(result) {
              displaySuccessMessage($.i18n.prop("user.locked",curUser.username()));
            },
            error: function(result) {
              var obj = jQuery.parseJSON(result.responseText);
              displayRedbackError(obj);
            }
          });
      }

      this.unlock=function(){
        this.locked(false);
        var curUser = this;
        clearUserMessages();
        $.ajax("restServices/redbackServices/userService/unlockUser/"+encodeURIComponent(curUser.username()), {
            type: "GET",
            success: function(result) {
              displaySuccessMessage($.i18n.prop("user.unlocked",curUser.username()));
            },
            error: function(result) {
              var obj = jQuery.parseJSON(result.responseText);
              displayRedbackError(obj);
            }
        });
      }

      // value is boolean
      this.changePasswordChangeRequired=function(value){
        this.passwordChangeRequired(value);
        var curUser = this;
        var url = "restServices/redbackServices/userService/passwordChangeRequired/"+encodeURIComponent(curUser.username());
        if (value==false){
          url = "restServices/redbackServices/userService/passwordChangeNotRequired/"+encodeURIComponent(curUser.username());
        }
        $.ajax(url, {
            type: "GET",
            success: function(result) {
              displaySuccessMessage($.i18n.prop("user.passwordChangeRequired.updated",curUser.username()));
            },
            error: function(result) {
              var obj = jQuery.parseJSON(result.responseText);
              displayRedbackError(obj);
            }
        });
      };

      this.i18n = $.i18n.prop;
  }

  /**
   * view for admin user creation
   */
  AdminUserViewModel=function() {
    this.user = new User("admin","","", "the administrator");
  }

  /**
   * open a modal box to create admin user
   */
  adminCreateBox=function() {
    window.redbackModel.createUser=true;
    jQuery("#main-content").attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');
    var viewModel = new AdminUserViewModel();
    ko.applyBindings(viewModel);
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
  }

  /**
   * open a modal box for login
   */
  loginBox=function(){

    screenChange();
    if (window.modalLoginWindow!=null){
      window.modalLoginWindow=null;
    }
    if (window.modalLoginWindow==null) {
      window.modalLoginWindow = $("#modal-login").modal();
      window.modalLoginWindow.bind('hidden', function () {
        $("#modal-login-err-message").hide();
      })
    }

    $("#user-login-form").validate({
      showErrors: function(validator, errorMap, errorList) {
        customShowError("#user-login-form",validator,errorMap,errorMap);
      }
    });
    $("#modal-login").delegate("#modal-login-ok", "click keydown keypress", function(e) {
      e.preventDefault();
      login();
    });

  }

  /**
   * callback success function on rest login call.
   * modal close and hide/show some links (login,logout,register...)
   * @param result
   */
  var successLoginCallbackFn=function(result){
    var logged = false;
    if (result == null) {
      logged = false;
    } else {
      if (result.user) {
        logged = true;
      }
    }
    $.log("successLoginCallbackFn, logged:"+logged);
    if (logged == true) {
      var user = mapUser(result.user);
      $.log("user.passwordChangeRequired:"+user.passwordChangeRequired());
      if (user.passwordChangeRequired()==true){
        changePasswordBox(true,false,user);
        return;
      }
      // not really needed as an exception is returned but "ceintures et bretelles" as we said in French :-)
      if (user.locked()==true){
        $.log("user locked");
        displayErrorMessage($.i18n.prop("accout.locked"));
        return
      }
      // FIXME check validated
      reccordLoginCookie(user);
      $("#login-link").hide();
      $("#logout-link").show();
      $("#register-link").hide();
      $("#change-password-link").show();
      if (window.modalLoginWindow){
        window.modalLoginWindow.modal('hide');
      }
      clearForm("#user-login-form");
      decorateMenuWithKarma(user);
      return;
    }
    $("#modal-login-err-message").html($.i18n.prop("incorrect.username.password"));
    $("#modal-login-err-message").show();
  }

  /**
   * callback error function on rest login call. display error message
   * @param result
   */
  var errorLoginCallbackFn= function(result) {
   var obj = jQuery.parseJSON(result.responseText);
   displayRedbackError(obj,"modal-login-err-message");
   $("#modal-login-err-message").show();
  }

  /**
   * callback complate function on rest login call. remove spinner from modal login box
   * @param result
   */
  var completeLoginCallbackFn=function(){
    $("#modal-login-ok").removeAttr("disabled");
    $("#small-spinner").remove();
  }

  /**
   * validate login box before ajax call
   */
  login=function(){
    $.log("user.js#login");
    $("#modal-login-err-message").html("");
    screenChange();
    var valid = $("#user-login-form").valid();
    if (!valid) {
        return;
    }
    $("#modal-login-ok").attr("disabled","disabled");

    //#modal-login-footer
    $('#modal-login-footer').append(smallSpinnerImg());

    var url = 'restServices/redbackServices/loginService/logIn?userName='+$("#user-login-form-username").val();
    url += "&password="+$("#user-login-form-password").val();

    loginCall($("#user-login-form-username").val(),$("#user-login-form-password").val()
        ,successLoginCallbackFn,errorLoginCallbackFn,completeLoginCallbackFn);

  }

  /**
   * call REST method for login
   * @param username
   * @param password
   * @param successCallbackFn
   * @param errorCallbackFn
   * @param completeCallbackFn
   */
  loginCall=function(username,password,successCallbackFn, errorCallbackFn, completeCallbackFn) {
    var url = 'restServices/redbackServices/loginService/logIn?userName='+username;
    url += "&password="+password;

    $.ajax({
      url: url,
      success: successCallbackFn,
      error: errorCallbackFn,
      complete: completeCallbackFn
    });

  }

  /**
   *
   * @param previousPassword display and validate previous password text field
   * @param registration are we in registration mode ?
   */
  changePasswordBox=function(previousPassword,registration,user){
    screenChange();
    $.log("changePasswordBox previousPassword:"+previousPassword+",registration:"+registration+",user:"+user);
    if (previousPassword==true){
      $("#password-change-form-current-password-div").show();
      $("#password-change-form-current-password").addClass("required");
    }else{
      $("#password-change-form-current-password-div").hide();
      $("#password-change-form-current-password").removeClass("required");
    }
    if (window.modalChangePasswordBox == null) {
      window.modalChangePasswordBox = $("#modal-password-change").modal({backdrop:'static',show:false});
      window.modalChangePasswordBox.bind('hidden', function () {
        $("#modal-password-change-err-message").hide();
      })
      $("#modal-password-change").delegate("#modal-change-password-ok", "click keydown keypress", function(e) {
        e.preventDefault();
        changePassword(previousPassword,registration,user);
      });
    }
    window.modalChangePasswordBox.modal('show');
    $("#password-change-form").validate({
      rules: {
        passwordChangeFormNewPasswordConfirm : {
          equalTo: "#passwordChangeFormNewPassword"
        }
      },
      showErrors: function(validator, errorMap, errorList) {
        customShowError("#password-change-form",validator,errorMap,errorMap);
      }
    });


    $("#modal-password-change").focus();
  }

  /**
   * display modal box for updating current user details
   */
  editUserDetailsBox=function(){
    clearUserMessages();
    $("#modal-user-edit-err-message").hide();
    $("#modal-user-edit-err-message").html("");
    if (window.modalEditUserBox == null) {
      window.modalEditUserBox = $("#modal-user-edit").modal({backdrop:'static',show:false});
      window.modalEditUserBox.bind('hidden', function () {
        $("#modal-user-edit-err-message").hide();
      })
      $("#modal-user-edit #modal-user-edit-ok").on( "click keydown keypress", function(e) {
        e.preventDefault();
        $.log("user.js#editUserDetailsBox");
        var valid = $("#user-edit-form").valid();
        if (!valid) {
            return;
        }
        var user = {
          username:currentUser.username,
          fullName:$("#modal-user-edit #fullname").val(),
          email:$("#modal-user-edit #email").val(),
          previousPassword:$("#modal-user-edit #userEditFormCurrentPassword").val(),
          password:$("#modal-user-edit #userEditFormNewPassword").val(),
          confirmPassword:$("#modal-user-edit #userEditFormNewPasswordConfirm").val()
        };
        editUserDetails(user);
      });
    }
    var currentUser = getUserFromLoginCookie();
    $("#modal-user-edit #username").html(currentUser.username);
    $("#modal-user-edit #fullname").val(currentUser.fullName);
    $("#modal-user-edit #email").val(currentUser.email);
    window.modalEditUserBox.modal('show');
    $("#user-edit-form").validate({
      rules: {
        userEditFormNewPasswordConfirm : {
          equalTo: "#userEditFormNewPassword"
        }
      },
      showErrors: function(validator, errorMap, errorList) {
        customShowError("#user-edit-form",validator,errorMap,errorMap);
      }
    });


    $("#modal-user-edit").focus();
  }

  /**
   * REST call to update current user
   * @param user
   */
  editUserDetails=function(user){
    $("#modal-user-edit-err-message").html("");
    $.ajax("restServices/redbackServices/userService/updateMe", {
        data: "{\"user\": " +  ko.toJSON(user)+"}",
        contentType: 'application/json',
        type: "POST",
        dataType: 'json',
        success: function(result) {
          var created = JSON.parse(result);
          // FIXME i18n
          if (created == true) {
            displaySuccessMessage("details updated.");
            window.modalEditUserBox.modal('hide');
            reccordLoginCookie(user);
            clearForm("#user-edit-form");
            return this;
          } else {
            displayErrorMessage("details cannot be updated","modal-user-edit-err-message");
          }
        },
        error: function(result) {
          var obj = jQuery.parseJSON(result.responseText);
          $("#modal-user-edit-err-message").show();
          displayRedbackError(obj,"modal-user-edit-err-message");
        }
      });

  }


  /**
   *
   * @param previousPassword display and validate previous password text field
   * @param registration are we in registration mode ? if yes the user will be logged
   */
  changePassword=function(previousPassword,registration,user){
    $.log("user.js#changePassword");
    var valid = $("#password-change-form").valid();
    if (valid==false) {
        return;
    }
    $('#modal-password-change-footer').append(smallSpinnerImg());

    if (registration==true) {
      var url = 'restServices/redbackServices/passwordService/changePasswordWithKey?';
      url += "password="+$("#passwordChangeFormNewPassword").val();
      url += "&passwordConfirmation="+$("#passwordChangeFormNewPasswordConfirm").val();
      url += "&key="+window.redbackModel.key;
    } else {
      var url = 'restServices/redbackServices/passwordService/changePassword?';
      url += "password="+$("#passwordChangeFormNewPassword").val();
      url += "&passwordConfirmation="+$("#passwordChangeFormNewPasswordConfirm").val();
      url += "&previousPassword="+$("#password-change-form-current-password").val();
      url += "&userName="+user.username();
    }

    $.ajax({
      url: url,
      success: function(result){
        $.log("changePassword#success result:"+result);
        var user = mapUser(result.user);
        if (user) {
          window.modalChangePasswordBox.modal('hide');
          $.log("changePassword#sucess,registration:"+registration);
          if (registration==true) {
            displaySuccessMessage($.i18n.prop('change.password.success.section.title'))
            loginCall(user.username(), $("#passwordChangeFormNewPassword").val(),successLoginCallbackFn);
          } else {
            displaySuccessMessage($.i18n.prop('change.password.success.section.title'));
          }
        } else {
          displayErrorMessage("issue appended");
        }

      },
      complete: function(){
        $("#small-spinner").remove();
        window.modalChangePasswordBox.modal('hide');
      },
      error: function(result) {
       var obj = jQuery.parseJSON(result.responseText);
       displayRedbackError(obj);
      }
    });

    //$.urlParam('validateMe')
    // for success i18n key change.password.success.section.title
  }

  /**
   * @param data User response from redback rest api
   */
  mapUser=function(data) {
    return new User(data.username, data.password, null,data.fullName,data.email,data.permanent,data.validated,
                    data.timestampAccountCreation,data.timestampLastLogin,data.timestampLastPasswordChange,
                    data.locked,data.passwordChangeRequired,self);
  }


});


