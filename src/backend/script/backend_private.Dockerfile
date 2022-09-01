FROM alpine:latest

LABEL maintainer="Tencent BlueKing Devops"

ENV LANG="en_US.UTF-8"

RUN mkdir -p /data/workspace

COPY ./cpack/jars-private /data/workspace/cpack/jars-private
COPY ./cpack /data/workspace/cpack
COPY ./jars-public /data/workspace/jars-public

WORKDIR /data/workspace