#!/bin/bash

[ "$1" = "" ] && echo && echo "You must pass in the synchronize.properties file!" && echo && exit

. $1

echo ">>>>>>>>>>>>>>>>>> Syncing Maven 2.x repository to cica.es"

rsync -e ssh --delete --max-delete=10 -v -riplt $MAVEN2_REPO/ $CICA_USERNAME@$CICA_SYNC_HOST:$M2_CICA_SYNC_DIR
