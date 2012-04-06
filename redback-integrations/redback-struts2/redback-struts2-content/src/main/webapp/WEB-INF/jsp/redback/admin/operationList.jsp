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

<html>
<s:i18n name="org.codehaus.plexus.redback.struts2.default">
<head>
  <title><s:text name="operation.list.page.title"/></title>
</head>

<body>

<%@ include file="/WEB-INF/jsp/redback/include/rbacListNavigation.jsp" %>

<h2><s:text name="operation.list.section.title"/></h2>

<%@ include file="/WEB-INF/jsp/redback/include/formValidationResults.jsp" %>

<s:form action="operations!remove" method="post" theme="simple">

  <table>

    <c:choose>
      <c:when test="${!empty allOperations}">
        <thead>
          <tr>
            <th>&nbsp;</th>
            <th><s:text name="name"/></th>
            <th><s:text name="description"/></th>
          </tr>
        </thead>
        
        <c:forEach var="operation" items="${allOperations}">
          <tr>
            <td>
              <s:checkbox name="selectedOperations" fieldValue="%{operation.name}" />
            </td>
            <td>
              <s:url id="operationUrl" action="operation-edit">
                <s:param name="operationName">${operation.name}</s:param>
              </s:url>
              <s:a href="%{operationUrl}"><c:out value="${operation.name}" /></s:a>
            </td>
            <td>
              <c:out value="${operation.description}" />
            </td>
          </tr>
        </c:forEach>
      </c:when>
      <c:otherwise>
        <p><em><s:text name="operation.list.no.operations.available"/></em></p>
      </c:otherwise>
    </c:choose>
    
    <tr>
      <td colspan="3">
        <s:submit value="%{getText('remove.selected.roles')}" />
      </td>
    </tr>

  </table>
  
</s:form>

</body>
</s:i18n>
</html>
