/* 
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
define("archiva.cookie-information",function() {
CookieInformation=function(path,domain,secure,timeout,rememberMeEnabled){
    //private String path;
    this.path=path;

    //private String domain;
    this.domain=domain;

    //private String secure;
    this.secure=secure;

    //private String timeout;
    this.timeout=timeout;

    //private boolean rememberMeEnabled;
    this.rememberMeEnabled=rememberMeEnabled;
  }

  mapCookieInformation=function(data){
    if(!data){
      return new CookieInformation();
    }
    return new CookieInformation(data.path,data.domain,data.secure,data.timeout,data.rememberMeEnabled);
  }

});