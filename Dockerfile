FROM ubuntu:14.04

#BASE
RUN apt-get update
RUN apt-get install -y python-software-properties software-properties-common
RUN apt-get install -y vim git wget libfreetype6 libfontconfig bzip2  build-essential
RUN apt-get upgrade -y
RUN apt-get install -y apparmor

#Java
RUN \
  echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java7-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk7-installer

ENV JAVA_HOME /usr/lib/jvm/java-7-oracle

#SBT
RUN wget -O sbt.deb https://bintray.com/artifact/download/sbt/debian/sbt-0.13.9.deb
RUN dpkg -i sbt.deb

#SERVER Code
WORKDIR /
ADD . /code
WORKDIR /code
RUN compile stage
cd /demo-simple
COMMAND target/target
EXPOSE 9090
