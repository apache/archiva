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

<html>
<head>
  <title>Find Artifact</title>
  <ww:head/>
</head>

<body onload="document.checksumSearch.file.disabled = false">

<h1>Find Artifact</h1>

<div id="contentArea">
  <div id="searchBox">
    <script type="text/javascript">
      function generateMd5( file, defVal )
      {
        if ( file )
        {
          var s = document.ChecksumApplet.generateMd5(file);
          // If there is a space, it's an error message, not a checksum
          if ( s.indexOf(" ") >= 0 )
          {
            alert(s);
            return "";
          }
          else
            return s;
        }
        return defVal;
      }
    </script>

    <noscript>
      <span class="errorMessage">JavaScript is disabled: using the file browser will not work.</span>
    </noscript>

    <ww:form method="POST" action="checksumSearch" namespace="/"
             onsubmit="this.md5.value = generateMd5(this.file.value,this.md5.value); this.file.disabled = true">
      <tr>
        <td class="tdLabel"><label for="checksumSearch_file" class="label">Search for:</label></td>
        <td>
          <input type="file" name="file" size="50" value="" id="checksumSearch_file"/>
        </td>
      </tr>
      <ww:textfield label="Checksum" size="50" name="md5"/>
      <ww:submit value="Go!"/>
    </ww:form>

    <p>
      Select the file you would like to locate in the remote repository.
      The entire file will
      <b>not</b>
      be uploaded to the server. See the progress bar below for progress of
      locally creating a checksum that is uploaded to the server after you hit "Go!".
      <ww:actionerror/>
    </p>

    <p>
      <applet code="org/apache/maven/archiva/applet/ChecksumApplet.class"
              archive="archiva-applet.jar"
              width="400" height="20" name="ChecksumApplet">
      </applet>
    </p>

  </div>
</div>

</body>
</html>