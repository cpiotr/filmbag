FROM openjdk:11-slim
MAINTAINER c.piotre@gmail.com

VOLUME /tmp

ARG APP_VERSION=0.0.1
ENV APP_VERSION=${APP_VERSION}
ENV DB_HOST 172.17.0.2
ENV FILM_PROVIDER_URL http://192.168.178.206:8080/resources/suggestions/1

ENV ROOT_LOGGING_LEVEL=INFO
ENV APP_LOGGING_LEVEL=INFO

ENV JMX_OPTS="-Dcom.sun.management.jmxremote \
	-Dcom.sun.management.jmxremote.local.only=false \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.port=12340 \
	-Dcom.sun.management.jmxremote.ssl=false"

ENV JVM_OPTS="-Xmx1G -Xms1G \
	-Xss2M \
	-XX:+TieredCompilation \
	-XX:InitialCodeCacheSize=128m \
	-XX:ReservedCodeCacheSize=128m \
	-XX:-UseBiasedLocking \
	-XX:+PreserveFramePointer \
	-XX:+UnlockExperimentalVMOptions \
	-XX:+UseJVMCICompiler \
	-Xlog:gc:stdout:time"

COPY ./ /filmbag/
WORKDIR /filmbag
RUN sh -c './gradlew clean build -Pversion=$APP_VERSION -i'

EXPOSE 12345
EXPOSE 12340

CMD sh -c "java \
		-Djava.security.egd=file:/dev/./urandom \
		-Dlogging.level.root=$ROOT_LOGGING_LEVEL \
		-Dlogging.level.pl.ciruk=$APP_LOGGING_LEVEL \
		-Dserver.port=12345 \
		-Dexternal.provider.filmrequest.url=$FILM_PROVIDER_URL \
		$JMX_OPTS \
		$JVM_OPTS \
		-jar ./build/libs/filmbag-$APP_VERSION.jar"
