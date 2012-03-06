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
require(["text!js/redback/templates/user-edit.html?"+appendTemplateUrl(),
          "text!js/redback/templates/user-grids.html?"+appendTemplateUrl(),
          "text!js/redback/templates/login.html?"+appendTemplateUrl(),
          "text!js/redback/templates/register-form.html?"+appendTemplateUrl(),
          "text!js/redback/templates/password-change-form.html?"+appendTemplateUrl(),
          "text!js/redback/templates/user-edit-form.html?"+appendTemplateUrl(),
          "text!js/redback/templates/roles-tmpl.html?"+appendTemplateUrl()],
    function(useredit, usergrids, login,register,passwordchange,usereditform,roles) {

      var htmlFragment=$("#html-fragments");

      // template loading

      htmlFragment.append(useredit);

      htmlFragment.append(usergrids);

      $.tmpl(login).appendTo("#html-fragments");

      $.tmpl(register).appendTo("#html-fragments");

      $.tmpl(passwordchange).appendTo("#html-fragments");

      $.tmpl(usereditform).appendTo("#html-fragments");

      htmlFragment.append(roles);

      $.log("redback-tmpl.js loaded");
    }
);