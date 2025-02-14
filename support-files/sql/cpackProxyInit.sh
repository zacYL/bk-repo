#!/bin/bash

#使用方式：sh repoProxyInit.sh {username} {password} {http://bkrepo.com}   必须使用管理员账号
#     例：sh repoProxyInit.sh admin bkrepo http://bkrepo.com/repository
#将需要添加的依赖源加在proxy_array中，格式：{公有源名}^{仓库类型}^{公有源地址}。
       

url=$3
username=$1
password=$2

proxy_array=(
    "central^NPM^http://npm.org"
    "aliyun^NPM^https://registry.npmmirror.com"
    "tencent^MAVEN^http://mirrors.cloud.tencent.com/nexus/repository/maven-public"
    "aliyun^MAVEN^https://maven.aliyun.com"
    "center^PYPI^https://pypi.org"
)

addProxy(){
    curl -k -X POST  -u$username:$password "$url"/repository/api/proxy-channel \
    -H 'Content-Type: application/json' \
    -d '{ "public":true, "name":"'$4'", "repoType":"'$5'", "url": "'$6'"}'
}

for proxy in ${proxy_array[@]}
do
    echo "\n start add proxy : $proxy"
    array=(${proxy//^/ })
    addProxy  $username $password $url ${array[0]} ${array[1]} ${array[2]}
done

