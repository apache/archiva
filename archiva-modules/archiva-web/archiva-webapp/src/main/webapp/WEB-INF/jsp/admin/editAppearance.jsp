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
  <title>Configure Appearance</title>
  <ww:head/>
</head>

<body>
<h1>Appearance</h1>

<h2>Organisation Details</h2>

<p>
  Enter the details of the company super POM below. If it exists, the organization name, URL and logo will be read
  from it.
</p>

<ww:actionmessage/>
<ww:form method="post" action="editAppearance" namespace="/admin" validate="true" theme="xhtml">
  <ww:textfield name="editAppearance.organisationName" label="Organisation Name"/>
  <ww:textfield name="editAppearance.organisationUrl" label="Organisation Url"/>
  <ww:textfield name="editAppearance.organisationLogo" label="Orgnaisation Logo"/>
  <ww:submit value="Save"/>
</ww:form>
</body>

</html>