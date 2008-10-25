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

<!-- jQuery -->
<script type="text/javascript" src="/archiva/js/jquery/jquery-1.2.6.packjs"></script>

<!-- required plugins -->
<script type="text/javascript" src="/archiva/js/jquery/date.js"></script>

<!-- jquery.datePicker.js -->
<script type="text/javascript" src="/archiva/js/jquery/jquery.datePicker.js"></script>


<html>
<head>
  <title>Reports</title>
  <s:head theme="ajax" />
  
  <script type="text/javascript" charset="utf-8">
    $(function()
    {
		$('#startDate').datePicker({clickInput:true})
		$('#endDate').datePicker({clickInput:true})
    });
  </script>
</head>

<body>
<h1>Reports</h1>
  
<div id="contentArea">

  <h2>Repository Statistics</h2>
  <s:form action="generateStatisticsReport" namespace="/report" validate="false">
    
    <s:optiontransferselect label="Repositories To Be Compared" name="availableRepositories"
		list="availableRepositories" doubleName="selectedRepositories"
		doubleList="selectedRepositories" size="8" doubleSize="8" 
		addAllToRightOnclick="selectAllOptions(document.getElementById('generateStatisticsReport_availableRepositories'));selectAllOptions(document.getElementById('generateStatisticsReport_selectedRepositories'));"
		addToRightOnclick="selectAllOptions(document.getElementById('generateStatisticsReport_availableRepositories'));selectAllOptions(document.getElementById('generateStatisticsReport_selectedRepositories'));"
		addAllToLeftOnclick="selectAllOptions(document.getElementById('generateStatisticsReport_availableRepositories'));selectAllOptions(document.getElementById('generateStatisticsReport_selectedRepositories'));"
		addToLeftOnclick="selectAllOptions(document.getElementById('generateStatisticsReport_availableRepositories'));selectAllOptions(document.getElementById('generateStatisticsReport_selectedRepositories'));" 
		/>
	
	<s:textfield label="Row Count" name="rowCount" /> 
	
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
