#!/bin/sh

echo "Migrating database"
gradle flywayMigrate \
        -i \
        -Pflyway.url=$JDBC_URL \
        -Pflyway.baselineOnMigrate=true
