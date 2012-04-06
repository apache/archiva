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
  <title><s:text name="role.list.page.title"/></title>
</head>

<body>

<!-- %@ include file="/WEB-INF/jsp/redback/include/rbacListNavigation.jsp" % -->

<h2><s:text name="role.list.section.title"/></h2>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

  <table width="100%">

    <c:choose>
      <c:when test="${!empty allRoles}">
        <thead>
          <tr>
            <th width="49%"><s:text name="role.name"/></th>
            <th width="49%"><s:text name="role.description"/></th>
          </tr>
        </thead>
        
        <c:forEach var="role" items="${allRoles}">
          <tr>
            <td>
              <s:url id="roleUrl" action="role">
                <s:param name="name">${role.name}</s:param>
              </s:url>
              <s:a href="%{roleUrl}"><c:out value="${role.name}" /></s:a>
            </td>
            <td>
              <c:out value="${role.description}" />
            </td>
          </tr>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <p><em><s:text name="role.list.no.roles.available"/></em></p>
      </c:otherwise>
    </c:choose>
  </table>

</body>
</s:i18n>
</html>
