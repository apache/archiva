#!/bin/sh

CONTACTS=""
MODE=rsync_ssh

FROM=mavensync@repository.codehaus.org:/repository
GROUP_DIR=
SSH_OPTS="-i $HOME/.ssh/new-id_dsa"
#RSYNC_OPTS="-L"

## NOTE that codehaus only honours some rsync options. Others may be summarily discarded and/or cause the rsync to break - check
## with them if changing them
