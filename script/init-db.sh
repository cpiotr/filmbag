#!/bin/sh

echo "Migrating database"
$APP_DIR/gradlew flywayMigrate \
        -i \
        -Pflyway.url=$JDBC_URL \
        -Pflyway.baselineOnMigrate=true
