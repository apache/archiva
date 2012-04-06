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

<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="redback" uri="/redback/taglib-1.0"%>

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="account.details.page.title"/></title>
</head>

<body>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

<h2><s:text name="account.details.section.title"/></h2>
   
<s:form action="account" namespace="/security" theme="xhtml"
         id="registerForm" method="post" name="register" cssClass="security register">     
  <%@ include file="/WEB-INF/jsp/redback/include/userCredentials.jsp" %>
  <redback:isReadOnlyUserManager>
  	<s:submit value="%{getText('goback')}" method="cancel" />
  </redback:isReadOnlyUserManager>
  <redback:isNotReadOnlyUserManager>
    <s:submit value="%{getText('submit')}" method="submit" />
    <s:submit value="%{getText('cancel')}" method="cancel" />
  </redback:isNotReadOnlyUserManager>
</s:form>

</body>
</s:i18n>
</html>
