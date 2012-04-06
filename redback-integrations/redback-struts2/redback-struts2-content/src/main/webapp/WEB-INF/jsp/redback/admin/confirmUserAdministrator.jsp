<%--
  ~ Copyright 2010 The Codehaus.
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
  <title><s:text name="user.edit.confirm.page.title"/></title>
</head>

<body>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

<h2><s:text name="user.edit.confirm.section.title"/></h2>

<redback:ifAuthorized permission="user-management-user-edit" resource="${user.username}">

  <s:form action="useredit" namespace="/security" theme="xhtml"
         id="userEditForm" method="post" cssClass="security userEdit">
    <redback:isNotReadOnlyUserManager>
      <p>
        You must re-confirm your password to proceed with the request to
        <strong>edit the account information</strong> for user: <strong>${user.username}</strong>
      </p>

      <s:password label="%{getText('user.admin.password')}" name="userAdminPassword" size="20" required="true"/>
      <s:hidden label="Username"    name="username" />
      <s:hidden name="user.username" value="%{user.username}"/>
      <s:hidden name="user.fullName" value="%{user.fullName}"/>
      <s:hidden name="user.email" value="%{user.email}"/>
      <s:hidden name="user.password" value="%{user.password}"/>
      <s:hidden name="user.confirmPassword" value="%{user.confirmPassword}"/>
      <s:hidden name="user.timestampAccountCreation" value="%{user.timestampAccountCreation}"/>
      <s:hidden name="user.timestampLastLogin" value="%{user.timestampLastLogin}"/>
      <s:hidden name="user.timestampLastPasswordChange" value="%{user.timestampLastPasswordChange}"/>
      <s:hidden name="user.locked" value="%{user.locked}"/>
      <s:hidden name="user.passwordChangeRequired" value="%{user.passwordChangeRequired}"/>
      <s:hidden name="method:confirmAdminPassword" value="Submit"/>
      <s:submit id="confirmUserAdminSubmit" value="%{getText('submit')}" method="confirmAdminPassword" />
      <s:submit id="cancelUserAdminSubmit" value="%{getText('cancel')}" method="cancel" />
    </redback:isNotReadOnlyUserManager>
  </s:form>
</redback:ifAuthorized>

</body>
</s:i18n>
</html>
