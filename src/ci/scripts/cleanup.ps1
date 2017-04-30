<#
    Powershell script for cleaning up remaining processes on the CI servers
#>

Get-Process 

$processes = Get-WmiObject Win32_Process -Filter "name = 'java.exe'"
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
Get-Process firefox | Stop-Process
Get-Process chrome | Stop-Process
Get-Process iexplore | Stop-Process
