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
  // first redback then archiva
  // -- redback
  // load default
  loadAndParseFile("restServices/redbackServices/utilServices/getBundleResources", {cache:false, mode: 'map',encoding:'utf-8'});
  // load browser locale
  var browserLang = $.i18n.browserLang();
  var requestLang = $.urlParam('request_lang');
  if (requestLang) {
    browserLang=requestLang;
  }
  $.log("use browserLang:"+browserLang);
  loadAndParseFile("restServices/redbackServices/utilServices/getBundleResources?locale="+browserLang, {cache:false, mode: 'map',encoding:'utf-8'});
  // -- archiva
  // load default
  loadAndParseFile("restServices/archivaServices/commonServices/getI18nResources", {cache:false, mode: 'map',encoding:'utf-8'});
  // load browser locale
  var browserLang = $.i18n.browserLang();
  loadAndParseFile("restServices/archivaServices/commonServices/getI18nResources?locale="+browserLang, {cache:false, mode: 'map',encoding:'utf-8'});
});