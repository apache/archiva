<%--
  ~ Copyright 2005-2006 The Apache Software Foundation.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~    http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="ww" uri="/webwork" %>

<html>
<head>
  <title>Configuration</title>
  <ww:head/>
</head>

<body>

<h1>Available Permissions</h1>

<div id="contentArea">
  <ww:url id="rolesUrl" action="roles"/>
  <ww:url id="permissionsUrl" action="permissions"/>
  <ww:url id="operationsUrl" action="operations"/>
  <ww:url id="resourcesUrl" action="resources"/>

  <p><ww:a href="%{rolesUrl}">Roles</ww:a>|<ww:a href="%{permissionsUrl}">Permissions</ww:a>|<ww:a href="%{operationsUrl}">Operations</ww:a>|<ww:a href="%{resourcesUrl}">Resources</ww:a> </p>

    <p>
      Permissions list page
    </p>
    <ww:actionerror/>

  <ww:iterator id="permission" value="permissions">
       <ww:url id="permissionUrl" action="permission">
         <ww:param name="permissionName">${permission.name}</ww:param>
       </ww:url>

       <ww:a href="%{permissionUrl}">${permission.name}</ww:a><br/>
     </ww:iterator>

     <p>
       <ww:url id="newPermissionUrl" action="permission"/>

       <ww:a href="%{newPermissionUrl}">new</ww:a><br/>
     </p>
     
    
</div
  </body>
</html>