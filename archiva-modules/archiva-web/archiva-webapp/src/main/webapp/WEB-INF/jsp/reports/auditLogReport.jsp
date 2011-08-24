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
  <s:head theme="xhtml" />
  
  <link rel="stylesheet" href="<c:url value='/css/no-theme/jquery-ui-1.8.14.theme.css'/>" type="text/css" />
  <script type="text/javascript" src="<c:url value='/js/jquery-1.6.1.min.js'/>"></script>
  <script type="text/javascript" src="<c:url value='/js/jquery-ui-1.8.14.custom.min.js'/>"></script>
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
     
    <s:hidden name="initial"/>
    
    <div id="auditLogReport"> 	
        <table id="auditLogFieds">
          <tbody>
            <tr>
	   	      <td>Repository: </td>
	   	      <td><s:select name="repository" list="repositories" theme="simple"/></td>
	   	    <tr>
	   	    <tr>
	   	      <td>Group ID: </td>
	   	      <td><s:textfield id="groupId" name="groupId" theme="simple"/></td>
	   	    <tr>
	   	    <tr>
	   	      <td>Artifact ID: </td>
	   	      <td><s:textfield id="artifactId" name="artifactId" theme="simple"/></td>
	   	    <tr>
	   	    <tr>
	   	      <td>Start Date: </td>
	   	      <td><s:textfield id="startDate" name="startDate" theme="simple"/>	      
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
	    	  </td>
	   	    <tr>
	   	    <tr>
	   	      <td>End Date: </td>
	   	      <td><s:textfield id="endDate" name="endDate" theme="simple"/>
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
	    	  </td>
	   	    <tr>
	   	    <tr>
	   	      <td>Row Count: </td>
	   	      <td><s:textfield name="rowCount" theme="simple"/></td>
	   	    <tr>
	   	    <tr>
	   	      <td/>
	   	      <td style="text-align: right"><s:submit value="View Audit Log" theme="simple"/></td>
	   	    </tr>
	   	  </tbody>
	   	</table>	
    </div>    
    
    <p/>
    
    <div class="auditLogReportResults">
    
    <h2>${headerName}</h2>
      <p>
        <s:actionerror/>
      </p>
        
	    <c:if test="${not empty (auditLogs)}">  
		  <table class="auditlogs" cellspacing="0">
	         <tr>
		        <th>Event</th>
		        <th>Repository</th>
		        <th>Resource</th>
		        <th>Event Date</th>
		        <th>Username</th>
		      </tr>
		    
		    <c:forEach items="${auditLogs}" var="auditLog" varStatus="i">	    
		      <tr>
		        <td>${auditLog.action}</td>
		        <td>${auditLog.repositoryId}</td>
		        <td>${auditLog.resource}</td>
		        <td>${auditLog.timestamp}</td>
		        <td>${auditLog.userId}</td>
		      </tr>		    
		    </c:forEach>
		  </table>	

          <c:set var="prevPageUrl">
            <s:url action="viewAuditLogReport" namespace="/report">
              <s:param name="repository" value="%{#attr.repository}" />
              <s:param name="groupId" value="%{#attr.groupId}" />
              <s:param name="artifactId" value="%{#attr.artifactId}" />
              <s:param name="rowCount" value="%{#attr.rowCount}" />
              <s:param name="page" value="%{#attr.page - 1}"/>
              <s:param name="startDate" value="%{#attr.startDate}"/>
              <s:param name="endDate" value="%{#attr.endDate}" />
            </s:url>
          </c:set>
          <c:set var="nextPageUrl">
            <s:url action="viewAuditLogReport" namespace="/report">
              <s:param name="repository" value="%{#attr.repository }" />
              <s:param name="groupId" value="%{#attr.groupId}" />
              <s:param name="artifactId" value="%{#attr.artifactId }" />
              <s:param name="rowCount" value="%{#attr.rowCount}" />
              <s:param name="page" value="%{#attr.page + 1}"/>
              <s:param name="startDate" value="%{#attr.startDate}"/>
              <s:param name="endDate" value="%{#attr.endDate}" />
            </s:url>
           </c:set>

           <s:set name="page" value="page"/>
           <c:if test="${page gt 1}"><a href="${prevPageUrl}">&lt;&lt;</a></c:if>
           <strong>Page: </strong>${page}
           <s:set name="isLastPage" value="isLastPage"/>
           <c:if test="${!isLastPage}"><a href="${nextPageUrl}">&gt;&gt;</a></c:if>
 
		</c:if>  
	</div>
   
  </s:form>
    
  
</div>

</body>
</html>
