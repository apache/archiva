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

  // define a container object with various datas
  window.redbackModel = {usersViewModel:null,userOperationNames:null,key:null,userCreate:false};


  displayRedbackError=function(obj,idToAppend) {
    // {"redbackRestError":{"errorMessages":{"args":1,"errorKey":"user.password.violation.numeric"}}}
    if ($.isArray(obj.redbackRestError.errorMessages)) {
      $.log("displayRedbackError with array");
      for(var i=0; i<obj.redbackRestError.errorMessages.length; i++ ) {
        if(obj.redbackRestError.errorMessages[i].errorKey) {
          $.log("displayRedbackError with array loop");
          displayErrorMessage($.i18n.prop( obj.redbackRestError.errorMessages[i].errorKey, obj.redbackRestError.errorMessages[i].args ),idToAppend);
        }
      }
    } else {
      $.log("displayRedbackError no array");
      displayErrorMessage($.i18n.prop( obj.redbackRestError.errorMessages.errorKey, obj.redbackRestError.errorMessages.args ),idToAppend);
    }
  }

  // unbinding
  $("#user-create-form-cancel-button").on("click", function(){
    $('#user-create').hide();
  });


  $("#user-create").on("submit",function(){
    //nothing
  });

  smallSpinnerImg=function(){
    return "<img id=\"login-spinner\" src=\"images/small-spinner.gif\"/>";
  };

  openDialogConfirm=function(okFn, okMessage, cancelMessage, title){
    $("#dialog-confirm" ).dialog({
      resizable: false,
      title: title,
      modal: true,
      show: 'slide',
      buttons: [{
        text: okMessage,
        click: okFn},
        {
        text: cancelMessage,
        click:function() {
          $(this).dialog( "close" );
        }
      }]
    });
  }

  closeDialogConfirm=function(){
    $("#dialog-confirm" ).dialog("close");
  }

  /**
   * return a user see user.js if user logged otherwise null
   */
  userLogged=function() {
    // call restServices/redbackServices/loginService/isLogged to know
    // if a session exists and check the cookie
    var userLogged = true;
    $.ajax("restServices/redbackServices/loginService/isLogged", {
      type: "GET",
      async: false,
      success: function(data) {
        userLogged = JSON.parse(data);
      }
    });
    if (userLogged == false)
    {
      return null;
    }
    return jQuery.parseJSON($.cookie('redback_login'));
  }

});