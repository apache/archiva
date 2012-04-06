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

<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/redback/taglib-1.0" prefix="redback" %>

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="login.page.title"/></title>
</head>

<body onload="javascript:document.forms['login'].username.focus();">


<c:choose>
  <c:when test="${sessionScope.securitySession.authenticated != true}">
  
  <h2><s:text name="login.section.title"/></h2>

  <%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>
  
  
    <s:form action="login" namespace="/security" theme="xhtml" 
         id="loginForm" method="post" name="login" cssClass="security login">
      <s:textfield label="%{getText('username')}" name="username" size="30" />
      <s:password  label="%{getText('password')}" name="password" size="20" />
      <s:checkbox label="%{getText('login.remember.me')}" name="rememberMe" value="false" />
      <s:submit value="%{getText('login')}" method="login" id="loginSubmit"/>
      <s:submit value="%{getText('cancel')}" method="cancel" id="loginCancel" />
  </s:form>
<%-- TODO: Figure out how to auto-focus to first field --%>

<ul class="tips">
  <%--
  <li>
     Forgot your Username? 
     <s:url id="forgottenAccount" action="findAccount" />
     <s:a href="%{forgottenAccount}">Email me my account information.</s:a>
  </li>
    --%>
  <redback:isNotReadOnlyUserManager>
  <li>
     <s:text name="login.need.an.account"/>
     <s:url id="registerUrl" action="register" />
     <s:a id="registerLinkLoginPage" href="%{registerUrl}"><s:text name="login.register"/></s:a>
  </li>
  <li>
     <s:text name="login.forgot.your.password"/>
     <s:url id="forgottenPassword" action="passwordReset" />
     <s:a id="forgottenPasswordLink" href="%{forgottenPassword}"><s:text name="login.request.password.reset"/></s:a>
  </li>
  </redback:isNotReadOnlyUserManager>
</ul>
</c:when>
<c:otherwise>
  <p/>
	<s:text name="login.already.logged.in"/>
  <p/>
</c:otherwise>
</c:choose>
</body>
</s:i18n>
</html>
