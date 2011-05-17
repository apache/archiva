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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
<head>
  <title>Admin: Delete Managed Repository</title>
  <s:head/>
</head>

<body>

<h1>Admin: Delete Managed Repository</h1>

<%-- changed the structure of displaying errorMessages in order for them to be escaped. --%>
<s:if test="hasActionErrors()">
      <ul>
      <s:iterator value="actionErrors">
          <li><span class="errorMessage"><s:property escape="true" /></span></li>
      </s:iterator>
      </ul>
</s:if>

<div id="contentArea">

  <div class="warningbox">
    <p>
      <strong>WARNING: This operation can not be undone.</strong>
    </p>
  </div>
  
  <p>
    Are you sure you want to delete the following managed repository?
  </p>

  <%-- used c:out in displaying EL's so that they are escaped --%>
  <div class="infobox">
    <table class="infotable">
      <tr>
        <td>ID:</td>
        <td><code><c:out value="${repository.id}" /></code></td>
      </tr>
      <tr>
        <td>Name:</td>
        <td><c:out value="${repository.name}" /></td>
      </tr>
      <tr>
        <td>Directory:</td>
        <td><c:out value="${repository.location}" /></td>
      </tr>
    </table>
  </div>

  <s:form method="post" action="deleteRepository" namespace="/admin" validate="true" theme="simple">
    <s:hidden name="repoid"/>
    <s:token/>
    <div class="buttons">
      <s:submit value="Delete Configuration Only" method="deleteEntry" />
      <s:submit value="Delete Configuration and Contents" method="deleteContents" />
      <s:submit value="Cancel" method="execute"/>
    </div>
  </s:form>
</div>

</body>
</html>