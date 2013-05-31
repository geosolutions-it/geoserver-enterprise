#!/bin/bash
source ./setParams.sh
curl -X GET  \
  ${GSURI}/rest/bkprst \
    --user ${LOGIN}
