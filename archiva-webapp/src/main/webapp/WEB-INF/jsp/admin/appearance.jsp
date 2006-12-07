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

<%@ taglib prefix="ww" uri="/webwork" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
  <title>Configure Appearance</title>
  <ww:head/>
</head>

<body>
<h1>Appearance</h1>

<div style="float: right">
  <a href="<ww:url action='editAppearance' />">Edit</a>
</div>
<h2>Company Details</h2>

<p>
  The logo in the top right of the screen is controlled by your selected 'company POM'.
</p>

<ww:set name="companyPom" value="companyPom"/>

<c:if test="${empty(companyPom)}">
  <p>
    You have not yet specified a company POM. <a href="<ww:url action='editAppearance' />">Select a Company POM</a>
  </p>
</c:if>

<c:if test="${!empty(companyPom)}">
  <p>
    Your selected company POM is below. If you would like to change the organization name, url or logo, you can
    <a href="<ww:url action='editCompanyPom'/>">edit the POM</a>.
  </p>

  <ww:set name="companyModel" value="companyModel"/>
  <table>
    <ww:label name="companyPom.groupId" label="Group ID"/>
    <ww:label name="companyPom.artifactId" label="Artifact ID"/>
    <c:if test="${companyModel != null}">
      <ww:label name="companyModel.version" label="Version"/>
    </c:if>
  </table>

  <div style="float: right">
    <a href="<ww:url action='editCompanyPom' />">Edit Company POM</a>
  </div>
  <h3>POM Information</h3>

  <c:choose>
    <c:when test="${companyModel != null}">
      <table>
        <tr>
          <th>Name</th>
          <td>${companyModel.organization.name}</td>
        </tr>
        <tr>
          <th>URL</th>
          <td><a href="${companyModel.organization.url}">
            <code>${companyModel.organization.url}</code>
          </a></td>
        </tr>
        <tr>
          <th>Logo URL</th>
          <td>
            <code>${companyModel.properties['organization.logo']}</code>
          </td>
        </tr>
      </table>
    </c:when>
    <c:otherwise>
      Company POM '${companyPom.groupId}:${companyPom.artifactId}' doesn't exist.
      <a href="<ww:url action='editCompanyPom' />">Create company POM</a>
    </c:otherwise>
  </c:choose>
</c:if>
</body>
</html>