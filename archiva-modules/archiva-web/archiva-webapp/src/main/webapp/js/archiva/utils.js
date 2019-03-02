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

require(["jquery","jquery.tmpl","i18n","knockout"], function(jquery,jqueryTmpl,i18n,ko) {

  loadi18n=function(loadCallback){
    $.log("loadi18n");
    var browserLang = usedLang();
    $.log("use browserLang:"+browserLang);

    var options = {
      cache:false,
      mode: 'map',
      encoding:'utf-8',
      callback: loadCallback
    };
    loadAndParseFile("restServices/archivaServices/commonServices/getAllI18nResources?locale="+browserLang,options );
  }

  /**
   * log message in the console
   */
  $.log = (function(message) {
    if ( !window.archivaJavascriptLog ){
      return;
    }
    Sammy.log(message);
    /*return;
    if (typeof window.console != 'undefined' && typeof window.console.log != 'undefined') {
      console.log(message);
    } else {
      // do nothing no console
    }*/
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

  usedLang=function(){
    var browserLang = $.i18n.browserLang();
    var requestLang = $.urlParam('request_lang');
    if (requestLang) {
      browserLang=requestLang;
    }
    return browserLang;
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

  getUrlHash=function(){
    var matches = window.location.toString().match(/^[^#]*(#.+)$/);
    return matches ? matches[1] : null;
  }

  refreshContent=function(){
    var currentHash=getUrlHash();
    $.log("getUrlHash:"+currentHash);
    window.sammyArchivaApplication.runRoute('get',currentHash);

  }

  /**
   * clear #main-content and call clearUserMessages
    */
  screenChange=function(){
    var mainContent=$("#main-content");
    mainContent.empty();
    mainContent.removeAttr("data-bind");
    $("#body_content").find(".popover" ).hide();
    clearUserMessages();
    if(window.archivaModel.adminExists==false){
      displayErrorMessage($.i18n.prop("admin.creation.mandatory"));
    }
  }

  /**
   * clear content of id if none clear content of #user-messages
    * @param idToAppend
   */
  clearUserMessages=function(idToAppend){
    var textId = idToAppend ? $("#"+idToAppend) : $("#user-messages");
    $(textId).empty();
  }

  /**
   * clear all input text and password found in the the selector
   * @param selectorStr
   */
  clearForm=function(selectorStr){
    $(selectorStr).find("input[type='text']").each(function(ele){
      $(this).val("");
    });
    $(selectorStr).find("input[type='password']").each(function(ele){
      $(this).val("");
    });

  }

  /**
   * open a confirm dialog based on bootstrap modal
   * @param okFn callback function to call on ok confirm
   * @param okMessage message in the ok button
   * @param cancelMessage message in the cancel button
   * @param title title of the modal box
   * @param bodyText html content of the modal box
   */
  openDialogConfirm=function(okFn, okMessage, cancelMessage, title,bodyText){
    var dialogCancel=$("#dialog-confirm-modal-cancel");
    if (window.modalConfirmDialog==null) {
      window.modalConfirmDialog = $("#dialog-confirm-modal").modal();
      window.modalConfirmDialog.bind('hidden', function () {
        $("#dialog-confirm-modal-header-title").empty();
        $("#dialog-confirm-modal-body-text").empty();
      })
      dialogCancel.on("click", function(){
        window.modalConfirmDialog.modal('hide');
      });
    }
    $("#dialog-confirm-modal-header-title").html(title);
    $("#dialog-confirm-modal-body-text").html(bodyText);
    var dialogConfirmModalOk=$("#dialog-confirm-modal-ok");
    if (okMessage){
      dialogConfirmModalOk.html(okMessage);
    }
    if (cancelMessage){
      dialogCancel.html(cancelMessage);
    }
    window.modalConfirmDialog.modal('show');

    // unbind previous events !!

    dialogConfirmModalOk.off( );
    dialogConfirmModalOk.on("click", okFn);

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

  removeMediumSpinnerImg=function(){
    $("#medium-spinner").remove();
  }

  removeMediumSpinnerImg=function(selector){
    if (typeof selector == 'string') {
      $(selector).find("#medium-spinner").remove();
    } else {
      selector.find("#medium-spinner").remove();
    }

  }

  mediumSpinnerImg=function(){
    return "<img id=\"medium-spinner\" src=\"images/medium-spinner.gif\"/>";
  };

  closeDialogConfirm=function(){
    window.modalConfirmDialog.modal('hide');
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

  /**
   * display redback error from redback json error response
   * {"redbackRestError":{"errorMessages":{"args":1,"errorKey":"user.password.violation.numeric"}}}
   * @param obj
   * @param idToAppend
   */
  displayRedbackError=function(obj,idToAppend) {
    if ($.isArray(obj.errorMessages)) {
      $.log("displayRedbackError with array");
      for(var i=0; i<obj.errorMessages.length; i++ ) {
        if(obj.errorMessages[i].errorKey) {
          displayErrorMessage($.i18n.prop( obj.errorMessages[i].errorKey, obj.errorMessages[i].args ),idToAppend);
        }
        if(obj.errorMessages[i].message) {
          displayErrorMessage(obj.errorMessages[i].message,idToAppend);
        }
      }
    } else {
      displayErrorMessage($.i18n.prop( obj.errorMessages.errorKey, obj.errorMessages.args ),idToAppend);
    }
  }

  /*
   * generic function to display error return by rest service
   * if fieldName is here the function will try to find a field with this name and add a span on it
   * if not error is displayed in #user-messages div
   */
  displayRestError=function(data,idToAppend){
    $.log("displayRestError");
    // maybe data is just the response so test if if we have a responseText and transform it to json
    if(data.responseText){
      data= $.parseJSON(data.responseText);
    }
    if (data.redbackRestError){
      displayRedbackError(archivaRestError,idToAppend)
    }
    // if we have the fieldName display error on it
    if (data && data.fieldName){
      var mainContent=$("#main-content");

      if (mainContent.find("#"+data.fieldName)){
        var message=null;
        if (data.errorKey) {
          message=$.i18n.prop(data.errorKey);
        } else {
          message=data.errorMessage;
        }
        mainContent.find("div.clearfix" ).removeClass( "error" );
        mainContent.find("span.help-inline" ).remove();
        mainContent.find("#"+data.fieldName).parents( "div.clearfix" ).addClass( "error" );
        mainContent.find("#"+data.fieldName).parent().append( "<span class=\"help-inline\">" + message + "</span>" );
        return;
      }
      // we don't have any id with this fieldName so continue
    }

    if (data.errorKey && data.errorKey.length>0){
      displayErrorMessage($.i18n.prop( data.errorKey ),idToAppend);
    } else if (data.errorMessages){
      $.each(data.errorMessages, function(index, value) {
        if(data.errorMessages[index].errorKey) {
          displayErrorMessage( $.i18n.prop(data.errorMessages[index].errorKey,data.errorMessages[index].args?data.errorMessages[index].args:null),idToAppend);
        }
      });
    } else {
      $.log("print data.errorMessage:"+data.errorMessage);
      displayErrorMessage(data.errorMessage,idToAppend);
    }

  }

  /**
   * used by validation error to customize error display in the ui
   * @param selector
   * @param validator
   * @param errorMap
   * @param errorList
   */
  customShowError=function(selector, validator, errorMap, errorList) {
    removeValidationErrorMessages(selector);
    for ( var i = 0; errorList[i]; i++ ) {
      var error = errorList[i];
      if (typeof selector == 'string') {
        var field = $(selector).find("#"+error.element.id);
      } else {
        var field = selector.find("#"+error.element.id);
      }
      field.parents( "div.control-group" ).addClass( "error" );
      field.parent().append( "<span class=\"help-inline\">" + error.message + "</span>" );
    }
  }

  removeValidationErrorMessages=function(selector){
    if (typeof selector == 'string') {
      $(selector).find("div.control-group" ).removeClass( "error" );
      $(selector).find("span.help-inline").remove();
    } else {
      selector.find("div.control-group" ).removeClass( "error" );
      selector.find("span.help-inline").remove();
    }

  }

  appendArchivaVersion=function(){
    return "_archivaVersion="+window.archivaRuntimeInfo.version;
  }

  buildLoadJsUrl=function(srcScript){
    return srcScript+"?"+appendArchivaVersion()+"&_"+jQuery.now();
  }

  timestampNoCache=function(){
    if (!window.archivaDevMode){
      return "";
    }
    return "&_="+jQuery.now();
  }


  /**
   * mapping for a java Map entry
   * @param key
   * @param value
   * @param subscribeFn if any will be called as subscrible function field
   */
  Entry=function(key,value,subscribeFn){
    var self=this;
    this.modified=ko.observable(false);
    this.modified.subscribe(function(newValue){
      $.log("Entry modified");
    });

    this.key=ko.observable(key);
    this.key.subscribe(function(newValue){
      self.modified(true);
      if(subscribeFn){
        subscribeFn(newValue)
      }
    });

    this.value=ko.observable(value);
    this.value.subscribe(function(newValue){
      self.modified(true);
      if(subscribeFn){
        subscribeFn(newValue)
      }
    });


  }

  /**
   * map {"strings":["snapshots","internal"]} to an array
   * @param data
   */
  mapStringList=function(data){
    if (data && data.strings){
    return $.isArray(data.strings) ?
        $.map(data.strings,function(item){return item}): [data.strings];
    }
    return [];
  }

  /**
   * return an array with removing duplicate strings
   * @param strArray an array of string
   * @param sorted to sort or not
   */
  unifyArray=function(strArray,sorted){
    var res = [];
    $(strArray).each(function(idx,str){
      if ( $.inArray(str,res)<0){
        res.push(str);
      }
    });
    return sorted?res.sort():res;
  }

  goToAnchor=function(anchor){
    var curHref = window.location.href;
    curHref=curHref.substringBeforeLast("#");
    window.location.href=curHref+"#"+anchor;
  }

  //------------------------------------
  // utils javascript string extensions
  //------------------------------------

  String.prototype.isEmpty = function(str) {
    return ($.trim(this ).length < 1);
  }
  String.prototype.isNotEmpty = function(str) {
    return ($.trim(this ).length > 0);
  }

  String.prototype.endsWith = function(str) {
    return (this.match(str+"$")==str)
  }

  String.prototype.startsWith = function(str) {
    return (this.match("^"+str)==str)
  }

  String.prototype.substringBeforeLast = function(str) {
    return this.substring(0,this.lastIndexOf(str));
  }

  String.prototype.substringBeforeFirst = function(str) {
    var idx = this.indexOf(str);
    if(idx<0){
      return this;
    }
    return this.substring(0,idx);
  }

  String.prototype.substringAfterLast = function(str) {
    return this.substring(this.lastIndexOf(str)+1);
  }
  /**
   *
   * @param str
   * @return {String} if str not found return empty string
   */
  String.prototype.substringAfterFirst = function(str) {
    var idx = this.indexOf(str);
    if (idx<0){
      return "";
    }
    return this.substring(idx+str.length);
  }

  escapeDot=function(str){
    return str.replace(/\./g,"\\\.");
  }

  /**
   * select class:
   * * .popover-doc: activate popover with html:true and click trigger
   * * .tooltip-doc: active tooltip
   */
  activatePopoverDoc=function(){
    var mainContent=$("#main-content");
    mainContent.find(".popover-doc" ).popover({html: true, trigger: 'click'});
    /*mainContent.find(".popover-doc" ).on("click",function(){
      $(this).popover("show");
    });

    mainContent.find(".popover-doc" ).mouseover(function(){
      $(this).popover("destroy");
    });*/

    mainContent.find(".tooltip-doc" ).tooltip({html: true, trigger: 'hover'});
  }

  //------------------------------------
  // remote logging
  //------------------------------------
  JavascriptLog=function(loggerName,message){
    this.loggerName=loggerName;
    this.message=message;
  }

  remoteLogTrace=function(loggerName,message){
    var javascriptLog=new JavascriptLog(loggerName,message);
    remoteLog("trace",javascriptLog);
  }

  remoteLogDebug=function(loggerName,message){
    var javascriptLog=new JavascriptLog(loggerName,message);
    remoteLog("debug",javascriptLog);
  }

  remoteLogInfo=function(loggerName,message){
    var javascriptLog=new JavascriptLog(loggerName,message);
    remoteLog("info",javascriptLog);
  }

  remoteLogWarn=function(loggerName,message){
    var javascriptLog=new JavascriptLog(loggerName,message);
    remoteLog("warn",javascriptLog);
  }

  remoteLogError=function(loggerName,message){
    var javascriptLog=new JavascriptLog(loggerName,message);
    remoteLog("error",javascriptLog);
  }

  /**
   *
   * @param level trace/debug/info/warn/error
   * @param javascriptLog
   */
  remoteLog=function(level,javascriptLog){
    $.ajax("restServices/archivaUiServices/javascriptLogger/"+level,{
            type: "PUT",
            contentType: 'application/json',
            data: $.toJSON(javascriptLog)
           }
    );
  }

  //-----------------------------------------
  // extends jquery tmpl to support var def
  //-----------------------------------------

  $(function() {
    $.extend($.tmpl.tag, {
        "var": {
            open: "var $1;"
        }
    });
  });

});