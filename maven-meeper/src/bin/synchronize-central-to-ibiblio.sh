#!/bin/bash

[ "$1" = "" ] && echo && echo "You must pass in the synchronize.properties file!" && echo && exit

. $1

echo ">>>>>>>>>>>>>>>>>> Syncing Maven 2.x repository to Ibiblio"

rsync -e ssh --delete --max-delete=10 -v -riplt $MAVEN2_REPO/ $IBIBLIO_SYNC_HOST:$M2_IBIBLIO_SYNC_DIR

echo ">>>>>>>>>>>>>>>>>> Syncing Maven 1.x plugins to Ibiblio"

# M1 Sync only for plugins ...
rsync -e ssh --delete --max-delete=10 -v -riplt $MAVEN1_REPO/ $IBIBLIO_SYNC_HOST:$M1_IBIBLIO_SYNC_DIR
