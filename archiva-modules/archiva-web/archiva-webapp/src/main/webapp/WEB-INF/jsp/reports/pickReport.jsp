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

<link type="text/css" rel="StyleSheet" href="/archiva/css/datepicker.css" />
<script type="text/javascript" src="/archiva/js/datepicker/datepicker.js"></script>

<html>
<head>
  <title>Reports</title>
  <s:head theme="ajax" />
</head>

<body>
<h1>Reports</h1>
  
<div id="contentArea">

  <h2>Repository Statistics</h2>
  <s:form action="generateStatisticsReport" namespace="/report" validate="false">
    
    <s:optiontransferselect label="Repositories To Be Compared" name="availableRepositories"
		list="availableRepositories" doubleName="selectedRepositories"
		doubleList="selectedRepositories" size="8" doubleSize="8"/>
	
	<s:textfield label="Row Count" name="rowCount" /> 	
	<s:textfield label="Start Date" name="startDate" disabled="true"/>
      <script type="text/javascript">
          var d1 = new Date();
          var dp1 = new DatePicker(d1);
 
          var tables = document.forms[0].getElementsByTagName("table");
          var myRow = tables[0].insertRow(3);
          var actionsCell = myRow.insertCell(0);
          var startDateCell = myRow.insertCell(1);
          startDateCell.appendChild(dp1.create());
      
          dp1.onchange = function () {
   	           document.forms[0].startDate.value = dp1.getDate();
          };
      </script>

    <s:textfield label="End Date" name="endDate" disabled="true"/>
	  <script type="text/javascript">
          var d2 = new Date();
          var dp2 = new DatePicker(d2);

          var tables = document.forms[0].getElementsByTagName("table");
          var myRow = tables[0].insertRow(5);
          var actionsCell = myRow.insertCell(0);
          var startDateCell = myRow.insertCell(1);
          startDateCell.appendChild(dp2.create());
      
          dp2.onchange = function () {
   	           document.forms[0].endDate.value = dp2.getDate();
          };
     </script>
        
    <s:submit value="View Statistics"/>
  </s:form>
    
  <h2>Repository Health</h2>
  <s:form namespace="/report" action="generateReport" validate="true">
    <s:textfield label="Row Count" name="rowCount" />
    <s:textfield label="Group ID" name="groupId"/>
    <s:select label="Repository ID" name="repositoryId" list="repositoryIds"/>
  
    <s:submit value="Show Report"/>
  </s:form>

</div>

</body>
</html>
