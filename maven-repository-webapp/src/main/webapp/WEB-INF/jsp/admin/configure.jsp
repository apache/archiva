<%@ taglib prefix="ww" uri="/webwork" %>
<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
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

<html>
<head>
  <title>Configuration</title>
  <ww:head />
</head>

<body>

<h1>Configuration</h1>

<div id="contentArea">
  <div id="searchBox">
    <ww:actionmessage />
    <ww:form method="post" action="saveConfiguration" namespace="/admin" validate="true">
      <ww:textfield name="repositoryDirectory" label="Repository Directory" size="100" />
      <ww:textfield name="indexerCronExpression" label="Indexing Cron Expression" />
      <ww:submit value="Save Configuration" />
    </ww:form>
  </div>
</div>

</body>
</html>