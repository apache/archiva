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

<html>
<head>
  <title>Admin: Add Legacy artifact path</title>
  <ww:head/>
</head>

<body>

<h1>Admin: Add Legacy artifact path</h1>

<div id="contentArea">

  <script type="text/javascript">
  function parse( path )
  {
      var group = path.indexOf( "/" );
      if ( group > 0 )
      {
          document.getElementById( "addLegacyArtifactPath_groupId" ).value
              = path.substring( 0, group );
          group += 1;
          var type = path.indexOf( "/", group );
          if ( type > 0 )
          {
              document.getElementById( "addLegacyArtifactPath_type" ).value
                  = path.substring( group, type - 1 );
          }
          type += 1;
          var version = path.indexOf( "-", type );
          var ext = path.lastIndexOf( "." );
          if ( version > 0 )
          {
              document.getElementById( "addLegacyArtifactPath_artifactId" ).value
                  = path.substring( type, version );
              document.getElementById( "addLegacyArtifactPath_version" ).value
                  = path.substring( version + 1, ext );
          }

      }
  }
  function computeArtifactReference()
  {
      document.getElementById("addLegacyArtifactPath_legacyArtifactPath_artifact").value
          = document.getElementById("addLegacyArtifactPath_groupId").value
          + ":"
          + document.getElementById("addLegacyArtifactPath_artifactId").value
          + ":"
          + document.getElementById("addLegacyArtifactPath_version").value
          + ":"
          + document.getElementById("addLegacyArtifactPath_classifier").value
          + ":"
          + document.getElementById("addLegacyArtifactPath_type").value;
  }
  </script>

  <ww:actionmessage/>
  <ww:actionerror/>
  <ww:form method="post" action="addLegacyArtifactPath!commit" namespace="/admin" validate="true" onsubmit="computeArtifactReference()">
    <ww:textfield name="legacyArtifactPath.path" label="Path" size="50" required="true" onchange="parse( this.value )"/>
    <ww:textfield name="groupId" label="GroupId" size="20" required="true" disabled="true"/>
    <ww:textfield name="artifactId" label="ArtifactId" size="20" required="true"/>
    <ww:textfield name="version" label="Version" size="20" required="true"/>
    <ww:textfield name="classifier" label="Classifier" size="20" required="false"/>
    <ww:textfield name="type" label="Type" size="20" required="true" disabled="true"/>
    <ww:hidden name="legacyArtifactPath.artifact"/>
    <ww:submit value="Add Legacy Artifact Path"/>
  </ww:form>

  <script type="text/javascript">
    var ref = document.getElementById("addLegacyArtifactPath_legacyArtifactPath_artifact").value;
    var i = ref.indexOf( ":" );
    document.getElementById("addLegacyArtifactPath_groupId").value = ref.substring( 0, i );
    var j = i + 1;
    var i = ref.indexOf( ":", j );
    document.getElementById("addLegacyArtifactPath_artifactId").value = ref.substring( j, i );
    var j = i + 1;
    var i = ref.indexOf( ":", j );
    document.getElementById("addLegacyArtifactPath_version").value = ref.substring( j, i );
    var j = i + 1;
    var i = ref.indexOf( ":", j );
    document.getElementById("addLegacyArtifactPath_classifier").value = ref.substring( j, i );

    document.getElementById("addLegacyArtifactPath_legacyArtifactPath_path").focus();
  </script>

</div>

</body>
</html>
