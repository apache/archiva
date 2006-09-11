<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
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
  <title>Configuration</title>
  <ww:head/>
</head>

<body>

<h1>Role Modification</h1>

<div id="contentArea">

<ww:actionerror/>
<ww:form action="saveRole" method="post">
  <ww:hidden name="roleName"/>

  <ww:textfield label="name" name="name"/> <br/>
  <ww:textfield label="description" name="description"/> <br/>
  <ww:checkbox label="assignable?" name="assignable"/><br/>
  <br/>
  Currently Assigned Permissions:<br/>
  <ww:iterator id="permission" value="permissions">
    <ww:url id="removeAssignedPermissionUrl" action="removeAssignedPermission">
      <ww:param name="roleName" value="roleName"/>
      <ww:param name="removePermissionName">${permission.name}</ww:param>
    </ww:url>
    ${permission.name} | <ww:a href="%{removeAssignedPermissionUrl}">remove</ww:a><br/>
  </ww:iterator>
  <br/>
  <ww:select label="add new permission" name="assignPermissionName" list="assignablePermissions"  listKey="name" listValue="name" emptyOption="true"/><br/>
  <br/>
  Currently Assigned Roles:<br/>
  <ww:iterator id="arole" value="childRoles.roles">
    <ww:url id="removeAssignedRoleUrl" action="removeAssignedRole">
      <ww:param name="roleName" value="roleName"/>
      <ww:param name="removeRoleName" value="${arole.name}"/>
    </ww:url>
    ${arole.name} | <ww:a href="%{removeAssignedRoleUrl}">remove</ww:a><br/>
  </ww:iterator>
  <br/>
  <ww:select label="add sub role" name="assignedRoleName" list="assignableRoles" listKey="name" listValue="name" emptyOption="true"/><br/>

  <p>
    <ww:submit/>
  </p>
</ww:form>
   </div>
</body>
</html>