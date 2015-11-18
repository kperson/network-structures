#!/bin/sh

cd /vagrant

#sbt repos
sudo cp repositories.sample repositories
sudo cp .sbtopts.sample .sbtopts

#artifactory
#create docker data volume
sudo docker rm artifactory-data || true
sudo docker run -d -v /artifactory/backup -v /artifactory/data -v /artifactory/logs --name artifactory-data busybox echo 'artifactory data volume'

#if a backup file exists, load into the volume created above
if [ -e "artifactory.tar" ]; then
  sudo docker run --volumes-from artifactory-data -v $(pwd):/backup busybox bin/sh -c "cd /artifactory && tar xvf /backup/artifactory.tar"
else
  sudo cp artifactory-base.tar artifactory.tar
  sudo docker run --volumes-from artifactory-data -v $(pwd):/backup busybox bin/sh -c "cd /artifactory && tar xvf /backup/artifactory.tar"
fi

#start docker container
sudo docker kill artifactory || true
sudo docker rm artifactory || true
sudo docker run -d --name artifactory -p 8050:8080 --volumes-from artifactory-data kperson/artifactory