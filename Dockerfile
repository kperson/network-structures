FROM ubuntu:14.04

#Base
RUN apt-get update
RUN apt-get install -y python-software-properties software-properties-common vim git wget libfreetype6 libfontconfig bzip2 build-essential
RUN apt-get upgrade -y
RUN apt-get install -y apparmor

#Java 7
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

#Server Code
EXPOSE 8080
WORKDIR /
ADD . /code
WORKDIR /code
RUN sbt 'project hub' clean compile stage
CMD ["hub/target/universal/stage/bin/hub", "--host", "0.0.0.0", "--port", "8080"]