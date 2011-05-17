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
  <title>Admin: Delete Proxy Connector</title>
  <s:head/>
</head>

<body>

<h1>Admin: Delete Proxy Connector</h1>

<s:actionerror/>

<div id="contentArea">

  <h2>Delete Proxy Connector</h2>

  <blockquote>
    <strong><span class="statusFailed">WARNING:</span> This operation can not be undone.</strong>
  </blockquote>

  <p>
    Are you sure you want to delete proxy connector <code>[ ${source} , ${target} ]</code> ?
  </p>

  <s:form method="post" action="deleteProxyConnector!delete" namespace="/admin" validate="true">
    <s:hidden name="target"/>
    <s:hidden name="source"/>
    <s:token/>
    <s:submit value="Delete"/>
  </s:form>
</div>

</body>
</html>