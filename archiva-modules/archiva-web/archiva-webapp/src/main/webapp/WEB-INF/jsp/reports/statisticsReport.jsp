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
<%@ taglib prefix="archiva" uri="http://archiva.apache.org" %>

<html>
<head>
  <title>Reports</title>
  <ww:head/>
</head>

<body>
<h1>Statistics Report</h1>

<div id="contentArea">  		
  <c:choose>
  	<c:when test="${reposSize > 1}">
  	
  	    <h1>Latest Statistics Comparison Report</h1>  	    
  		<table class="infoTable" border="1">
		<tr>
		  <th>Repository</th>
		  <th>Total File Count</th>
		  <th>Total Size</th>
		  <th>Artifact Count</th>
		  <th>Group Count</th>
		  <th>Project Count</th>
		  <th>Plugins</th>
		  <th>Archetypes</th>
		  <th>Jars</th>
		  <th>Wars</th>
		  <th>Deployments</th>
		  <th>Downloads</th>
		</tr>			
		
		<c:forEach var="stats" items="${repositoryStatistics}">
			<tr>
				<td>${stats.repositoryId}</td>
				<td align="right">${stats.fileCount}</td>
				<td align="right">${stats.totalSize}</td>
				<td align="right">${stats.artifactCount}</td>
				<td align="right">${stats.groupCount}</td>
				<td align="right">${stats.projectCount}</td>
				<td align="right">${stats.pluginCount}</td>
				<td align="right">${stats.archetypeCount}</td>
				<td align="right">${stats.jarCount}</td>
				<td align="right">${stats.warCount}</td>
				<td align="right">${stats.deploymentCount}</td>
				<td align="right">${stats.downloadCount}</td>
			</tr>				
		</c:forEach>
	  </table>
  		
  	</c:when>
  	<c:otherwise>
  	
  		<h1>Statistics for Repository '${selectedRepo}'</h1>
  		<table class="infoTable" border="1">
			<tr>
			  <th>Date of Scan</th>
			  <th>Total File Count</th>
			  <th>Total Size</th>
			  <th>Artifact Count</th>
			  <th>Group Count</th>
			  <th>Project Count</th>
			  <th>Plugins</th>
			  <th>Archetypes</th>
			  <th>Jars</th>
			  <th>Wars</th>
			  <th>Deployments</th>
		  	  <th>Downloads</th>
			</tr>			
	  		
	  		<c:forEach var="stats" items="${repositoryStatistics}">
	  			<tr>
	  				<td align="right">${stats.dateOfScan}</td>
	  				<td align="right">${stats.fileCount}</td>
	  				<td align="right">${stats.totalSize}</td>
	  				<td align="right">${stats.artifactCount}</td>
	  				<td align="right">${stats.groupCount}</td>
	  				<td align="right">${stats.projectCount}</td>
	  				<td align="right">${stats.pluginCount}</td>
	  				<td align="right">${stats.archetypeCount}</td>
	  				<td align="right">${stats.jarCount}</td>
	  				<td align="right">${stats.warCount}</td>
	  				<td align="right">${stats.deploymentCount}</td>
					<td align="right">${stats.downloadCount}</td>
	  			</tr>				
	  		</c:forEach>
  		</table>
  		  		
  	</c:otherwise>
  </c:choose>
 
</div>
</body>
</html>
