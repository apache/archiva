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
  <title>User Management - Find a User</title>
  <ww:head />
</head>

<body>


    <h1>User Management</h1>

    <div id="contentArea">
      <div id="searchBox">
        <ww:form action="userDetails">
          <p>
            <ww:textfield label="Find a user" name="user"/>
            <ww:submit value="Search"/>
          </p>
        </ww:form>
      </div>
    </div>



    <div class="clear">
      <hr/>
    </div>


</body>
</html>