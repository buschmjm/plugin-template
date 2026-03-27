FROM eclipse-temurin:25-jre-noble

# Fixed UID/GID — matches everhytale image for PVC permission compatibility
RUN groupadd -r -g 1001 hytale && \
    useradd -r -u 1001 -g hytale -m -d /server hytale

# Game files go to /opt/hytale-server (read-only, baked into image)
# Server data goes to /server (persistent volume mounted by k8s)
COPY --chown=hytale:hytale Server/ /opt/hytale-server/
COPY --chown=hytale:hytale Assets.zip /opt/hytale-server/Assets.zip

RUN mkdir -p /server && chown hytale:hytale /server

WORKDIR /server
USER 1001

EXPOSE 5520/udp
VOLUME ["/server"]

ENTRYPOINT ["java", \
    "-Xmx16G", \
    "-XX:+UseG1GC", \
    "-XX:+ParallelRefProcEnabled", \
    "-XX:+AlwaysPreTouch", \
    "-jar", "/opt/hytale-server/HytaleServer.jar", \
    "--assets", "/opt/hytale-server/Assets.zip"]
