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
<html>
<head>
  <title>User Management - User Details</title>
  <ww:head />
</head>

<body>

    <div id="contentArea">
      <div id="searchBox">
        <div style="float: right">

        </div>

        <h2>Modify User Details - ${username}</h2>

        <ww:form action="userDetails" method="post">
          <ww:textfield label="Full Name" name="fullName"/>
          <ww:textfield label="Email Address" name="email"/>

          <ww:checkbox label="Account Locked" name="locked"/>

          <ww:submit/>
        </ww:form>
      </div>
    </div>



      <div class="clear">
        <hr/>
      </div>

</body>
</html>