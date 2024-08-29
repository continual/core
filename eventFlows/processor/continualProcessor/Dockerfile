
FROM eclipse-temurin:11-jdk-jammy

RUN useradd -m -d /home/continualio -s /bin/bash -G sudo continualio \
	&& mkdir -p /opt/continualio \
	&& chown -R continualio:continualio /opt/continualio \
	&& mkdir -p /var/opt/continualio \
	&& chown -R continualio:continualio /var/opt/continualio

RUN apt-get update \
	&& apt-get -y install dnsutils \
	&& apt-get -y install jq \
	&& apt-get -y install net-tools \
	&& apt-get -y install vim

WORKDIR /opt/continualio

USER continualio

RUN echo "" >> /home/continualio/.bashrc \
    && echo "alias ll='ls -la'" >> /home/continualio/.bashrc

ENTRYPOINT [ "/bin/bash" ]
