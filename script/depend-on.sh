#!/bin/sh

HOST=$1
PORT=$2
TIMEOUT=$3

depend_on() {
    for i in `seq $TIMEOUT` ; do
    nc -z "$HOST" "$PORT" > /dev/null 2>&1

    result=$?
    if [ $result -eq 0 ] ; then
        exit 0
    fi
    echo "Waiting..."
    sleep 1
    done
    exit 123
}

depend_on "$@"
