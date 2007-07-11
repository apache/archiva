#!/bin/bash

##
##Usage: <scriptname> <process name> <timeout in hours> 
##


 ps -eo comm,etime,pid |
 grep $1|
 awk -v TIMEOUT=4 '
{
	if($2~/-/){
		system("kill -9 "$3);
	}
	else{
		tl=split($2, tm, ":");
		print tl;
		if(tl==3 && tm[1]>TIMEOUT){
			system("kill -9 "$3);
		}
	}
}'
