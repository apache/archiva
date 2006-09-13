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
  <title>Login Page</title>
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

    <h2>Login</h2>

	<%-- You don't need a table to wrap form elements in,
	     the ww:form creates the table, labels, context sensitive actionerrors, requirements indicators, etc...
	       - Joakim --%>

    <ww:form action="login" method="post">
      <ww:textfield label="Username" name="username" size="30" required="true" />
      <ww:password  label="Password" name="password" size="20" required="true" />
      <ww:submit value="Login"/>
    </ww:form>
       
    <ul class="tips">
      <li>
         Forgot your Username? 
         <ww:url id="forgottenAccount" action="findAccount" />
         <ww:a href="%{forgottenAccount}">Email me my account information.</ww:a>
      </li>
      <li>
         Forgot your Password? 
         <ww:url id="forgottenPassword" action="resetPassword" />
         <ww:a href="%{forgottenPassword}">Request a password reset.</ww:a>
      </li>
      <li>
        Need an Account?
        <ww:url id="registerUrl" action="register" />
        <ww:a href="%{registerUrl}">Register!</ww:a>
    </ul>
  </div>
</div>


<div class="clear">
  <hr/>
</div>

</body>

</html>
