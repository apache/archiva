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
  <title><s:text name="request.password.reset.page.title"/></title>
</head>

<body onload="javascript:document.forms['passwordReset'].username.focus();">

<h2><s:text name="request.password.reset.section.title"/></h2>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

<s:form action="passwordReset" namespace="/security" theme="xhtml" 
         id="passwordResetForm" method="post" name="passwordReset" cssClass="security passwordReset">
  <s:textfield label="%{getText('username')}" name="username" size="30" required="true" />
  <s:submit value="%{getText('request.password.reset')}" method="reset" />
  <s:submit value="%{getText('cancel')}" method="cancel" />
</s:form>

</body>
</s:i18n>
</html>
