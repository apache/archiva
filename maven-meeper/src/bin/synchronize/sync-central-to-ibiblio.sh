#!/bin/bash

syncProperties=$1                                                                                                                          
                                                                                                                                           
. $syncProperties      

dest=/home/maven/repository-staging/to-ibiblio

rsync -e ssh --delete --max-delete=10 -v -riplt $dest/maven2/ $IBIBLIO_SYNC_HOST:$M2_IBIBLIO_SYNC_DIR

date > $dest/maven2/last-sync.txt
chmod a+r $dest/maven2/last-sync.txt

# M1 Sync only for plugins ...
rsync -e ssh --delete --max-delete=10 -v -riplt $dest/maven/ $IBIBLIO_SYNC_HOST:$M1_IBIBLIO_SYNC_DIR
