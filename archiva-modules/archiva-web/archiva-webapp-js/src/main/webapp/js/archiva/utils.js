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

/**
 * log message in the console
 */
$.log = (function(message) {
  if ( !window.archivaJavascriptLog ){
    return;
  }
  if (typeof window.console != 'undefined' && typeof window.console.log != 'undefined') {
    console.log(message);
  } else {
    // do nothing no console
  }
});

/**
 * return value of a param in the url
 * @param name
 */
$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    if (results) {
      return results[1] || 0;
    }
    return null;
}

/**
 * display a success message
 * @param text the success text
 * @param idToAppend the id to append the success box
 */
displaySuccessMessage=function(text,idToAppend){
  var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
  $.tmpl($("#alert-message-success").html(), { "message" : text }).appendTo( textId );
  $(textId).focus();
}

/**
 * display an error message
 * @param text the success text
 * @param idToAppend the id to append the success box
 */
displayErrorMessage=function(text,idToAppend){
  var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
  $.tmpl($("#alert-message-error").html(), { "message" : text }).appendTo( textId );
  $(textId).focus();
}

/**
 * display a warning message
 * @param text the success text
 * @param idToAppend the id to append the success box
 */
displayWarningMessage=function(text,idToAppend){
  var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
  $.tmpl($("#alert-message-warning").html(), { "message" : text }).appendTo( textId );
  $(textId).focus();
}

displayInfoMessage=function(text,idToAppend){
  var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
  $.tmpl($("#alert-message-info").html(), { "message" : text }).appendTo( textId );
  $(textId).focus();
}

/**
 * clear #main-content and call clearUserMessages
  */
screenChange=function(){
  $("#main-content").html("");
  $("#main-content").removeAttr("data-bind");
  clearUserMessages();
}

/**
 * clear content of id if none clear content of #user-messages
  * @param idToAppend
 */
clearUserMessages=function(idToAppend){
  var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
  $(textId).html('');
}

/**
 * clear all input text and password found in the the selector
 * @param selectorStr
 */
clearForm=function(selectorStr){
  $(selectorStr+" input[type='text']").each(function(ele){
    $(this).val("");
  });
  $(selectorStr+" input[type='password']").each(function(ele){
    $(this).val("");
  });

}

/**
 * open a confirm dialog based on bootstrap modal
 * @param okFn callback function to call on ok confirm
 * @param okMessage
 * @param cancelMessage
 * @param title
 */
openDialogConfirm=function(okFn, okMessage, cancelMessage, title,bodyText){
  if (window.modalConfirmDialog==null) {
    window.modalConfirmDialog = $("#dialog-confirm-modal").modal();//{backdrop:'static',show:false}
    window.modalConfirmDialog.bind('hidden', function () {
      $("#dialog-confirm-modal-header-title").html("");
      $("#dialog-confirm-modal-body-text").html("");
    })
    $("#dialog-confirm-modal-cancel").on("click", function(){
      window.modalConfirmDialog.modal('hide');
    });
  }
  $("#dialog-confirm-modal-header-title").html(title);
  $("#dialog-confirm-modal-body-text").html(bodyText);
  if (okMessage){
    $("#dialog-confirm-modal-ok").html(okMessage);
  }
  if (cancelMessage){
    $("#dialog-confirm-modal-cancel").html(cancelMessage);
  }
  window.modalConfirmDialog.modal('show');

  // unbind previous events !!
  $("#dialog-confirm-modal-ok").off( );
  $("#dialog-confirm-modal-ok").on("click", okFn);

}

/**
 * return a small spinner html img element
 */
smallSpinnerImg=function(){
  return "<img id=\"small-spinner\" src=\"images/small-spinner.gif\"/>";
};

removeSmallSpinnerImg=function(){
  $("#small-spinner").remove();
}

mediumSpinnerImg=function(){
  return "<img id=\"medium-spinner\" src=\"images/medium-spinner.gif\"/>";
};

removeMediumSpinnerImg=function(){
  $("#medium-spinner").remove();
}

removeMediumSpinnerImg=function(selector){
  $(selector+" #medium-spinner").remove();
}

closeDialogConfirm=function(){
  window.modalConfirmDialog.modal('hide');
}

closeDialogConfirmui=function(){
  $("#dialog-confirm" ).dialog("close");
}

/**
 * open a confirm dialog with jqueryui
 * @param okFn callback function to call on ok confirm
 * @param okMessage
 * @param cancelMessage
 * @param title
 */
openDialogConfirmui=function(okFn, okMessage, cancelMessage, title){
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

mapStringArray=function(data){
  if (data) {
    if ($.isArray(data)){
      return $.map(data,function(item){
        return item;
     });
    } else {
      return new Array(data);
    }
  }
  return null;
}

// extends jquery tmpl to support var def
$.extend($.tmpl.tag, {
    "var": {
        open: "var $1;"
    }
});

/**
 * display redback error from redback json error response
 * {"redbackRestError":{"errorMessages":{"args":1,"errorKey":"user.password.violation.numeric"}}}
 * @param obj
 * @param idToAppend
 */
displayRedbackError=function(obj,idToAppend) {
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

/*
 * generic function to display error return by rest service
 * if fieldName is here the function will try to find a field with this name and add a span on it
 * if not error is displayed in #user-messages div
 */
displayRestError=function(data,idToAppend){

  if (data.redbackRestError){
    displayRedbackError(archivaRestError,idToAppend)
  }
  // if we have the fieldName display error on it
  if (data.archivaRestError && data.archivaRestError.fieldName){
    if ($("#main-content #"+data.archivaRestError.fieldName)){
      var message=null;
      if (data.archivaRestError.errorKey) {
        message=$.i18n.prop('data.archivaRestError.errorKey');
      } else {
        message=data.archivaRestError.errorMessage;
      }
      $( "#main-content div.clearfix" ).removeClass( "error" );
      $( "#main-content span.help-inline" ).remove();
      $("#main-content #"+data.archivaRestError.fieldName).parents( "div.clearfix" ).addClass( "error" );
      $("#main-content #"+data.archivaRestError.fieldName).parent().append( "<span class=\"help-inline\">" + message + "</span>" );
      return;
    }
    // we don't have any id with this fieldName so continue
  }

  if (data.archivaRestError && data.archivaRestError.errorKey && data.archivaRestError.errorKey.length>0){
      $.log("with errorKey:"+dataarchivaRestError.errorKey);
      displayErrorMessage($.i18n.prop( data.archivaRestError.errorKey ),idToAppend);
    } else {
      $.log("data.errorMessage:"+data.archivaRestError.errorMessage);
      displayErrorMessage(data.archivaRestError.errorMessage,idToAppend);
  }

}

/**
 * used by validation error to customize error display in the ui
 * @param validator
 * @param errorMap
 * @param errorList
 */
customShowError=function(formId, validator, errorMap, errorList) {
  $( "#"+formId+" div.control-group" ).removeClass( "error" );
  $( "#"+formId+" span.help-inline" ).remove();
  for ( var i = 0; errorList[i]; i++ ) {
    var error = errorList[i];
    var field = $("#"+error.element.id);
    field.parents( "div.control-group" ).addClass( "error" );
    field.parent().append( "<span class=\"help-inline\">" + error.message + "</span>" );
  }
}

timestampNoCache=function(){
  if (!window.archivaDevMode){
    return "";
  }
  return "&_="+jQuery.now();
}

appendTemplateUrl=function(){
  return "?"+appendArchivaVersion()+timestampNoCache();
}