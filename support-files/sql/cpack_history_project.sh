#!/bin/bash

# e.g. sh cpack_history_project.sh admin bkrepo http://bkrepo.com https://devops.canway.net
# admin    制品库管理员账号
# bkrepo   制品库管理员账号密码
# http://bkrepo.com    制品库地址
# https://devops.canway.net   devops地址

username=$1
password=$2
cpackUrl=$3
devopsUrl=$4
curl -k -s -O $devopsUrl/ms/project/api/service/projects/getAllProject

cat getAllProject | grep '"englishName"' | awk -F'"' '{print $4}' > projects

project_list=($(awk '{print $0}' projects)) 

addCustomRepository(){
    curl -k -X POST  -u$username:$password $cpackUrl/repository/api/repo/create \
    -H 'Content-Type: application/json' \
    -d '{ "projectId": "'$1'", "name":"custom", "type":"GENERIC", "category": "LOCAL", "description":"create by ci" }'
}
addPipelineRepository(){
    curl -k -X POST  -u$username:$password $cpackUrl/repository/api/repo/create \
    -H 'Content-Type: application/json' \
    -d '{ "projectId": "'$1'", "name":"pipeline", "type":"GENERIC", "category": "LOCAL", "description":"create by ci" }'
}
addReportRepository(){
    curl -k -X POST  -u$username:$password $cpackUrl/repository/api/repo/create \
    -H 'Content-Type: application/json' \
    -d '{ "projectId": "'$1'", "name":"report", "type":"GENERIC", "category": "LOCAL", "description":"create by ci" }'
}
addDockerRepository(){
    curl -k -X POST  -u$username:$password $cpackUrl/repository/api/repo/create \
    -H 'Content-Type: application/json' \
    -d '{ "projectId": "'$1'", "name":"docker-local", "type":"DOCKER", "category": "LOCAL", "description":"create by ci" }'
}

addProject(){
    curl -k -X POST  -u$username:$password $cpackUrl/repository/api/project/create \
    -H 'Content-Type: application/json' \
    -d '{ "name": "'$1'", "displayName":"'$1'", "description":"create by ci" }'
}

isExistProject(){
    result=$(curl -k -s -X GET -u$username:$password $cpackUrl/repository/api/project/exist/$1 | grep data | awk -F'[ ,]' '{print $5}')
    if [ "true" == "$result" ]
    then 
        return 0
    else 
        return 1
    fi
}

for project in ${project_list[@]}
do
    isExistProject $project
    exist=$?
    if [ $exist == 0 ]
    then 
        echo "\n project : $project exist"
    else
        echo "\n project : $project start "
        addProject $project
        addCustomRepository $project
        addPipelineRepository $project
        addReportRepository $project
        addDockerRepository $project
    fi
done