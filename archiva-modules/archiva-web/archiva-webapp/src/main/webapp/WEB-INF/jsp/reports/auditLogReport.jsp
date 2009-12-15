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

<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://www.extremecomponents.org" prefix="ec" %>

<html>
<head>

  <title>Audit Log Report</title>
  <s:head theme="ajax" />
  
  <link rel="stylesheet" href="<c:url value='/css/ui.datepicker.css'/>" type="text/css" media="all"/>
  <script type="text/javascript" src="<c:url value='/js/jquery/jquery-1.2.6.pack.js'/>"></script>
  <script type="text/javascript" src="<c:url value='/js/jquery/ui.datepicker.packed.js'/>"></script>
  <script type="text/javascript" charset="utf-8">
    $(document).ready(function()
    {
		$('#startDate').datepicker()
		$('#endDate').datepicker()
    });
  </script>
</head>

<body>
<h1>Audit Log Report</h1>
  
<div id="contentArea">
  
  <s:form action="viewAuditLogReport" namespace="/report" validate="false">
     
    <div id="auditLogReport"> 	
	   	<s:select label="Repository" name="repository" list="repositories"/>
	   	
	   	<s:textfield label="Group ID" id="groupId" name="groupId"/>
	   	
	   	<s:textfield label="Artifact ID" id="artifactId" name="artifactId"/>
	   	
		<s:textfield label="Start Date" id="startDate" name="startDate"/>	      
	    <%--
	    <script type="text/javascript">
	      Calendar.setup({
	        inputField     :    "startDate",     
	        ifFormat       :    "%Y-%m-%d",             
	        align          :    "Tl",           
	        singleClick    :    true
	      });
	    </script>
	    --%>
		
		<s:textfield label="End Date" id="endDate" name="endDate"/>
		<%--
		<script type="text/javascript">
	      Calendar.setup({
	        inputField     :    "endDate",     
	        ifFormat       :    "%Y-%m-%d",             
	        align          :    "Tl",           
	        singleClick    :    true
	      });
	    </script>
		--%>    
		
		<s:textfield label="Row Count" name="rowCount" />
		
	    <s:submit value="View Audit Log"/>
    </div>    
   
  </s:form>
  
   <c:if test="${not empty (auditLogs)}">
	  <table border="1">
        <thead>
	      <tr>
	        <th align="center">Event</th>
	        <th align="center">Repository</th>
	        <th align="center">Artifact</th>
	        <th align="center">Event Date</th>
	        <th align="center">Username</th>
	      </tr>
	    </thead>
	    <c:forEach items="${auditLogs}" var="auditLog" varStatus="i">
	    <tbody>
	      <tr>
	        <td>${auditLog.event}</td>
	        <td>${auditLog.repositoryId}</td>
	        <td>${auditLog.artifact}</td>
	        <td>${auditLog.eventDate}</td>
	        <td>${auditLog.username}</td>
	      </tr>
	    </tbody>
	    </c:forEach>
	  </table>   
    </c:if> 
</div>

</body>
</html>
