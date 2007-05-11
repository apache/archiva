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

<c:choose>
  <c:when test="${mode == 'edit'}">
    <c:set var="addedit" value="Edit" />
    <c:set var="disableId" value="true" />
  </c:when>
  <c:otherwise>
    <c:set var="addedit" value="Add" />
    <c:set var="disableId" value="false" />
  </c:otherwise>
</c:choose>

<html>
<head>
  <title>Admin: ${addedit} Network Proxy</title>
  <ww:head/>
</head>

<body>

<h1>Admin: ${addedit} Network Proxy</h1>

<div id="contentArea">

  <h2>${addedit} Network Proxy</h2>

  <ww:actionerror/> 
  <ww:actionmessage/>
  
  <ww:form method="post" action="saveNetworkProxy" namespace="/admin">
    <ww:hidden name="mode"/>
    
    <ww:textfield name="proxy.id" label="Identifier" size="10" required="true"
      disabled="${disableId}"/>
    
    <%@ include file="/WEB-INF/jsp/admin/include/networkProxyForm.jspf" %>
    <ww:submit value="Save Network Proxy"/>
  </ww:form>

  <script type="text/javascript">
    document.getElementById("saveNetworkProxy_host").focus();
  </script>

</div>

</body>
</html>