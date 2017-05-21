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
define("redback",["jquery","utils","jquery.validate","jquery.json","knockout",
  "knockout.simpleGrid","redback.roles","redback.user","redback.users"],
function(jquery,utils,jqueryValidate,jqueryJson,ko) {

  // define a container object with various datas
  window.redbackModel = {userOperationNames:null,key:null,i18n:$.i18n.map};

  // unbinding
  $("#user-create-form-cancel-button").on("click", function(){
    $('#user-create').hide();
  });


  $("#user-create").on("submit",function(){
    //nothing
  });

  /**
   * call successFn on success with passing user object coming from cookie
   */
  userLogged=function(successFn,notLoggedFn) {
    // call restServices/redbackServices/loginService/isLogged to know
    // if a session exists and check the cookie
    $.log("userLogged");
    var userLogged = true;
    $.ajax("restServices/redbackServices/loginService/isLogged", {
      type: "GET",
      success: function(data) {
        $.log("isLogged:"+data);
        var user = data ? mapUser(data):null;
        var cookieUser = getUserFromLoginCookie();

        window.user=user;
        if(user){
          if (cookieUser!=null){
            $.log("cookieUser:"+cookieUser.password());
            user.password(cookieUser.password());
            user.rememberme(cookieUser.rememberme());
          }
          reccordLoginCookie(user);
        }
        $.log("userLogged:"+(user!=null));
        if (successFn){
          successFn(user ? user:null);
        }
        if(!user){
          if(notLoggedFn){
            notLoggedFn();
          }
        }
      },
      statusCode: {
        204: function() {
          if (successFn){
            successFn(null);
          }
          if(notLoggedFn){
            notLoggedFn();
          }
        }
      }
    });
  }

  Operation=function(name) {
    this.name=ko.observable(name);
  }

  /**
   * @param data Operation response from redback rest api
   */
  mapOperation=function(data) {
    return new Operation(data.name,null);
  }

  Permission=function(name,operation,resource) {
    this.name=ko.observable(name);
    this.operation=ko.observable(operation);
    this.resource=ko.observable(resource);
  }

  /**
   * @param data Permission response from redback rest api
   */
  mapPermission=function(data) {
    return new Permission(data.name,
                          data.operation?mapOperation(data.operation):null,
                          data.resource?mapResource(data.resource):null);
  }

  Resource=function(identifier,pattern) {
    this.identifier=ko.observable(identifier);
    this.pattern=ko.observable(pattern);
  }

  /**
   * @param data Resource response from redback rest api
   */
  mapResource=function(data) {
    return new Resource(data.identifier,data.pattern);
  }

  //---------------------------------------
  // register part
  //---------------------------------------

  /**
   * open the register modal box
   */
  registerBox=function(){
    var modalRegister=$("#modal-register");
    if (window.modalRegisterWindow==null) {
      window.modalRegisterWindow = modalRegister.modal({backdrop:'static',show:false});
      window.modalRegisterWindow.bind('hidden', function () {
        $("#modal-register-err-message").hide();
      })
    }
    window.modalRegisterWindow.modal('show');
    $("#user-register-form").validate({
      showErrors: function(validator, errorMap, errorList) {
        customShowError("#user-register-form",validator,errorMap,errorMap);
      }
    });
    modalRegister.delegate("#modal-register-ok", "click keydown keypress", function(e) {
      e.preventDefault();
      register();
    });
    //$("#modal-register").focus();
  }

  UserRegistrationRequest=function(user,applicationUrl){
    this.user=user;
    this.applicationUrl=applicationUrl;
  }

  /**
   * validate the register form and call REST service
   */
  register=function(){

    $.log("redback.js#register");
    var valid = $("#user-register-form").valid();
    if (!valid) {
        return;
    }
    clearUserMessages();
    $("#modal-register-ok").attr("disabled","disabled");

    $('#modal-register-footer').append(smallSpinnerImg());

      $.ajax({
          url: "restServices/archivaServices/archivaAdministrationService/applicationUrl",
          type: "GET",
          dataType: 'text',
          success: function(data){
            $.log("applicationUrl ok:"+data);

            var user = {
              username: $("#user-register-form-username").val(),
              fullName: $("#user-register-form-fullname").val(),
              email: $("#user-register-form-email").val()
            };

            var userRegistrationRequest=new UserRegistrationRequest(user,data);
            $.ajax({
              url:  'restServices/redbackServices/userService/registerUser',
              data:  JSON.stringify(userRegistrationRequest),
              type: 'POST',
              contentType: "application/json",
              success: function(result){
                var registered = false;
                if (result == "-1") {
                  registered = false;
                } else {
                  registered = true;
                }

                if (registered == true) {
                  window.modalRegisterWindow.modal('hide');
                  $("#register-link").hide();
                  // FIXME i18n
                  displaySuccessMessage("registered your key has been sent");
                }
              },
              complete: function(){
                $("#modal-register-ok").removeAttr("disabled");
                removeSmallSpinnerImg();
              },
              error: function(result) {
                window.modalRegisterWindow.modal('hide');
              }
            });
          }
      });

  }

  /**
   * validate a registration key and go to change password key
   * @param key
   */
  validateKey=function(key,registration) {
    // FIXME spinner display
    $.ajax({
      url: 'restServices/redbackServices/userService/validateKey/'+key,
      type: 'GET',
       success: function(result){
         window.redbackModel.key=key;
         $.log("validateKey#sucess");
         changePasswordBox(false,registration?registration:true,null);
       },
       complete: function(){
         // hide spinner
       },
       error: function(result) {
         $.log("validateKey#error");
       }
    })
  }

});