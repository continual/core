
FROM openjdk:11

# some basic coordinates
ARG APPUSER=continual
ARG APPUSERDIR=/home/${APPUSER}/
ARG BASEAPPDIR=/opt/app
ARG APPNAME=continualrcvr
ARG PKG_NAME=continualRcvr
ARG PKG_VERSION=1.0.0-SNAPSHOT

# setup a user id and while still root, setup the base appdir
RUN useradd -m -d ${APPUSERDIR} -s /bin/bash -G sudo ${APPUSER} \
    && mkdir -p ${BASEAPPDIR}

# updates and tools
RUN apt-get update \
	&& apt-get -y install vim \
	&& apt-get -y install jq 

# copy in the deliverable and unpack it
COPY --chown=${APPUSER}:${APPUSER} target/${PKG_NAME}-${PKG_VERSION}-bundle.tar.gz /tmp/${PKG_NAME}-${PKG_VERSION}-bundle.tar.gz
RUN tar zxf /tmp/${PKG_NAME}-${PKG_VERSION}-bundle.tar.gz -C ${BASEAPPDIR} \
	&& rm /tmp/${PKG_NAME}-${PKG_VERSION}-bundle.tar.gz \
	&& mkdir ${BASEAPPDIR}/${PKG_NAME}-${PKG_VERSION}/logs \
	&& chown -R ${APPUSER}:${APPUSER} ${BASEAPPDIR}/${PKG_NAME}-${PKG_VERSION} \
	&& ln -s ${BASEAPPDIR}/${PKG_NAME}-${PKG_VERSION} ${BASEAPPDIR}/${PKG_NAME}

# setup the working directory
WORKDIR ${BASEAPPDIR}/${PKG_NAME}-${PKG_VERSION}

# expose the service port
EXPOSE 8080

# use our app user to run the software
USER ${APPUSER}

# some shell setup
RUN echo "" >> ~/.bashrc \
	&& echo "alias ll='ls -l'" >> ~/.bashrc \
	&& echo "alias l='ls -lA'" >> ~/.bashrc

# startup
ENTRYPOINT [ "./bin/rcvr.sh" ]
