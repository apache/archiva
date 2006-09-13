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
          <pss:ifAnyAuthorized permissions="edit-all-users,edit-user" resource="${username}">
            <ww:url id="userDetailsUrl" action="userDetails">
              <ww:param name="username">${sessionScope.SecuritySessionUser.username}</ww:param>
            </ww:url>
            <ww:a href="%{userDetailsUrl}">Edit details</ww:a>
          </pss:ifAnyAuthorized>
        </div>

        <h2>${sessionScope.SecuritySessionUser.fullName}</h2>

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

        <h2>Assigned Roles</h2>

        <table class="bodyTable">
          <ww:iterator id="role" value="assignedRoles">

            <tr class="a">
             <td>
               <em>${role.name}</em><br/>
             </td>
              <td>
                <pss:ifAuthorized permission="remove-roles">
                  <ww:url id="removeAssignedRoleUrl" action="removeRoleFromUser">
                    <ww:param name="principal">${username}</ww:param>
                    <ww:param name="roleName">${role.name}</ww:param>
                  </ww:url>
                  <ww:a href="%{removeAssignedRoleUrl}">Delete</ww:a>
                </pss:ifAuthorized>
              </td>
            </tr>
          </ww:iterator>
        </table>

        <h2>Grant Roles</h2>

        <pss:ifAuthorized permission="grant-roles">
          <table class="bodyTable">
            <ww:iterator id="role" value="availableRoles">
              <tr class="a">
                <td>
                  <em>${role.name}</em><br/>
                </td>
                <td>
                  <ww:url id="addRoleUrl" action="assignRoleToUser">
                    <ww:param name="principal">${username}</ww:param>
                    <ww:param name="roleName">${role.name}</ww:param>
                  </ww:url>
                  <ww:a href="%{addRoleUrl}">Add</ww:a>
                </td>
              </tr>
            </ww:iterator>
          </table>
        </pss:ifAuthorized>


           <%--
          <p>
            This following screen needs have the various roles worked into it.
          </p>

          <table class="bodyTable">
            <tr class="b">
              <td>
                <input type="radio" checked="checked"></input>

              </td>
              <td>Administrator</td>
              <td>
                <select>
                  <option>(Please Select)</option>
                  <option>System Administrator</option>
                  <option>User Administrator</option>
                </select>
              </td>
            </tr>
            <tr class="a">
              <td>
                <input type="radio"></input>
              </td>
              <td>Repository</td>

              <td>
                <select>
                  <option>(Please Select)</option>
                  <option>manager</option>
                  <option>obverser</option>
                </select>
                of
                <ww:select name="resourceName" list="resources" listKey="identifier" listValue="identifier" headerKey="" headerValue="(Please Select)"/>
              </td>
            </tr> --%>
                 <%--  add in for project level security
            <tr class="b">
              <td>
                <input type="radio"></input>
              </td>
              <td>Project</td>
              <td>
                <select>
                  <option>(Please Select)</option>

                  <option>manager</option>
                  <option>obverser</option>
                </select>
                of
                <select>
                  <option>(Please Select)</option>
                  <option>central</option>

                  <option>central-plugins</option>
                  <option>All repositories</option>
                </select>
                for project group
                <input type="text" name="projectExpression"></input>
                <br></br>
                <span style="font-size: x-small">
                  (eg org.apache.maven gives permissions on that group, and any sugroups)
                </span>

              </td>
            </tr>

            <tr class="a">
              <td></td>
              <td>
                <input type="submit" value="Add Role"></input>
              </td>
              <td></td>
            </tr>

          </table>
             --%>
      </div>
  </div>

      <div class="clear">
        <hr/>
      </div>


</body>
</html>