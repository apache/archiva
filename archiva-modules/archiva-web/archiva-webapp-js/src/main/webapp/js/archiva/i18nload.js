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
  // load i18n resources from rest call

  var browserLang = $.i18n.browserLang();
  var requestLang = $.urlParam('request_lang');
  if (requestLang) {
    browserLang=requestLang;
  }
  $.log("use browserLang:"+browserLang);
  // -- archiva
  // load default
  //loadAndParseFile("restServices/archivaServices/commonServices/getAllI18nResources", {cache:false, mode: 'map',encoding:'utf-8'});
  //if (browserLang!='en'){
    var options = {
      cache:false,
      mode: 'map',
      encoding:'utf-8'
    };
    loadAndParseFile("restServices/archivaServices/commonServices/getAllI18nResources?locale="+browserLang,options );
  //}
});