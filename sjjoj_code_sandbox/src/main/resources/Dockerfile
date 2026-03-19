FROM openjdk:8-jdk

RUN apt-get update \
    && apt-get install -y python3 gcc g++ \
    && rm -rf /var/lib/apt/lists/*

ARG VERSION=""
ENV JAVA_OPTS=""
ENV PARAMS=""

RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo 'Asia/Shanghai' >/etc/timezone

ADD ./oj-code-sandbox-0.0.1-SNAPSHOT.jar /app.jar

ENTRYPOINT ["sh","-c","java $JAVA_OPTS $PARAMS -jar /app.jar $PARAMS"]
