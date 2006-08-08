<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>

<html>
<head>
  <title>Configuration</title>
  <ww:head />
</head>

<body>

<h1>Configuration</h1>

<div id="contentArea">

  <h2>Add Proxied Repository</h2>

  <ww:actionmessage />
  <ww:form method="post" action="addProxiedRepository" namespace="/admin" validate="true">
    <ww:textfield name="id" label="Identifier" size="10" />
    <ww:textfield name="name" label="Name" size="50" />
    <ww:textfield name="url" label="URL" size="50" />
    <ww:select list="#@java.util.LinkedHashMap@{'default' : 'Maven 2.x Repository', 'legacy' : 'Maven 1.x Repository'}"
               name="layout" label="Type" />
    <ww:select name="snapshotsPolicy" label="Snapshots" list="#@java.util.LinkedHashMap@{
        'disabled' : 'Disabled',
        'daily' : 'Enabled, updated daily',
        'hourly' : 'Enabled, updated hourly',
        'never' : 'Enabled, never updated',
        'interval' : 'Enabled, updated on given interval'}" />
    <ww:textfield label="Snapshot update interval" name="snapshotsInterval" size="4" />
    <ww:select name="releasesPolicy" label="Releases" list="#@java.util.LinkedHashMap@{
        'disabled' : 'Disabled',
        'daily' : 'Enabled, updated daily',
        'hourly' : 'Enabled, updated hourly',
        'never' : 'Enabled, never updated',
        'interval' : 'Enabled, updated on given interval'}" />
    <ww:textfield label="Release update interval" name="releasesInterval" size="4" />
    <ww:select list="configuration.repositoriesMap" name="managedRepository" label="Proxied through"/>

    <ww:submit value="Add Repository" />
  </ww:form>
</div>

</body>
</html>