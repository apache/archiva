<#
    Powershell script for cleaning up remaining processes on the CI servers
#>

$currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
Write-Output "$currentUser"

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



$processes = Get-WmiObject Win32_Process -Filter "name = 'java.exe'" | Where-Object {$_.GetOwner().User -eq $currentUser } 
foreach($proc in $processes)
{
    if($proc.CommandLine.Contains("selenium-server.jar"))
    {
        Write-Host "stopping proccess $($proc.ProcessId) with $($proc.ThreadCount) threads; $($proc.CommandLine.Substring(0, 50))..."
        Stop-Process -F $proc.ProcessId
    } else
    {
        Write-Host "skipping proccess $($proc.ProcessId) with $($proc.ThreadCount) threads; $($proc.CommandLine.Substring(0, 50))..."
    }
}

Write-Output "Filter: name = '$procName'"
foreach ($procName in ['firefox.exe','ieplore.exe','chrome.exe']) 
{
  $processes = Get-WmiObject Win32_Process -Filter "name = '$procName'" | Where-Object {$_.GetOwner().User -eq $currentUser } 
  foreach($proc in $processes)
  {
     Write-Host "stopping proccess $($proc.ProcessId) with $($proc.ThreadCount) threads; $($proc.CommandLine.Substring(0, 50))..."
     Stop-Process -F $proc.ProcessId
  }
}
