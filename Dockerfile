FROM frolvlad/alpine-java:jdk8-full

ENV TRON_PATH=/opt/java-tron

WORKDIR ${TRON_PATH}

USER root

# installs dependencies packages
RUN apk update \
    && apk -U upgrade \
    && apk add --no-cache --update libstdc++ curl ca-certificates bc \
    && update-ca-certificates --fresh \
    && rm -rf /var/cache/apk/*

# adds tron user and fix tron folder's permission
RUN	adduser -S tron -u 1000 -G root \
    && chown -R tron:root ${TRON_PATH}

USER tron

COPY --chown=tron:root build/libs/FullNode.jar build/libs/SolidityNode.jar ${TRON_PATH}/

COPY --chown=tron:root start.sh ./
RUN	chmod +x ${TRON_PATH}/start.sh \
    && curl -LO https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/main_net_config.conf

ENTRYPOINT [ "sh" ]
CMD [ "start.sh" ]