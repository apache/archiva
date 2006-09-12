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

<div id="contentArea">
  <div id="searchBox">
    <div style="float: right">
      <a href="#">Forgotten your Password?</a>

    </div>

    <p>
      <ww:actionmessage/>
      <ww:actionerror/>
    </p>

    <h2>Login</h2>
    <ww:form action="login">
      <table class="bodyTable">
        <tr class="a">
          <th>
            Username
          </th>
          <td>
            <ww:textfield name="username" size="30"/>
          </td>
        </tr>
        <tr class="b">
          <th>
            Password
          </th>
          <td>
            <ww:password name="password" size="20"/>

          </td>
        </tr>
        <tr class="a">
          <td></td>
          <td>
            <ww:submit value="Login"/>
          </td>
        </tr>
      </table>

    </ww:form>
  </div>
</div>


<div class="clear">
  <hr/>
</div>

</body>

</html>
