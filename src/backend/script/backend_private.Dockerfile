FROM alpine:latest

LABEL maintainer="Tencent BlueKing Devops"

ENV LANG="en_US.UTF-8"

RUN mkdir -p /data/workspace

COPY ./cpack /data/workspace/cpack

WORKDIR /data/workspace