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
$.log = (function(message) {
  if (typeof window.console != 'undefined' && typeof window.console.log != 'undefined') {
    console.log(message);
  } else {
    // do nothing no console
  }
});

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

screenChange=function(){
  $("#main-content").html("");
  clearUserMessages();
}

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