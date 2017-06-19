#!/usr/bin/env bash

JAVA=java
JAVA_OPS="-Xmx2g -XX:+UseConcMarkSweepGC"
LIB=mailer-1.0.0-SNAPSHOT.jar:lib/*
$JAVA $JAVA_OPS -cp $LIB:conf/:templates/._class/ vn.com.vndirect.mail.MailerService &
echo $! > PID


