#!/bin/sh

docker kill artifactory || true
docker rm artifactory || true
sudo docker run -d --name artifactory -p 8050:8080 --volumes-from artifactory-data kperson/artifactory
