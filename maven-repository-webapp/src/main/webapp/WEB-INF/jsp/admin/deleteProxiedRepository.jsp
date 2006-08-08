<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
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

  <h2>Delete Proxied Repository</h2>

  <blockquote>
    <strong><span class="statusFailed">WARNING:</span> This operation can not be undone.</strong>
  </blockquote>

  <ww:form method="post" action="deleteProxiedRepository" namespace="/admin" validate="true">
    <ww:hidden name="repoId" />
    <ww:radio list="#@java.util.LinkedHashMap@{
    'delete-contents' : 'Remove the repository and delete its contents from managed repositories',
    'delete-entry' : 'Remove the repository from the available list, but leave the contents in the managed repositories',
    'unmodified' : 'Leave the repository unmodified'}" name="operation" theme="repository-manager" />
    <ww:submit value="Go" />
  </ww:form>
</div>

</body>
</html>