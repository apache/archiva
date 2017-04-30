<#
    Powershell script for cleaning up remaining processes on the CI servers
#>

$list = dir
foreach ($item in $list) {
     $fn = $item.name + "_.txt"
     get-itemproperty $item | format-list 
}
Get-Process -IncludeUserName 
