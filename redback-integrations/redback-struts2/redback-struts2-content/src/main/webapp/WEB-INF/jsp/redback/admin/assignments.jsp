<%--
  ~ Copyright 2005-2006 The Codehaus.
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

<%@ taglib prefix="s" uri="/struts-tags"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="assignments.page.title"/></title>
</head>

<body>

<h2><s:text name="assignments.section.title"/></h2>

    <div class="axial">
      <table border="1" cellspacing="2" cellpadding="3" width="100%">
        <s:label label="%{getText('username')}" name="principal"/>
        <s:label label="%{getText('full.name')}" name="user.fullName"/>
        <s:label label="%{getText('email')}" name="user.email"/>
      </table>
    </div>

<!--<h3><s:text name="assignments.available.roles"/></h3>-->

    <s:form action="addRolesToUser" namespace="/security" name="addRoles">
      <s:hidden name="principal"/>
      <s:hidden name="addRolesButton" value="true"/>
      <s:token/>

      <s:iterator id="application" value="applicationRoleDetails">

        <h3><c:out value="${application.name}" /></h3>
        <c:if test="${!empty application.description}">
          <p><i><c:out value="${application.description}" /></i></p>
        </c:if>

        <c:if test="${!empty assignedRoles}">

         <h5><s:text name="assignments.assigned.roles"/></h5>
         <table>
         <s:iterator id="assignedRole" value="assignedRoles">
           <s:checkbox label="%{assignedRole}" name="addNDSelectedRoles" fieldValue="%{assignedRole}"/>
         </s:iterator>
         </table>

        </c:if>
        <c:if test="${!empty availableRoles}">
          <h5><s:text name="assignments.available.roles"/></h5>
          <table>
          <s:iterator id="availableRole" value="availableRoles">
            <s:checkbox label="%{availableRole}" name="addNDSelectedRoles" value="false" fieldValue="%{availableRole}"/>
          </s:iterator>
          </table>
        </c:if>

        <c:if test="${!empty table}">
          <h5><s:text name="assignments.resource.roles"/></h5>
          <table>
            <tr>
              <td></td>
              <s:iterator id="column" value="tableHeader">
                <td>${column.namePrefix}</td>
              </s:iterator>
            </tr>

            <c:forEach var="row" items="${table}">
              <tr>
                <c:forEach var="column" items="${row}">

                  <c:choose>
                    <c:when test="${column.label}">
                      <td>${column.name}</td>
                    </c:when>
                    <c:when test="${column.assigned}">
                      <td>
                        <center>
                          <input type="checkbox" name="addDSelectedRoles" value="${column.name}" checked="checked"/>
                        </center>
                      </td>
                    </c:when>
                    <c:when test="${column.effectivelyAssigned}">
                      <td>
                        <center>
                          <input type="checkbox" name="addDSelectedRoles" value="${column.name}" disabled="disabled"/>
                        </center>
                      </td>
                    </c:when>
                    <c:otherwise>
                      <td>
                        <center>
                          <input type="checkbox" name="addDSelectedRoles" value="${column.name}"/>
                        </center>
                      </td>
                     </c:otherwise>
                   </c:choose>

                </c:forEach>
              </tr>
            </c:forEach>
          </table>
        </c:if>
      </s:iterator>
   <%--
      <h4>Global Roles</h4>
      <s:checkboxlist list="nondynamicroles" name="addNDSelectedRoles" value="NDRoles" theme="redback"/>
      <br/>

      <h4>Resource Roles</h4>
      <c:choose>
        <c:when test="${!empty dynamicroles}">
          <c:set var="numtemplates" value="0"/>
          <table border="1">
           <tr>
             <td>&nbsp</td>
             <s:iterator id="template" value="templates">
      	       <td>${template.namePrefix}</td>
      	       <c:set var="numtemplates" value="${numtemplates + 1}"/>
              </s:iterator>
           </tr>
           <tr>
             <c:set var="count" value="0"/>
             <s:iterator id="dynamicrole" value="dynamicroles" status="row_status">
               <c:if test="${count == 0}">
                 <td>${dynamicrole.resource}</td>
               </c:if>
               <c:set var="chkbx" value="<input type='checkbox' name='addDSelectedRoles' value='${dynamicrole.name}'/>"/>
               <s:iterator id="drole" value="DRoles">
                 <c:if test="${(drole == dynamicrole.name)}">
                   <c:set var="chkbx" value="<input type='checkbox' name='addDSelectedRoles' value='${dynamicrole.name}' checked='yes'/>"/>
                 </c:if>
               </s:iterator>
               <td><center>${chkbx}</center></td>
               <c:set var="count" value="${count + 1}"/>
               <c:if test="${count == numtemplates}">
                 <c:choose>
                   <c:when test="${row_status.last}">
                     </tr>
                   </c:when>
                   <c:otherwise>
                     </tr><tr>
                   </c:otherwise>
                 </c:choose>
                 <c:set var="count" value="0"/>
               </c:if>
             </s:iterator>
          </table>
        </c:when>
        <c:otherwise>
          <p><em><s:text name="assignments.no.roles.to.grant"/></em></p>
        </c:otherwise>
      </c:choose>
--%>
      <br/>
      <s:submit value="%{getText('assignments.submit')}" name="submitRolesButton" theme="simple" />
      <br/>
      <s:reset type="button" value="%{getText('assignments.reset')}" name="resetRolesButton" theme="simple" />
    </s:form>

</body>
</s:i18n>
</html>
