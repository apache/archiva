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
define("redback-templates",["text!templates/redback/user-edit.html",
          "text!templates/redback/user-grids.html",
          "text!templates/redback/login.html",
          "text!templates/redback/register-form.html",
          "text!templates/redback/password-change-form.html",
          "text!templates/redback/user-edit-form.html",
          "text!templates/redback/roles-tmpl.html",
          "jquery_tmpl","utils"],
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