#!/bin/bash
source ./setParams.sh

curl -X PUT -v \
  ${GSURI}/rest/reload \
  --user ${LOGIN} 
  