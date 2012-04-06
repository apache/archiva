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
  <title><s:text name="user.edit.page.title"/></title>
</head>

<body>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

<h2><s:text name="user.edit.section.title"/></h2>

  <redback:ifAuthorized permission="user-management-user-edit" resource="${user.username}">
    <s:form action="useredit" namespace="/security" theme="xhtml"
           id="userEditForm" method="post" name="useredit" cssClass="security userEdit">
      <%@ include file="/WEB-INF/jsp/redback/include/userCredentials.jsp" %>
      <redback:isNotReadOnlyUserManager>
        <s:checkbox label="%{getText('user.edit.locked.user')}" name="user.locked" />
        <s:checkbox label="%{getText('user.edit.force.user.change.password')}" name="user.passwordChangeRequired" />
        <s:hidden label="Username"    name="username" />
        <s:submit value="%{getText('update')}" method="submit" />
        <s:submit value="%{getText('cancel')}" method="cancel" />
      </redback:isNotReadOnlyUserManager>
    </s:form>

    <c:if test="${ emailValidationRequired}">
    <p>
      <s:form action="register!resendRegistrationEmail" namespace="/security" theme="xhtml"
           id="resendRegistationForm" method="post" name="resendRegistration" cssClass="security userEdit">
           <s:hidden label="Username"    name="username" />
           <s:submit value="Resend Validation" method="submit" />
      </s:form>
    </p>
    </c:if>
  </redback:ifAuthorized>

  <redback:ifAuthorized permission="user-management-user-role" resource="${user.username}">
    <c:if test="${!empty effectivelyAssignedRoles}">
      <h3><s:text name="effective.roles"/></h3>

      <ul>
        <s:iterator id="role" value="effectivelyAssignedRoles">
          <li>${role.name}</li>
        </s:iterator>
      </ul>

    </c:if>

    <s:url id="assignmentUrl" action="assignments" includeParams="none">
      <s:param name="username">${user.username}</s:param>
    </s:url>
    <s:a href="%{assignmentUrl}"><s:text name="user.edit.roles"/></s:a>
  </redback:ifAuthorized>
</body>
</s:i18n>
</html>
