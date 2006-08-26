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

  <h2>Add Synced Repository</h2>

  <ww:actionmessage />
  <ww:form method="post" action="addSyncedRepository" namespace="/admin" validate="true">
    <ww:select list="#@java.util.LinkedHashMap@{
      'rsync' : 'Rsync', 
      'svn' : 'Subversion',
      'cvs' : 'CVS',
      'file': 'Local File System'}" name="method" label="Method" />

    <ww:submit value="Continue" />
  </ww:form>
</div>

</body>
</html>