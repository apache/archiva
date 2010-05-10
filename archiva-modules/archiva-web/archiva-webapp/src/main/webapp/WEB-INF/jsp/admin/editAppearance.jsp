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
  <title>Configure Appearance</title>
  <s:head/>
</head>

<body>
<h1>Appearance</h1>

<h2>Organization Details</h2>

<p>
  Enter the details of your organization below.
</p>

<s:set name="editOrganisationInfo" value="editOrganisationInfo"/>
<s:actionmessage/>
<s:form method="post" action="saveAppearance" namespace="/admin" validate="true" theme="xhtml">
    <s:textfield name="organisationName" value="%{#attr.organisationName}" label="Name" size="50"  />
    <s:textfield name="organisationUrl" value="%{#attr.organisationUrl}" label="URL" size="50"/>
    <s:textfield name="organisationLogo" value="%{#attr.organisationLogo}" label="Logo URL" size="50" />
  <s:submit value="Save"/>
</s:form>
</body>

</html>
