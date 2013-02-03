#!/bin/sh
mvn package
java -jar target/ReminderBot.jar \
   --name reminderbot \
   --host irc.collapse.io \
   --port 6667 \
   --channels "#collapse,#reminderbot"
