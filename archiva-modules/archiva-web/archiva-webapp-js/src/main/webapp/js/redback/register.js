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
   * open the register modal box
   */
  registerBox=function(){
    if (window.modalRegisterWindow==null) {
      window.modalRegisterWindow = $("#modal-register").modal({backdrop:'static',show:false});
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
    $("#modal-register").delegate("#modal-register-ok", "click keydown keypress", function(e) {
      e.preventDefault();
      register();
    });
    //$("#modal-register").focus();
  }

  /**
   * validate the register form and call REST service
   */
  register=function(){
    $.log("register.js#register");
    var valid = $("#user-register-form").valid();
    if (!valid) {
        return;
    }
    clearUserMessages();
    $("#modal-register-ok").attr("disabled","disabled");

    $('#modal-register-footer').append(smallSpinnerImg());

    var user = {};
    user.username = $("#user-register-form-username").val();
    user.fullName = $("#user-register-form-fullname").val();
    user.email = $("#user-register-form-email").val();
    jQuery.ajax({
      url:  'restServices/redbackServices/userService/registerUser',
      data:  '{"user":'+JSON.stringify(user)+'}',
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
        var obj = jQuery.parseJSON(result.responseText);
        displayRedbackError(obj);
        window.modalRegisterWindow.modal('hide');
      }
    })

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
         var obj = jQuery.parseJSON(result.responseText);
         $.log("validateKey#error response:"+obj);
         displayRedbackError(obj);
       }
    })
  }

});