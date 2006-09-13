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
<%@ taglib prefix="pss" uri="plexusSecuritySystem" %>
<html>
<head>
  <title>User Management</title>
  <ww:head />
</head>

<body>

    <div id="contentArea">
      <div id="searchBox">
        <div style="float: right">
          <%-- add this back in when the functionality works, or when we move to the plexus-user-management pages
          <pss:ifAnyAuthorized permissions="edit-all-users,edit-user" resource="${username}">
            <ww:url id="userDetailsUrl" action="userDetails" method="input">
              <ww:param name="username">${sessionScope.SecuritySessionUser.username}</ww:param>
            </ww:url>
            <ww:a href="%{userDetailsUrl}">Edit details</ww:a>
          </pss:ifAnyAuthorized>
          --%>
        </div>

        <h2>${fullName}</h2>

        <table class="bodyTable">
          <tr class="a">
            <th>Username</th>

            <td>${username}</td>
          </tr>
          <tr class="b">
            <th>Email</th>
            <td>${email}</td>
          </tr>
        </table>

        <h2>Currently Assigned Roles</h2>

        <table class="bodyTable">
          <ww:iterator id="role" value="assignedRoles">
            <tr class="a">
             <td>
               <em>${role.name}</em><br/>
             </td>
            </tr>
          </ww:iterator>
        </table>


        <pss:ifAnyAuthorized permissions="grant-roles,remove-roles">
          <h2>Role Management</h2>

          <pss:ifAuthorized permission="grant-roles">
            <h3>Grant</h3>
            <ww:form action="assignRoleToUser" method="post">
              <ww:hidden name="principal" value="${username}"/>
              <ww:hidden name="username" value="${username}"/>
              <ww:radio name="roleName" list="availableRoles" listKey="name" listValue="name" labelposition="left"/>
              <ww:submit value="Grant"/>
            </ww:form>
          </pss:ifAuthorized>

          <pss:ifAuthorized permission="remove-roles">
            <h3>Remove</h3>
            <ww:form action="removeRoleFromUser" method="post">
              <ww:hidden name="principal" value="${username}"/>
              <ww:hidden name="username" value="${username}"/>
              <ww:radio name="roleName" list="assignedRoles" listKey="name" listValue="name" labelposition="left"/>
              <ww:submit value="Remove"/>
            </ww:form>
          </pss:ifAuthorized>
        </pss:ifAnyAuthorized>
      </div>
    </div>

    <div class="clear">
      <hr/>
    </div>


</body>
</html>