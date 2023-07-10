#!/bin/bash

# e.g. sh cpack_menu_resources_update.sh https://devops.canway.net
# https://devops.canway.net   devops地址

devopsUrl=$1

deleteArtifactoryResource(){
    curl -k -X POST $devopsUrl/ms/auth/api/service/custom/migration/resource/delete \
    -H 'accept: */*' \
    -H 'X-DEVOPS-UID: admin' \
    -H 'Content-Type: application/json' \
    -d '{
      "resourceIds": [
        "'$1'"
      ],
      "physicalDelete": true
    }'
}

deleteArtifactoryMenu(){
  curl -k -X POST $devopsUrl/ms/auth/api/service/custom/migration/menu/delete \
  -H 'accept: */*' \
  -H 'X-DEVOPS-UID: admin' \
  -H 'Content-Type: application/json' \
  -d '{
    "menuIds": [
      '$1'
    ],
    "physicalDelete": true
  }'
}

deleteArtifactoryResource 967b804ed6a740ce9fda257c7d0a74bc
deleteArtifactoryResource e7cc790661444727ae6d2cb52e6705f8

deleteArtifactoryMenu 311
deleteArtifactoryMenu 406

curl -k -X POST $devopsUrl/ms/auth/api/service/custom/migration/resource/delete \
    -H 'accept: */*' \
    -H 'X-DEVOPS-UID: admin' \
    -H 'Content-Type: application/json' \
    -d '{
      "resourceIds": [
        "e7cc790661444727ae6d2cb52e6705f8"
      ],
      "physicalDelete": true
    }'