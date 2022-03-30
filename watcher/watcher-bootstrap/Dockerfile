
FROM openjdk:11

# RUN cd v2ray-cli/
# RUN cp v2ray-cli/config.json /home/config.json
# RUN ln -s v2ray-cli/v2ray /bin/v2ray
# RUN ln -s v2ray-cli/v2ctl /bin/v2ctl
# RUN systemctl daemon-reload
# RUN systemctl start v2ray
RUN export https_proxy=socks5://10.233.87.225:1080


ENV TZ=Asia/Shanghai
RUN set -eux; \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime; \
    echo $TZ > /etc/timezone
RUN set -eux; \
    addgroup --gid 1000 java-app; \
    adduser --system --uid 1000 --gid 1000 --home=/home/java-app/ --shell=/bin/sh --disabled-password java-app;
USER java-app

EXPOSE 6003

ARG JAR_FILE=target/chainscan-watcher-bootstrap.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java -Xmx6144m -Xms1024m","-jar","/app.jar"]
