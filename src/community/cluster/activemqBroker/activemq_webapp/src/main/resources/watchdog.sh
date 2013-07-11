#!/bin/bash

#The MIT License
#
#Copyright (c) 2011 GeoSolutions S.A.S.
#http://www.geo-solutions.it
#
#Permission is hereby granted, free of charge, to any person obtaining a copy
#of this software and associated documentation files (the "Software"), to deal
#in the Software without restriction, including without limitation the rights
#to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#copies of the Software, and to permit persons to whom the Software is
#furnished to do so, subject to the following conditions:
#
#The above copyright notice and this permission notice shall be included in
#all copies or substantial portions of the Software.
#
#THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
#THE SOFTWARE.

############################################################
# author: carlo cancellieri - ccancellieri@geo-solutions.it
# date: 7 Apr 2011
#
# simple watchdog for webservice
############################################################


# the url to use to test the service
# !make sure to implement the url_test logic also into the bottom function

# THE URL OF THE BROKER 'A'
URL_A="http://localhost:8383/activemq_master/"
URL_B="http://localhost:8383/activemq_slave/"
URL=$URL_A

#set the connection timeout (in seconds)
TIMEOUT=30

# seconds to wait to recheck the service (this is not cron)
# tomcat restart time
TOMCAT_TIMEOUT=50

# used to filter the process (to get the pid)
# use: ps -efl
# to see what you have to filter
FILTER_A="activemq_master"
FILTER_B="activemq_slave"
FILTER=$FILTER_A

# the service to restart
# should be a script placed here:
# /etc/init.d/
# 
SERVICE_A="activemq_master"
SERVICE_B="activemq_slave"
SERVICE=$SERVICE_A

# maximum tries to perform a quick restart
# when the service fails to be started a quick (30 sec)
# restart will be tried at least $RETRY times
# if restart fails RETRY times  the script ends returning '100'
RETRY=2

################### WATCHDOG #####################

url_test()
{
   #try to access tomcat's page
   #decide on reply
   # - example testing the exit code
   #wget -O - -T "${TIMEOUT}" -o "/dev/null" --proxy=off ${WFSURL} >> "${LOGFILE}" 2>&1
   #url_test "${?}"

   TMPFILE=/tmp/urltest
   rm $TMPFILE

   eval=wget -O $TMPFILE -T "${TIMEOUT}" -o "/dev/null" --proxy=off ${URL} >> "${LOGFILE}" 2>&1
   pid="`ps -efl | awk -v FILTER="${FILTER}" '!/awk/&&/org.apache.catalina.startup.Bootstrap/{if ($0 ~ FILTER) {print $4}}'`"

   if [ "$eval" == "0" ]; then
#OK -> A is reachable (non locked)      
      if [ -z "$pid" ]; then
#ERROR!!! process B is not found start the broker B
	  return 1;
      else
#OK -> B is reachable return 0
	  return 0;
      fi
   else
#ERROR!!! -> A is NOT reachable (may be it is locked???)
      if [ -z "$pid" ]; then
#GRAVE ERROR!!! process B is not found start the broker B and A
	  return 1;
      else
#OK -> B is reachable return 0
	  return 0;
      fi
   fi
   return 1;
}

switch()
{
    if [ "$URL" == "$URL_A" ]; then
	URL=$URL_B
    else
	URL=$URL_A
    fi

    # used to filter the process (to get the pid)
    # use: ps -efl
    # to see what you have to filter

    if [ "$FILTER" == "$FILTER_A" ]; then
	FILTER=$FILTER_B
    else
	FILTER=$FILTER_A
    fi

    # the service to restart
    # should be a script placed here:
    # /etc/init.d/
    # 
    if [ "$SERVICE" == "$SERVICE_A" ]; then
	SERVICE=$SERVICE_B
    else
	SERVICE=$SERVICE_A
    fi

    CMD_STOP="/etc/init.d/$SERVICE stop"
    CMD_START="/etc/init.d/$SERVICE start"

    # the output file to use as log
    # must exists otherwise the stdout will be used
    # NOTE: remember to logrotate this file!!!
    LOGFILE="/var/lib/tomcat/$SERVICE/logs/watchdog.log"
}

check(time, eval)
{
        #testing on url_test exit code
	if [ "$eval" -eq 0 ] ; then
		echo "`date` WatchDog Status: OK -> $SERVICE is responding at URL $URL" >> $LOGFILE
		return 0;
	else
		echo "`date` WatchDog Status: FAIL -> $SERVICE is NOT responding properly at URL $URL" >> $LOGFILE
		echo "`date` WatchDog Action: Stopping service $SERVICE" >> $LOGFILE

		PIDFILE=`${CMD_STOP} |awk '/PID/{gsub(".*[(]","",$0);gsub("[)].*","",$0); print $0}'`
		if [ -e "$PIDFILE" ]; then
			echo "`date` removing pid file: $PIDFILE" >> "${LOGFILE}"
			rm "$PIDFILE" >> "${LOGFILE}" 2>&1
		fi
		sleep 1

		for thepin in $(ps -eo pid,cmd | grep org.apache.catalina.startup.Bootstrap | grep "$FILTER" | grep -v grep | cut -f 1 -d \  ) ; do
#`ps -efl | awk -v FILTER="${FILTER}" '!/awk/&&/org.apache.catalina.startup.Bootstrap/{if ($0 ~ FILTER) {print $4}}'`; do
			echo "`date` WatchDog Action: Stop failed -> TERMinating service $SERVICE (pid: ${thepin})" >> $LOGFILE
			kill -15 $thepin >> $LOGFILE 2>&1
			sleep "$TIMEOUT"
			while [ "${thepin}" = "`ps -efl | awk -v FILTER="${FILTER}" '!/awk/&&/org.apache.catalina.startup.Bootstrap/{if ($0 ~ FILTER) {print $4}}'`" ];
			do 
				echo "`date` WatchDog Action: TERM failed -> KILLing service $SERVICE (pid: ${thepin})" >> "${LOGFILE}"
				kill -9 "${thepin}" >> "${LOGFILE}" 2>&1
				sleep "$TIMEOUT"
			done
		done

		echo "`date` WatchDog Action: Starting service ${SERVICE}" >> "${LOGFILE}"
		${CMD_START} >> "${LOGFILE}" 2>&1
		if [ "$?" -eq 0 ]; then
			echo "`date` WatchDog Action: service ${SERVICE} STARTED" >> "${LOGFILE}"
			times=`expr "$times" "+" "1"`
			# give tomcat time to start
			sleep "$TOMCAT_TIMEOUT"
			# let's retest the connection STILL NOT RETURN
		elif [ "$?" -eq 1 ]; then
			times=`expr "$times" "+" "1"`
			echo "`date` WatchDog Action: service ${SERVICE} ALREADY STARTED (WHAT'S HAPPENING? -> quick retry ($times/$RETRY))" >> "${LOGFILE}"
			# give tomcat time to start
			sleep "$TOMCAT_TIMEOUT"
		else
			times=`expr "$times" "+" "1"`
			echo "`date` WatchDog Action: Starting service FAILED ${SERVICE} (WHAT'S HAPPENING? -> quick retry ($times/$RETRY))" >> "${LOGFILE}"
			# give tomcat time to start
			sleep "$TOMCAT_TIMEOUT"
		fi
	fi
	return 1;
}

if [ ! -e "$LOGFILE" ]; then
	LOGFILE="/dev/stdout"
	echo "`date` WatchDog output file: DOES NOT EXIST: using ${LOGFILE}" >> "${LOGFILE}"
else
	echo "`date` WatchDog setting output to: ${LOGFILE}" >> "${LOGFILE}"
fi

#loop
times=0;
while [ "$times" -lt "$RETRY" ]
do
  	eval=url_test
	ret=check(times,eval);
	if [ "$ret" != "0" ]; then
	  echo "`date` WatchDog Action: Starting service FAILED ${SERVICE} (WHAT's HAPPENING? -> exit (status: 100))" >> "${LOGFILE}"
	fi
	switch
done

exit $ret