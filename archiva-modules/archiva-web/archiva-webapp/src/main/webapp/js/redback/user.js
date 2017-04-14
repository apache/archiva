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
define("redback.user",["jquery","utils","i18n","jquery.validate","knockout","knockout.simpleGrid","purl","archiva.main"],
function(jquery,utils,i18n,jqueryValidate,ko,koSimpleGrid,purl) {

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
   * @param readOnly
   * @param uuserManagerId
   * @param validationToken
   */
  User=function(username, password, confirmPassword,fullName,email,permanent,validated,timestampAccountCreation,
                timestampLastLogin,timestampLastPasswordChange,locked,passwordChangeRequired,ownerViewModel,readOnly,
                userManagerId,validationToken) {
    var self=this;
    // Potentially Editable Field.
    this.username = ko.observable(username);
    this.username.subscribe(function(newValue){self.modified(true)});
    // Editable Fields.
    this.password = ko.observable(password);
    this.password.subscribe(function(newValue){self.modified(true)});

    this.confirmPassword = ko.observable(confirmPassword);
    this.confirmPassword.subscribe(function(newValue){self.modified(true)});

    this.fullName = ko.observable(fullName);
    this.fullName.subscribe(function(newValue){self.modified(true)});

    this.email = ko.observable(email);
    this.email.subscribe(function(newValue){self.modified(true)});

    this.permanent = ko.observable(permanent);
    this.permanent.subscribe(function(newValue){self.modified(true)});

    this.validated = ko.observable(validated);
    this.validated.subscribe(function(newValue){self.modified(true)});

    // Display Only Fields.
    this.timestampAccountCreation = ko.observable(timestampAccountCreation);
    this.timestampLastLogin = ko.observable(timestampLastLogin);
    this.timestampLastPasswordChange = ko.observable(timestampLastPasswordChange);
    // admin only
    this.locked = ko.observable(locked);
    this.locked.subscribe(function(newValue){self.modified(true)});

    this.passwordChangeRequired = ko.observable(passwordChangeRequired);
    this.passwordChangeRequired.subscribe(function(newValue){self.modified(true)});

    this.assignedRoles = ko.observableArray(new Array());
    this.assignedRoles.subscribe(function(newValue){self.modified(true)});

    this.modified=ko.observable(false);

    this.readOnly=readOnly;

    this.userManagerId=userManagerId;

    this.rememberme=ko.observable(false);

    this.validationToken=validationToken;

    this.logged=false;

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
          data: ko.toJSON(this),
          contentType: 'application/json',
          type: "POST",
          dataType: 'json',
          success: function(result) {
            var created = result;
            if (created == true) {
              displaySuccessMessage( $.i18n.prop("user.created",currentUser.username()));
              if (successFnCallback){
                successFnCallback(currentUser);
              }
              clearForm("#main-content #user-create");
              $("#main-content").find("#user-create").hide();
              activateUsersGridTab();
              return this;
            } else {
              displayErrorMessage("user cannot created");
            }
          }
        });
    };

    this.createAdmin = function(succesCallbackFn,errorCallbackFn) {
      $.log("user.js#createAdmin");
      var valid = $("#user-create").valid();
      $.log("create admin");
      if (!valid) {
          return;
      }
      var currentAdminUser = this;
      $.ajax("restServices/redbackServices/userService/createAdminUser", {
          data: ko.toJSON(this),
          contentType: 'application/json',
          type: "POST",
          dataType: 'json',
          success: function(result) {
            var created = result;
            if (created == true) {
              displaySuccessMessage( $.i18n.prop("user.admin.created"));
              var onSuccessCall=function(result){
                var logUser = mapUser(result);
                currentAdminUser.validationToken=logUser.validationToken;
                reccordLoginCookie(currentAdminUser);
                addValidationTokenHeader(currentAdminUser);
                window.archivaModel.adminExists=true;
                screenChange();
                checkCreateAdminLink();
                checkSecurityLinks();
                if(succesCallbackFn){
                  succesCallbackFn();
                }
              }
              loginCall(currentAdminUser.username(), currentAdminUser.password(),false,onSuccessCall);
              return this;
            } else {
              displayErrorMessage("admin user not created");
            }
          },
          error: function(data){
            if(errorCallbackFn){
              errorCallbackFn();
            }
          }
        });
    };


    this.update=function(){
      var currentUser = this;
      $.ajax("restServices/redbackServices/userService/updateUser", {
          data: ko.toJSON(this),
          contentType: 'application/json',
          type: "POST",
          dataType: 'json',
          success: function(result) {
            var updated = result;
            if (updated == true) {
              clearUserMessages();
              displaySuccessMessage($.i18n.prop("user.updated",currentUser.username()));
              $("#main-content").find("#users-view-tabs-li-user-edit").find("a").html($.i18n.prop("add"));
              clearForm("#main-content #user-create");
              activateUsersGridTab();
              return this;
            } else {
              displayErrorMessage("user cannot be updated");
            }
          }
        });
    }

    this.save=function(){
      $.log("user.save create:"+window.redbackModel.createUser);
      if (window.redbackModel.createUser==true){
        var valid = $("#main-content").find("#user-create").valid();

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
    };

    this.updateAssignedRoles=function(){
      $.log("user#updateAssignedRoles");
      var curUser = this;
      clearUserMessages();
      $.ajax("restServices/redbackServices/roleManagementService/updateUserRoles", {
          data: ko.toJSON(this),
          contentType: 'application/json',
          type: "POST",
          dataType: 'json',
          success: function(result) {
            displaySuccessMessage($.i18n.prop("user.roles.updated",curUser.username()));
          }
        });
    };

    this.lock=function(){
      this.locked(true);
      var curUser = this;
      clearUserMessages();
      $.ajax("restServices/redbackServices/userService/lockUser/"+encodeURIComponent(curUser.username()), {
          type: "GET",
          success: function(result) {
            displaySuccessMessage($.i18n.prop("user.locked",curUser.username()));
            curUser.modified(false);
          }
        });
    };

    this.unlock=function(){
      this.locked(false);
      var curUser = this;
      clearUserMessages();
      $.ajax("restServices/redbackServices/userService/unlockUser/"+encodeURIComponent(curUser.username()), {
          type: "GET",
          success: function(result) {
            displaySuccessMessage($.i18n.prop("user.unlocked",curUser.username()));
            curUser.modified(false);
          }
      });
    };

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
            displaySuccessMessage($.i18n.prop("user.passwordChangeRequired.updated",curUser.username(),value));
            curUser.modified(false);
          }
      });
    };

  }

  /**
   * view for admin user creation
   */
  AdminUserViewModel=function() {
    this.user = new User("admin","","", "the administrator");
    var self=this;
    saveUser=function(){
      if(! $("#user-create" ).valid() ) {
        return;
      }
      self.user.createAdmin(function(){
          // go to search when admin created
          window.sammyArchivaApplication.setLocation("#search");
        }
      );
    }
  }

  /**
   * open a modal box to create admin user
   */
  adminCreateBox=function() {
    var mainContent=$("#main-content");

    $.ajax("restServices/redbackServices/userService/isAdminUserExists", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        var adminExists = data;
        window.archivaModel.adminExists=adminExists;
        if (adminExists == false) {

          window.redbackModel.createUser=true;
          mainContent.attr("data-bind",'template: {name:"redback/user-edit-tmpl",data: user}');
          var viewModel = new AdminUserViewModel();
          ko.applyBindings(viewModel,mainContent.get(0));
          $.log("adminCreateBox");
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
          $("#username").prop('disabled',true);
          // desactivate roles pill when adding user
          $("#edit_user_details_pills_headers").hide();

        } else {
          window.sammyArchivaApplication.setLocation("#search");
        }

      }
    });
  }

  /**
   * open a modal box for login
   */
  loginBox=function(){

    if (window.modalLoginWindow!=null){
      window.modalLoginWindow=null;
    }
    if (window.modalLoginWindow==null) {
      window.modalLoginWindow = $("#modal-login").modal();
      window.modalLoginWindow.on('hidden', function () {
        $("#modal-login-err-message").hide();
        removeValidationErrorMessages("#user-login-form");
      });
      // focus on user name
      window.modalLoginWindow.on('shown', function (e) {
        $("#user-login-form-username" ).focus();
      });
      window.modalLoginWindow.keypress( function (event) {
        if (event.which==13){
          $("#modal-login-ok" ).trigger("click");
        }
      });
    }

    var user=getUserFromLoginCookie();
    if(user){
      $.log("found user in cookie rememberme:"+(user.rememberme()));
      if(user.rememberme()){
        $("#user-login-form-username" ).val(user.username());
        $("#user-login-form-password" ).val(user.password());
        $("#user-login-form-rememberme" ).attr("checked","true");
      }
    } else {
      $.log("user not in cookie");
    }

    var rememberMe=window.cookieInformation.rememberMeEnabled;
    $.log("rememberMe:"+rememberMe);
    if (rememberMe=='true'){
      $("#user-login-form-rememberme-label" ).hide();
      $("#user-login-form-rememberme" ).attr("disabled","true");
      if($("#user-login-form-rememberme" ).get(0 ).checked){
        $("#user-login-form-rememberme" ).get(0 ).checked=false;
      }
      $("#user-login-form-username" ).val("");
      $("#user-login-form-password" ).val("");
    }

    var userLoginForm = $("#user-login-form");

    userLoginForm.validate({
      showErrors: function(validator, errorMap, errorList) {
        customShowError("#user-login-form",validator,errorMap,errorMap);
      }
    });
    $("#modal-login-ok").off();
    $("#modal-login-ok").on("click", function(e) {
      e.preventDefault();
      login();
    });

    $("#modal-login-password-reset").off();
    $("#modal-login-password-reset").on("click", function(e) {
      e.preventDefault();
      $.log("password reset");
      passwordReset();
    });

  };

  resetPasswordForm=function(key){
    $.log("resetPasswordForm:"+key);
    changePasswordBox(null,false,null,function(){
        $.log("ok chgt pwd")
        $.log("user.js#changePassword");
        var valid = $("#password-change-form").valid();
        if (valid==false) {
            return;
        }
        var url = 'restServices/redbackServices/passwordService/changePasswordWithKey?';
        url += "password="+$("#passwordChangeFormNewPassword").val();
        url += "&passwordConfirmation="+$("#passwordChangeFormNewPasswordConfirm").val();
        url += "&key="+key;
        $.log("url:"+url);

        $.ajax({
          url: url,
          success: function(result){
            $.log("changePassword#success result:"+result);
            var user = mapUser(result);
            if (user) {
              window.modalChangePasswordBox.modal('hide');
              displaySuccessMessage($.i18n.prop('change.password.success.section.title'));
            } else {
              displayErrorMessage("issue appended");
            }
            window.modalChangePasswordBox.modal('hide');
            var curHash = getUrlHash();
            var url = $.url(window.location);
            var newLocation=url.attr("path");
            var requestLang=url.param("request_lang");
            if(requestLang){
              newLocation+="?request_lang="+requestLang;
            }
            if(curHash){
              newLocation+="#"+curHash;
            }else{
              newLocation+="#search";
            }
            window.location=newLocation;
          },
           statusCode: {
             500: function(data){
               $("#modal-password-change-err-message" ).empty();
               displayRestError($.parseJSON(data.responseText),"modal-password-change-err-message");
               $("#modal-password-change-err-message" ).show();
             }
           }
        });

      }
    );
  }

  ResetPasswordRequest=function(username,applicationUrl){
    this.username=username;
    this.applicationUrl=applicationUrl;
  }

  passwordReset=function(){
    var userLoginFormUsername=$("#user-login-form-username" );
    var username = userLoginFormUsername.val();
    if(username.trim().length<1){
      var errorList=[{
        message: $.i18n.prop("username.cannot.be.empty"),
  		  element: userLoginFormUsername.get(0)
      }];
      customShowError("#user-login-form", null, null, errorList);
      return;
    }

    if (window.modalLoginWindow){
      window.modalLoginWindow.modal('hide');
    }
    $("#user-messages" ).html(mediumSpinnerImg());

    $.ajax({
        url: "restServices/archivaServices/archivaAdministrationService/applicationUrl",
        type: "GET",
        dataType: 'text',
        success: function(data){

          $.ajax("restServices/redbackServices/userService/resetPassword", {
            type: "POST",
            data:  JSON.stringify(new ResetPasswordRequest(username,data)),
            contentType: "application/json",
            success: function(result) {
              clearUserMessages();
              displayInfoMessage($.i18n.prop("password.reset.success"));
            }
          });
        }
    });
  }

  /**
   * validate login box before ajax call
   */
  login=function(){
    $.log("user.js#login");

    $("#modal-login-err-message").empty();

    var valid = $("#user-login-form").valid();
    if (!valid) {
        return;
    }
    $("#modal-login-ok").button("loading");

    $('#modal-login-footer').append(smallSpinnerImg());

    var rememberme=$('#user-login-form-rememberme').is(':checked');
    $.log("user.js#login, rememberme:"+rememberme);
    window.redbackModel.rememberme=rememberme;
    window.redbackModel.password=$("#user-login-form-password").val();

    loginCall($("#user-login-form-username").val(),window.redbackModel.password,rememberme
        ,successLoginCallbackFn,errorLoginCallbackFn,completeLoginCallbackFn);

  }

  /**
   * call REST method for login
   * @param username
   * @param password
   * @param rememberme
   * @param successCallbackFn
   * @param errorCallbackFn
   * @param completeCallbackFn
   */
  loginCall=function(username,password,rememberme,successCallbackFn, errorCallbackFn, completeCallbackFn) {
    var url = 'restServices/redbackServices/loginService/logIn';

    $.ajax({
      url: url,
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify({username:username,password:password}),
      success: successCallbackFn,
      error: errorCallbackFn,
      complete: completeCallbackFn
    });

  };

  /**
   *
   * @param previousPassword display and validate previous password text field
   * @param registration are we in registration mode ?
   */
  changePasswordBox=function(previousPassword,registration,user,okFn){
    $.log("changePasswordBox");
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
        if ( $.isFunction(okFn)){
          okFn();
        } else {
          changePassword(previousPassword,registration,user);
        }
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

  EditUserDetailViewModel=function(user){
    this.user=user;
  }

  /**
   * display modal box for updating current user details
   */
  editUserDetailsBox=function(){
    clearUserMessages();
    $("#modal-user-edit-err-message").hide();
    $("#modal-user-edit-err-message").empty();
    if (window.modalEditUserBox == null) {
      window.modalEditUserBox = $("#modal-user-edit").modal({backdrop:'static',show:false});
      window.modalEditUserBox.bind('hidden', function () {
        $("#modal-user-edit-err-message").hide();
      });
      $("#modal-user-edit").find("#modal-user-edit-ok").on( "click keydown keypress", function(e) {
        e.preventDefault();
        $.log("user.js#editUserDetailsBox");
        var valid = $("#user-edit-form").valid();
        if (!valid) {
            return;
        }
        var user = {
          username:currentUser.username,
          fullName:$("#modal-user-edit").find("#fullname").val(),
          email:$("#modal-user-edit").find("#email").val(),
          previousPassword:$("#modal-user-edit").find("#userEditFormCurrentPassword").val(),
          password:$("#modal-user-edit").find("#userEditFormNewPassword").val(),
          confirmPassword:$("#modal-user-edit").find("#userEditFormNewPasswordConfirm").val()
        };
        var kuser =getUserFromLoginCookie();
        user.rememberme=function(){
          return kuser.rememberme();
        }
        editUserDetails(user);
      });
    }
    var currentUser = getUserFromLoginCookie();
    /*$("#modal-user-edit").find("#username").html(currentUser.username);
    $("#modal-user-edit").find("#fullname").val(currentUser.fullName);
    $("#modal-user-edit").find("#email").val(currentUser.email);*/

    $("#modal-user-edit-content" ).attr("data-bind",'template: {name:"modal-user-edit-tmpl"}');

    var editUserDetailViewModel=new EditUserDetailViewModel(currentUser);
    ko.applyBindings(editUserDetailViewModel,$("#modal-user-edit-content").get(0));

    if(currentUser.readOnly){
      $("#modal-user-edit-footer" ).hide();
    }

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
    $("#modal-user-edit-err-message").empty();
    $.ajax("restServices/redbackServices/userService/updateMe", {
        data: ko.toJSON(user),
        contentType: 'application/json',
        type: "POST",
        dataType: 'json',
        success: function(result) {
          var created = result;
          if (created == true) {
            displaySuccessMessage( $.i18n.prop("user.details.updated"));
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
        var user = mapUser(result);
        if (user) {
          window.modalChangePasswordBox.modal('hide');
          if (registration==true) {
            $.log("changePassword#sucess,registration:"+registration);
            displaySuccessMessage($.i18n.prop('change.password.success.section.title'))
            loginCall(user.username(), $("#passwordChangeFormNewPassword").val(),true,successLoginCallbackFn,
                function(data){
                  displayRestError(data,"modal-password-change-content");
                }
                ,function(){
                  window.modalChangePasswordBox.modal('hide');
                  window.location=window.location.toString().substringBeforeFirst("?");
                  window.sammyArchivaApplication.setLocation("#search");
                });
          } else {
            displaySuccessMessage($.i18n.prop('change.password.success.section.title'));
          }
        } else {
          displayErrorMessage("issue appended");
        }
        window.modalChangePasswordBox.modal('hide');
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
                    data.locked,data.passwordChangeRequired,self,data.readOnly,data.userManagerId,
                    data.validationToken);
  }


});


