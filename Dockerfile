FROM gradle:8.7.0-jdk21-alpine as build
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ARG APP_VERSION=0.0.1
ENV APP_VERSION=${APP_VERSION}
ENV JDBC_URL jdbc:mariadb://filmbag-db:3306/db
ENV FILM_PROVIDER_URL http://192.168.178.206:8080/resources/suggestions
ENV FILM_PROVIDER_LIMIT 70
ENV REDIS_HOST localhost

ENV ROOT_LOGGING_LEVEL=INFO
ENV APP_LOGGING_LEVEL=INFO

ARG APP_PORT=18080
ENV APP_PORT=${APP_PORT}

ARG JMX_PORT=19999
ENV JMX_PORT=${JMX_PORT}
ENV JMX_OPTS="-Dcom.sun.management.jmxremote \
	-Dcom.sun.management.jmxremote.local.only=false \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.port=$JMX_PORT \
	-Dcom.sun.management.jmxremote.ssl=false"

ENV JVM_OPTS="-Xmx1G -Xms1G \
	-Xss2M \
	-XX:+TieredCompilation \
	-XX:InitialCodeCacheSize=128m \
	-XX:ReservedCodeCacheSize=128m \
	-XX:+PreserveFramePointer \
	-XX:+UnlockExperimentalVMOptions \
	-XX:+UseJVMCICompiler \
	-Xlog:gc:stdout:time"

COPY ./ /filmbag/
ARG APP_DIR=/filmbag
ENV APP_DIR=${APP_DIR}
WORKDIR $APP_DIR
RUN sh -c 'gradle clean build -Pversion=$APP_VERSION -x check'

EXPOSE ${JMX_PORT}
EXPOSE ${APP_PORT}

# RUN sh -c "apt update && DEBIAN_FRONTEND=noninteractive apt install -y netcat"
RUN apk add --update netcat-openbsd && rm -rf /var/cache/apk/*

CMD sh -c " \
    gradle flywayMigrate \
        -i \
        -Pflyway.url='$JDBC_URL' \
        -Pflyway.baselineOnMigrate=true \
    && \
    java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dlogging.level.root=$ROOT_LOGGING_LEVEL \
		-Dlogging.level.pl.ciruk=$APP_LOGGING_LEVEL \
		-Dserver.port=$APP_PORT \
		-Dspring.datasource.url=$JDBC_URL \
		-Dredis.host=$REDIS_HOST \
		-Dexternal.provider.filmrequest.url=$FILM_PROVIDER_URL \
		-Dexternal.provider.filmrequest.limit=$FILM_PROVIDER_LIMIT \
		$JMX_OPTS \
		$JVM_OPTS \
		-jar ./build/libs/filmbag-$APP_VERSION.jar"
