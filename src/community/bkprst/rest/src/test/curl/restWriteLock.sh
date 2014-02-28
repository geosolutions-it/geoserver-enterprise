#!/bin/bash
source ./setParams.sh
curl -X DELETE \
  ${GSURI}/rest/styles/line2?purge=true \
  --write-out "%{http_code}\n" \
  --user ${LOGIN}
curl -X POST --header "Content-type: application/vnd.ogc.sld+xml" -v \
  ${GSURI}/rest/styles?name=line2 \
  --data @style.sld \
  --post301 --post302 \
  --write-out "%{http_code}\n" \
  --user ${LOGIN}