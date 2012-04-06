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
<%@ taglib prefix="redback" uri="/redback/taglib-1.0"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="role.create.page.title"/></title>
</head>

<body>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

<h2><s:text name="role.create.section.title"/></h2>

<s:form action="rolecreate!submit" method="post" theme="xhtml"
         name="roleCreateForm" cssClass="securiy rolecreate">
  <s:token/>
  <s:textfield label="%{getText('role.name')}" name="roleName" />
  <s:textfield label="%{getText('role.description')}" name="description" maxlength="255"/>
  <tr>
    <td valign="top"><s:text name="permissions"/></td>
    <td>
    
      <table cellspacing="0" cellpadding="2" class="permission">
        <thead>
        <tr>
          <th><s:text name="name"/></th>
          <th><s:text name="role.create.operation"/></th>
          <th><s:text name="role.create.resource"/></th>
        </tr>
        </thead>
      <c:choose>
        <c:when test="${!empty permissions}">
          <c:forEach var="permission" varStatus="loop" items="${permissions}">
            <tr>
              <td>
                <input type="text" name="permissions(${loop.index}).name"
                  value="${permission.name}" />
              </td>
              <td>
                <input type="text" name="permissions(${loop.index}).operationName"
                  value="${permission.operationName}" />
              </td>
              <td>
                <input type="text" name="permissions(${loop.index}).resourceIdentifier"
                  value="${permission.resourceIdentifier}" />
              </td>
            </tr>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <em><s:text name="role.create.no.permissions.defined"/></em>
        </c:otherwise>
      </c:choose>
      
      <tr class="addPermission">
        <td>
          <s:textfield name="addpermission.name" theme="simple"/>
        </td>
        <td>
          <s:textfield name="addpermission.operationName" theme="simple" />
        </td>
        <td>
          <s:textfield name="addpermission.resourceIdentifier" theme="simple" />
        </td>
        <td>
          <s:submit value="%{getText('role.create.add.permission')}" 
                     onclick="setSubmitMode('addPermission')" />
        </td>
      </tr>
      </table>
      
    </td>
  </tr>
  <s:hidden name="submitMode" value="normal" />
  <s:submit value="%{getText('submit')}" onclick="setSubmitMode('normal')" />
</s:form>

<script language="javascript">
  function setSubmitMode(mode)
  {
    document.forms["roleCreateForm"].submitMode.value = mode;
  }
</script>

</body>
</s:i18n>
</html>
