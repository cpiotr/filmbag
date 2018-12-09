#!/bin/sh
echo "Running application"
java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dlogging.level.root=$ROOT_LOGGING_LEVEL \
		-Dlogging.level.pl.ciruk=$APP_LOGGING_LEVEL \
		-Dserver.port=$APP_PORT \
		-Dspring.datasource.url=$JDBC_URL \
		-Dexternal.provider.filmrequest.url=$FILM_PROVIDER_URL \
		$JMX_OPTS \
		$JVM_OPTS \
		-jar ./build/libs/filmbag-$APP_VERSION.jar
