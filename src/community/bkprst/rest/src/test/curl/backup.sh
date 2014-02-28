#!/bin/bash
source ./setParams.sh
curl -X POST --header 'Content-type: text/xml' \
  --data "<task><path>${BACKUP_DATADIR}</path></task>" \
  ${GSURI}/rest/bkprst/backup \
  --user ${LOGIN} > id.txt
export id=`sed 's/<id>//g' id.txt | sed 's/<\/id>//g'`
rm id.txt
echo "curl -X GET ${GSURI}/rest/bkprst/${id} --user ${LOGIN}" > taskinfo.sh
chmod a+x *.sh
./taskinfo.sh  
