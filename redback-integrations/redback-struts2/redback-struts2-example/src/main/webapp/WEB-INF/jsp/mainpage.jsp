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

<%@ taglib prefix="s" uri="/struts-tags" %>
<%@ taglib prefix="redback" uri="/redback/taglib-1.0" %>

<html>

<head>
  <title>Redback Security Example Webapp</title>
  <s:head />
</head>

<body>

<h4>This is the example mainpage</h4>

<div id="results">
  <%-- This is where the "Account Created Successfully" type message goes. --%>
  <div class="success">
    <s:actionmessage />
  </div>
  <%-- This is where errors from the action and other non-form field specific errors appear. --%>
  <div class="errors">
    <s:actionerror />
  </div>
</div>

<ol>
<li>
  <redback:ifAuthorized permission="user-management-manage-data">    
    <p/>        
    <s:url id="backupRestoreUrl" action="backupRestore" namespace="/security"/>
    <ul>      
      <li>Go see the <s:a href="%{backupRestoreUrl}">backup-restore</s:a>.</li>
    </ul>    
  </redback:ifAuthorized>
  
  <redback:ifAuthorized permission="user-management-user-list">
    You are authorized to see this content!
    <p/>

    <s:url id="userlistUrl" action="userlist" namespace="/security"/>
    <s:url id="rolesUrl" action="roles" namespace="/security"/>
        
    <ul>
      <li>Go see the <s:a href="%{userlistUrl}">userlist</s:a>.</li>      
      <li>Go see the <s:a href="%{rolesUrl}">roles</s:a>.</li>
    </ul>
    

  </redback:ifAuthorized>
  <redback:elseAuthorized>
    <redback:ifAuthorized permission="user-management-user-edit" resource="${sessionScope.securitySession.user.username}">
      Your logged in, you just don't have access to much...
    </redback:ifAuthorized>
    <redback:elseAuthorized>
      <s:url id="login" action="login" namespace="/security" />
      Go Ahead <s:a href="%{login}">Login.</s:a>
    </redback:elseAuthorized>
  </redback:elseAuthorized>

</li>
</ol>

</body>

</html>
