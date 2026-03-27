FROM eclipse-temurin:25-jre-noble

RUN groupadd -g 1001 hytale && \
    useradd -r -u 1001 -g hytale hytale

WORKDIR /server

COPY Server/ /server/
COPY Assets.zip /server/

RUN chown -R hytale:hytale /server

USER 1001

EXPOSE 5520/udp

ENTRYPOINT ["java", "-Xmx16G", "-XX:AOTCache=HytaleServer.aot", "-jar", "HytaleServer.jar", "--assets", "Assets.zip"]
