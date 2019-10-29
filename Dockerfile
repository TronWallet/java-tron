FROM tronwallet/tron-gradle

# keeping everything updated
RUN apk -U upgrade && \
    apk add --no-cache --update bash curl wget \
        tar tzdata iputils unzip findutils git gettext gdb lsof patch \
        libcurl libxml2 libxslt openssl-dev zlib-dev \
        make automake gcc g++ binutils-gold linux-headers paxctl libgcc libstdc++ \
        python gnupg ncurses-libs ca-certificates && \
    update-ca-certificates --fresh && \
    rm -rf /var/cache/apk/*

# home folder
ENV HOME=/opt/java-tron
RUN	mkdir -p ${HOME} && \
    adduser -s /bin/sh -u 1001 -G root -h ${HOME} -S -D default && \
    chown -R 1001:0 ${HOME}

WORKDIR ${HOME}

RUN curl -LO https://github.com/TronWallet/java-tron/releases/download/v3.6.5/FullNode.jar \
    && curl -LO https://github.com/TronWallet/java-tron/releases/download/v3.6.5/SolidityNode.jar \
    && curl -LO https://raw.githubusercontent.com/tronprotocol/tron-deployment/master/main_net_config.conf

COPY start.sh ${HOME}/start.sh
RUN	chmod +x ${HOME}/start.sh

USER 1001

CMD ${HOME}/start.sh