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
  <title><s:text name="role.summary.page.title"/></title>
</head>

<body>

<%@ include file="/WEB-INF/jsp/redback/include/rbacListNavigation.jsp" %>

<h2><s:text name="role.summary.section.title"/></h2>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

    <ul>
      <s:iterator id="role" value="allRoles">
        <li><s:text name="role"/>: <c:out value="${role.name}"/></li>
        <ul>
        <s:iterator id="permission" value="#role.permissions">
          <li>P[<c:out value="${permission.name}"/>] (<c:out value="${permission.operation.name}"/>, <c:out value="${permission.resource.identifier}"/>)</li>
        </s:iterator>
        </ul>
      </s:iterator>
    </ul>

</body>
</s:i18n>
</html>
