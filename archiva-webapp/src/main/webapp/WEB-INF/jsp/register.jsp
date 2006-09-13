<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
  <title>Registration Page</title>
  <ww:head/>
</head>

<body>

<div id="contentArea">
  <div id="searchBox">
  
    <div id="results">
      <%-- This is where the "Account Created Successfully" type message goes. --%>
      <div class="success">
	    <ww:actionmessage />
      </div>
      <%-- This is where errors from the action and other non-form field specific errors appear. --%>
      <div class="errors">
      	<ww:actionerror />
      </div>
    </div>

    <h2>Register for an Account</h2>

	<%-- You don't need a table to wrap form elements in,
	     the ww:form creates the table, labels, context sensitive actionerrors, requirements indicators, etc...
	       - Joakim --%>
    
    <ww:form action="register" method="post">   	
      <%@ include file="/WEB-INF/jsp/admin/include/registerUserForm.jspf" %>
      <ww:submit    value="Register"/>
    </ww:form>

  </div>
</div>


<div class="clear">
  <hr/>
</div>

</body>

</html>
