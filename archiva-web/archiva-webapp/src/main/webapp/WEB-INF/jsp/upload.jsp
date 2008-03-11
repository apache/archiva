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

<%-- http://www.opensymphony.com/webwork/wikidocs/File%20Upload%20Interceptor.html --%>

<%@ taglib prefix="ww" uri="/webwork" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="archiva" uri="http://maven.apache.org/archiva" %>
<%@ taglib prefix="redback" uri="http://plexus.codehaus.org/redback/taglib-1.0" %>

<html>
<head>
  <title>Upload Artifact</title>
  <ww:head/>
</head>

<body>

<h1>Upload Artifact</h1>
<div id="contentArea">
  <ww:form action="doUpload" method="post" enctype="multipart/form-data">
    <p>groupId: <input type="text" name="groupId" size="50" value="" id="groupId"/></p>
    <p>artifactId: <input type="text" name="artifactId" size="50" value="" id="artifactId"/></p>
    <p>version: <input type="text" name="version" size="50" value="" id="version"/></p>
    <p>packaging: <input type="text" name="packaging" size="50" value="" id="packaging"/></p>
    <p>classifier: <input type="text" name="classifier" size="50" value="" id="classifier"/></p>
    <p>repositoryId: <input type="text" name="repositoryId" size="50" value="" id="repositoryId"/></p>
    <p>
      <ww:file name="upload" label="File"/>
      <ww:submit/>
    </p>
  </ww:form>
</div>

</body>
</html>
