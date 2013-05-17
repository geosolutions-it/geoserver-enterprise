#!/bin/bash
source ./setParams.sh
curl -X GET \
  ${GSURI}/wms?request=getcapabilities\&version=1.1.1\&service=wms \
  --write-out "%{http_code}\n" \
  --output /dev/null 
   
  
