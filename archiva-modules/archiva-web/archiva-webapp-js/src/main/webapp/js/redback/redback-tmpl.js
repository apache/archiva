/*
 * Copyright 2011 The Codehaus.
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
require(["text!redback/templates/user-edit.html", "text!redback/templates/user-grids.html", "text!redback/templates/login.html"
          , "text!redback/templates/register-form.html","text!redback/templates/password-change-form.html"
          ,"text!redback/templates/user-edit-form.html"],
    function(usercreate, usergrids, login,register,passwordchange,useredit) {


      $.tmpl( login, $.i18n.map ).appendTo("#html-fragments");
      $.tmpl( register, $.i18n.map ).appendTo("#html-fragments");
      $.tmpl( passwordchange, $.i18n.map ).appendTo("#html-fragments");
      $.tmpl( useredit, $.i18n.map ).appendTo("#html-fragments");
      // template loading
      $("#html-fragments").append(usercreate);
      $("#html-fragments").append(usergrids);

    }
);