FROM openjdk:8-jdk-alpine
ARG JAR_FILE=dist/JWT_test.jar
ARG JARLIB_FILES=dist/lib
COPY src.zip /opt/src.zip
COPY ${JAR_FILE} /opt/JWT_test.jar
COPY ${JARLIB_FILES} /opt/lib
ENTRYPOINT ["/usr/lib/jvm/java-1.8-openjdk/jre/bin/java","-jar","/opt/JWT_test.jar"]
EXPOSE 8080/tcp
