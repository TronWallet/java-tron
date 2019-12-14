FROM debian:stretch

ENV TRON_PATH=/opt/tron

USER root

WORKDIR ${TRON_PATH}

# setup environment dependencies
RUN apt-get update \
    && apt-get install -y openjdk-8-jre openjfx curl \
    && rm -rf /var/lib/apt/lists/*

# adds tron user and fix tron folder's permission
RUN	groupadd -g 1000 tron \
    && useradd -r -u 1000 -g 1000 tron --home ${TRON_PATH} \
    && chown -R tron:tron ${TRON_PATH}

USER tron

RUN curl -LO https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/private_net_config.conf 

COPY --chown=tron:tron build/libs/FullNode.jar build/libs/SolidityNode.jar ${TRON_PATH}/

CMD java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -jar FullNode.jar --witness -c ${TRON_PATH}/private_net_config.conf