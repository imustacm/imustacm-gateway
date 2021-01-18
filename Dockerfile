FROM java:8

ADD target/imustacm-gateway-1.0-SNAPSHOT.jar /server/app.jar
WORKDIR /server
ENV JAVA_OPTS=""
EXPOSE 8888
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Xms300m -Xmx450m -jar /server/app.jar" ]
