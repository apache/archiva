#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
# Powershell script updating Selenium drivers on ci server
#
# Author: Martin Stockhammer <martin_s@apache.org>  
# Date  : 2017-05-14
#
# Description:

param (
    [switch]$Verbose = $False,
    [switch]$Force = $False
)

$psVersion = $PSVersionTable.PSVersion

Write-Output "PS-Version: $psVersion"
Write-Output "Verbose: $Verbose, Force: $Force"

$urls = @{
  "iedriver\2.53.1\win64\DriverServer.zip"="http://selenium-release.storage.googleapis.com/2.53/IEDriverServer_x64_2.53.1.zip"
  "iedriver\2.53.1\win32\DriverServer.zip"="http://selenium-release.storage.googleapis.com/2.53/IEDriverServer_Win32_2.53.1.zip"
  "iedriver\3.4.0\win64\DriverServer.zip"="http://selenium-release.storage.googleapis.com/3.4/IEDriverServer_x64_3.4.0.zip"
  "iedriver\3.4.0\win32\DriverServer.zip"="http://selenium-release.storage.googleapis.com/3.4/IEDriverServer_Win32_3.4.0.zip"
  "chromedriver\2.29\win32\DriverServer.zip"="http://chromedriver.storage.googleapis.com/2.29/chromedriver_win32.zip"
  "geckodriver\0.16.1\win32\DriverServer.zip"="http://github.com/mozilla/geckodriver/releases/download/v0.16.1/geckodriver-v0.16.1-win32.zip"
  "geckodriver\0.16.1\win64\DriverServer.zip"="http://github.com/mozilla/geckodriver/releases/download/v0.16.1/geckodriver-v0.16.1-win64.zip"
}

foreach ($h in $urls.GetEnumerator()) {
  $url = $h.Value
  Write-Output "Driver $url"
  $downloadFile = "C:\jenkins\tools\$($h.Name)"
  $downloadDir = Split-Path $downloadFile -Parent

  if ($Force -And (Test-Path -Path $downloadDir ) ) {
    Get-ChildItem -Path $downloadDir -Recurse | Remove-Item -force -recurse
  }

  if(!(Test-Path -Path $downloadDir )){
    New-Item -ItemType directory -Path $downloadFile
  }

  if ($Force -Or !(Test-Path -Path $downloadFile )){
    Invoke-WebRequest -Uri $url -OutFile $downloadFile

    $shell = New-Object -ComObject shell.application
    $zip = $shell.NameSpace($downloadFile)
    foreach ($item in $zip.items()) {
      $shell.Namespace($downloadDir).CopyHere($item)
    }

    Get-ChildItem -Path $downloadDir
  }
}