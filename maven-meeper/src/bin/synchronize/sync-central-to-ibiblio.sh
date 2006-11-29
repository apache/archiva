#!/bin/bash

syncProperties=$1                                                                                                                          
                                                                                                                                           
. $syncProperties      

dest=/home/maven/repository-staging/to-ibiblio

rsync -e ssh --delete --max-delete=10 -v -riplt $dest/maven2/ login.ibiblio.org:/public/html/maven2

date > $dest/maven2/last-sync.txt
chmod a+r $dest/maven2/last-sync.txt

# M1 Sync only for plugins ...
rsync -e ssh --delete --max-delete=10 -v -riplt $dest/maven/ login.ibiblio.org:/public/html/maven
