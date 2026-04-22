FROM ubuntu:latest
LABEL authors="romario"

ENTRYPOINT ["top", "-b"]