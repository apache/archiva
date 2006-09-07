<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
  <title>Login Page</title>
  <ww:head/>
</head>

<body>

<h1>Login</h1>

<div id="contentArea">
  <div id="nameColumn">
    <ww:form action="login">
      <table>
        <tr>
          <td>Username:</td>
          <td><ww:textfield name="username"/></td>
        </tr>
        <tr>
          <td>Password:</td>
          <td><ww:password name="password"/></td>
        </tr>
        <tr>
          <td><ww:submit name="Login"/></td>
          <td></td>
        </tr>
      </table>
    </ww:form>
    <p>
      <ww:url id="registerUrl" action="register" namespace="/"/>

      New user? - <ww:a href="%{registerUrl}">Register!</ww:a>
    </p>
  </div>
</div>

</body>
</html>
