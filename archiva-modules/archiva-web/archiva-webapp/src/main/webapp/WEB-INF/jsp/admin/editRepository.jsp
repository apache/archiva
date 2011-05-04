<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="s" uri="/struts-tags" %>

<html>
<head>
  <title>Admin: Edit Managed Repository</title>
  <s:head/>
</head>

<body>

<h1>Admin: Edit Managed Repository</h1>

<%-- changed the structure of displaying errorMessages & actionMessages in order for them to be escaped. --%>
<s:if test="hasActionErrors()">
      <ul>
      <s:iterator value="actionErrors">
          <li><span class="errorMessage"><s:property escape="true" /></span></li>
      </s:iterator>
      </ul>
</s:if>

<div id="contentArea">

  <s:if test="hasActionMessages()">
      <ul>
      <s:iterator value="actionMessages">
          <li><span class="actionMessage"><s:property escape="true" /></span></li>
      </s:iterator>
      </ul>
  </s:if>
  <s:form method="post" action="editRepository!commit" namespace="/admin" validate="false">
    <s:hidden name="repository.id"/>
    <s:label label="ID" name="repository.id" />
    <%@ include file="/WEB-INF/jsp/admin/include/repositoryForm.jspf" %>

    <c:set var="stats" value="${stagingRepository == null}"/>
     <jsp:useBean id="stats" type="java.lang.Boolean" scope="page"/>
     <c:if
      test='<%= !stats.booleanValue() %>'>
      <s:checkbox name="stageNeeded" value="true" label="Create stage repository"/>
     </c:if>
     <c:if
      test='<%= stats.booleanValue() %>'>
      <s:checkbox id="stageNeeded" name="stageNeeded" value="false" label="Create stage repository"/>
     </c:if>
    <s:submit value="Update Repository"/>
  </s:form>

  <script type="text/javascript">
    document.getElementById("editRepository_repository_name").focus();
  </script>

</div>

</body>
</html>