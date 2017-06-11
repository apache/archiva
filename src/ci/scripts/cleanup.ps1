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
# Powershell script for cleaning up remaining browser and selenium server processes on the CI servers
#
# Author: Martin Stockhammer <martin_s@apache.org>  
# Date  : 2017-04-30
#
# Descriptions:
#  Stopps processes related to the selenium checks, if they were not stopped by the selenium server, because
#  the job was aborted.
#  The script cannot determine, which of the processes are started by the current job, so if there are
#  parallel jobs running on this server that start processes with the same name and user, these
#  will be stopped too.
#
#  Per default the script will stop "firefox.exe","iexplore.exe","chrome.exe"
#  and the processes "java.exe","mshta.exe" if their commandline arguments contain "selenium-server"
# 
# Parameters:
#  -Verbose              : If set, more output will be printed
#  -Browsers proc1,proc2 : The list of executables that define the browser processes, that are started by selenium
#  -SeleniumProcesses    : The list of processes with the string "selenium-server" in the commandline arguments

param (
    [switch]$Verbose = $False,
    [String[]]$Browsers = @("firefox.exe","iexplore.exe","chrome.exe"),
    [String[]]$SeleniumProcesses = @("mshta.exe","java.exe","IEDriverServer.exe")
)

# $currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$currentUser = $env:UserName
Write-Output "User: $currentUser"

if ($Verbose) 
{
  Get-Process | Get-Member
  
  $View = @(
   @{l='Handles';e={$_.HandleCount}},
   @{l='NPM(K)';e={ (Get-Process -Id $_.ProcessId).NonpagedSystemMemorySize/1KB -as [int]}},
   @{l='PM(K)';e={ $_.PrivatePageCount/1KB -as [int]}},
   @{l='WS(K)';e={ $_.WorkingSetSize/1KB -as [int]}},
   @{l='VM(M)';e={ $_.VirtualSize/1mB -as [int]}},
   @{l='CPU(s)';e={ (Get-Process -Id $_.ProcessId).CPU -as [int]}},
   @{l='Id';e={ $_.ProcessId}},
   'UserName'
   @{l='ProcessName';e={ $_.ProcessName}}
  )
  Get-WmiObject Win32_Process | % { $_ | 
      Add-Member -MemberType ScriptProperty -Name UserName -Value {
          '{0}\{1}' -f $this.GetOwner().Domain,$this.GetOwner().User
      } -Force -PassThru
  }  
}

foreach ($procName in $SeleniumProcesses) 
{
  $processes = Get-WmiObject Win32_Process -Filter "name = '$procName'" | Where-Object {$_.GetOwner().User -eq $currentUser }  | Where-Object {$_.CommandLine -match "selenium-server"}
  if ($Verbose) {
    Write-Output "Filter: name = '$procName'"
  }
  foreach($proc in $processes)
  {
    Write-Output "stopping proccess $($proc.ProcessId) with $($proc.ThreadCount) threads; $($proc.CommandLine.Substring(0, 50))..."
    Stop-Process -F $proc.ProcessId
  }
}

foreach ($procName in $Browsers) 
{
  $processes = Get-WmiObject Win32_Process -Filter "name = '$procName'" | Where-Object {$_.GetOwner().User -eq $currentUser } 
  if ($Verbose) {
    Write-Output "Filter: name = '$procName'"
  }
  foreach($proc in $processes)
  {
     Write-Output "stopping proccess $($proc.ProcessId) with $($proc.ThreadCount) threads; $($proc.CommandLine.Substring(0, 50))..."
     Stop-Process -F $proc.ProcessId
  }
}
