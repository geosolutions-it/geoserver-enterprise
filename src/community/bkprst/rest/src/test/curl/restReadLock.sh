#!/bin/bash
source ./setParams.sh
curl -X GET --header 'Content-type: text/xml' \
  ${GSURI}/rest/styles/line.sld \
  --write-out "%{http_code}\n" \
  --user ${LOGIN}