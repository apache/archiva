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
  <title>Registration Page</title>
  <ww:head/>
</head>

<body>

<h1>Registration</h1>

<div id="contentArea">
  <div id="nameColumn">
    <ww:form action="register">
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
          <td>Full Name:</td>
          <td><ww:textfield name="fullName"/></td>
        </tr>
        <tr>
          <td>Email Address:</td>
          <td><ww:textfield name="email"/></td>
        </tr>
        <tr>
          <td><ww:submit name="Register"/></td>
          <td></td>
        </tr>
      </table>
    </ww:form>
  </div>
</div>

</body>
</html>
