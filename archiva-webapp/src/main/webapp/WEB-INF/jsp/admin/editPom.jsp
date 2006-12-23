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
<html>
<head>
  <title>Edit Company POM</title>
  <ww:head/>
</head>

<body>
<h1>Company POM</h1>

<ww:actionmessage/>
<ww:form method="post" action="saveCompanyPom" namespace="/admin" validate="true" theme="xhtml">
  <ww:label name="companyModel.groupId" label="Group ID"/>
  <ww:label name="companyModel.artifactId" label="Artifact ID"/>
  <tr>
    <td>Version</td>
    <td>
      <ww:property value="companyModel.version"/>
      <i>(The version will automatically be incremented when you save this form)</i>
    </td>
  </tr>
  <tr>
    <td></td>
    <td><h2>Organization</h2></td>
  </tr>
  <ww:textfield name="companyModel.organization.name" size="40" label="Name"/>
  <ww:textfield name="companyModel.organization.url" size="70" label="URL"/>
  <%-- TODO: how to get it to be a string, not a String[]? --%>
  <ww:textfield name="companyModel.properties['organization.logo']" size="70" label="Logo URL"/>
  <ww:submit value="Save"/>
</ww:form>

</body>
</html>