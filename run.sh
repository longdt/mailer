#!/usr/bin/env bash

JAVA=java
JAVA_OPS="-Xmx2g"
LIB=./*:target/lib/*
$JAVA $JAVA_OPS -cp $LIB:conf/:templates/._class/ vn.com.vndirect.mail.MailerService &
echo $! > PID


