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

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="password.page.title"/></title>
</head>

<body onload="javascript:document.forms['password'].existingPassword.focus();">

<h2><s:text name="password.section.title"/></h2>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

<s:form action="password" namespace="/security" theme="xhtml" 
         id="passwordForm" method="post" name="password" cssClass="security password">
  <c:if test="${provideExisting}">
    <s:password  label="%{getText('password.existing')}" name="existingPassword" size="20" required="true" />
  </c:if>
  <s:password  label="%{getText('password.new')}" name="newPassword" size="20" required="true" />
  <s:password  label="%{getText('password.new.confirm')}" name="newPasswordConfirm" size="20" required="true" />
  <s:submit value="%{getText('password.change')}" method="submit" />
  <s:submit value="%{getText('cancel')}" method="cancel" />
</s:form>

<ul class="tips">

</ul>

</body>
</s:i18n>
</html>
