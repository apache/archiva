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

<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
  <title>Configure Appearance</title>
  <s:head/>
</head>

<body>
<h1>Appearance</h1>

<div style="float: right">
  <a href="<s:url action='editAppearance' />">Edit</a>
</div>
<h2>Organization Details</h2>

<p>
  The logo in the top right of the screen is controlled by the following settings.
  <a href="<s:url action='editAppearance' />">Change your appearance</a>
</p>

<h3>Organization Information</h3>
<table>
  <tr>
    <th>Name</th>
    <td>${organisationName}</td>
  </tr>
  <tr>
    <th>URL</th>
    <td><a href="${organisationUrl}">
      <code>${organisationUrl}</code>
    </a></td>
  </tr>
  <tr>
    <th>Logo URL</th>
    <td>
      <code>${organisationLogo}</code>
    </td>
  </tr>
  <c:if test="${!empty (organisationLogo)}">
    <tr>
      <th>&nbsp;</th>
      <td><img src="${organisationLogo}"
        title="${organisationName}" border="0" alt="" /></td>
    </tr>
  </c:if>
</table>
</body>
</html>
