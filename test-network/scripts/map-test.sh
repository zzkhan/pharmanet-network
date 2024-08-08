#!/bin/bash

l="org1"
port="7054"

echo $l

case $l in
    "org1")
        port="7054"
        ;;
    "org2")
        port="8054"
        ;;
    "org3")
        port="9054"
        ;;
    "org4")
        port="10054"
        ;;
    "org5")
        port="11054"
        ;;
    *)
        echo "Unknown org ${l}"
        ;;
esac

echo "port is ${port}"
