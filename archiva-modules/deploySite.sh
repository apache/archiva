#!/bin/bash

mvn clean site site:stage -Preporting "$@"
mvn scm-publish:publish-scm "$@"

