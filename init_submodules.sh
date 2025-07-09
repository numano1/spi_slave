#!/usr/bin/env bash
#Selective initialization of submodules
#Written by by Marko Kosunen, marko.kosunen@aalto.fi, 2017
DIR=$( cd `dirname $0` && pwd )
SUBMODULES="\
    ./async_set_register \
    "

git submodule sync
for mod in $SUBMODULES; do 
    git submodule update --init $mod
    cd ${mod}
    if [ -f ./init_submodules.sh ]; then
        ./init_submodules.sh
    fi
    sbt publishLocal
    cd ${DIR}

done
exit 0









