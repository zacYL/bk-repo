#!/bin/bash

# e.g. sh cpack_permission_authentication_update.sh https://devops.canway.net
# https://devops.canway.net   devops地址

devopsUrl=$1
bkrepoUrl=${devopsUrl/devops/bkrepo}

# 删除平台资源接口
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

deleteArtifactoryResource 967b804ed6a740ce9fda257c7d0a74bc
deleteArtifactoryResource e7cc790661444727ae6d2cb52e6705f8

# 删除平台菜单接口
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

deleteArtifactoryMenu 311
deleteArtifactoryMenu 406

# 历史用户增加初始密码 Bk@123456
curl -k -X  POST $devopsUrl/ms/usermanager/api/service/user/initialPassword

# 修改admin账号的密码
curl -k -X POST $devopsUrl/ms/usermanager/api/service/authentication/password/update \
-H 'X-DEVOPS-UID: admin' \
-H 'Content-Type: application/json' \
-d '{
    "username":"admin",
    "password":"bkrepo"
}'

#token迁移
curl -k -X POST $bkrepoUrl/auth/api/token/migrate \
-H 'Authorization: Basic YWRtaW46YmtyZXBv'

#权限数据迁移
curl -k -X POST $bkrepoUrl/auth/api/permission/migrateToDevOps \
-H 'Authorization: Basic YWRtaW46YmtyZXBv'