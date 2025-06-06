#!/usr/bin/env bash
# 用途：构建并推送docker镜像

# 安全模式
set -euo pipefail

# 通用脚本框架变量
PROGRAM=$(basename "$0")
EXITCODE=0

ALL=1
GATEWAY=0
BACKEND=0
INIT=0
INIT_RBAC=0
VERSION=latest
PUSH=0
ALL_IN_ONE=0
REGISTRY=docker.io
NAMESPACE=bkrepo
USERNAME=
PASSWORD=
SLIM_PACKAGE_PATH=
LATEST=0
BACKENDS=(repository auth generic oci helm npm pypi replication opdata)

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*}
BACKEND_DIR=$ROOT_DIR/src/backend
FRONTEND_DIR=$ROOT_DIR/src/frontend
GATEWAY_DIR=$ROOT_DIR/src/gateway
IMAGE_DIR=$ROOT_DIR/support-files/kubernetes/images

usage () {
    cat <<EOF
用法:
    $PROGRAM [OPTIONS]...

            [ --gateway             [可选] 打包gateway镜像 ]
            [ --backend             [可选] 打包backend镜像 ]
            [ --all-in-one          [可选] 打包all in one镜像]
            [ --slim-package-path   [可选] slim包路径，打包all in one镜像需要]
            [ --init                [可选] 打包init镜像 ]
            [ --init-rbac           [可选] 打包init-rbac镜像 ]
            [ -v, --version         [可选] 镜像版本tag, 默认latest ]
            [ -p, --push            [可选] 推送镜像到docker远程仓库，默认不推送 ]
            [ -l, --latest          [可选] 是否更新并推送latest tag ]
            [ -r, --registry        [可选] docker仓库地址, 默认docker.io ]
            [ -n, --namespace        [可选] docker仓库地址, 默认docker.io ]
            [ --username            [可选] docker仓库用户名 ]
            [ --password            [可选] docker仓库密码 ]
            [ -h, --help            [可选] 查看脚本帮助 ]
EOF
}

usage_and_exit () {
    usage
    exit "$1"
}

log () {
    echo "$@"
}

error () {
    echo "$@" 1>&2
    usage_and_exit 1
}

warning () {
    echo "$@" 1>&2
    EXITCODE=$((EXITCODE + 1))
}

# 解析命令行参数，长短混合模式
(( $# == 0 )) && usage_and_exit 1
while (( $# > 0 )); do
    case "$1" in
        --gateway )
            ALL=0
            GATEWAY=1
            ;;
        --backend )
            ALL=0
            BACKEND=1
            ;;
        --all-in-one )
            ALL=0
            ALL_IN_ONE=1
            ;;
        --init )
            ALL=0
            INIT=1
            ;;
        --init-rbac )
            ALL=0
            INIT_RBAC=1
            ;;
        -v | --version )
            shift
            VERSION=$1
            ;;
        -p | --push )
            PUSH=1
            ;;
        -l | --latest )
            LATEST=1
            ;;
        -r | --registry )
            shift
            REGISTRY=$1
            ;;
        -n | --namespace )
            shift
            NAMESPACE=$1
            ;;
        --username )
            shift
            USERNAME=$1
            ;;
        --password )
            shift
            PASSWORD=$1
            ;;
        --slim-package-path )
            shift
            SLIM_PACKAGE_PATH=$1
            ;;
        --help | -h | '-?' )
            usage_and_exit 0
            ;;
        -*)
            error "不可识别的参数: $1"
            ;;
        *)
            break
            ;;
    esac
    shift
done

if [[ $PUSH -eq 1 && -n "$USERNAME" ]] ; then
    docker login --username $USERNAME --password $PASSWORD $REGISTRY
    log "docker login成功"
fi

# 创建临时目录
random=$RANDOM
mkdir -p $WORKING_DIR/tmp_$random
tmp_dir=$WORKING_DIR/tmp_$random
# 执行退出时自动清理tmp目录
trap 'rm -rf $tmp_dir' EXIT TERM

# 构建all in one镜像
if [[ ($ALL -eq 1 || $ALL_IN_ONE -eq 1)  && -n "$SLIM_PACKAGE_PATH" ]] ; then
    log "构建all in one镜像..."
    rm -rf $tmp_dir/*
    cp $SLIM_PACKAGE_PATH $tmp_dir/
    docker build -f $ROOT_DIR/support-files/docker/bkrepo.Dockerfile -t $REGISTRY/$NAMESPACE/bkrepo:$VERSION $tmp_dir --no-cache --network=host
    if [[ $LATEST -eq 1 ]] ; then
        docker tag   $REGISTRY/$NAMESPACE/bkrepo:latest $REGISTRY/$NAMESPACE/bkrepo:$VERSION
    fi
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/$NAMESPACE/bkrepo:$VERSION
        if [[ $LATEST -eq 1 ]] ; then
            docker push $REGISTRY/$NAMESPACE/bkrepo:latest
        fi
    fi
fi

# 编译frontend
if [[ $ALL -eq 1 || $GATEWAY -eq 1 ]] ; then
    log "编译frontend..."
    yarn --cwd $FRONTEND_DIR install
    yarn --cwd $FRONTEND_DIR run public

    # 打包gateway镜像
    log "构建gateway镜像..."
    rm -rf $tmp_dir/*
    cp -rf $FRONTEND_DIR/frontend $tmp_dir/
    cp -rf $GATEWAY_DIR $tmp_dir/gateway
    cp -rf $IMAGE_DIR/gateway/startup.sh $tmp_dir/
    cp -rf $ROOT_DIR/scripts/render_tpl $tmp_dir/
    cp -rf $ROOT_DIR/support-files/templates $tmp_dir/
    docker build -f $IMAGE_DIR/gateway/gateway.Dockerfile -t $REGISTRY/$NAMESPACE/bkrepo-gateway:$VERSION $tmp_dir --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/$NAMESPACE/bkrepo-gateway:$VERSION
    fi
fi

# 构建backend镜像
if [[ $ALL -eq 1 || $BACKEND -eq 1 ]] ; then
    for SERVICE in "${BACKENDS[@]}";
    do
        log "构建${SERVICE}镜像..."
        if [[ $SERVICE == "preview" ]]; then
            DOCKERFILE="$IMAGE_DIR/backend/preview.Dockerfile"
        else
            DOCKERFILE="$IMAGE_DIR/backend/backend.Dockerfile"
        fi
        $BACKEND_DIR/gradlew -p $BACKEND_DIR :$SERVICE:boot-$SERVICE:build -P'devops.assemblyMode'=k8s -x test
        rm -rf $tmp_dir/*
        cp $IMAGE_DIR/backend/startup.sh $tmp_dir/
        cp $BACKEND_DIR/release/boot-$SERVICE.jar $tmp_dir/app.jar
        docker build -f $DOCKERFILE -t $REGISTRY/$NAMESPACE/bkrepo-$SERVICE:$VERSION $tmp_dir --network=host
        if [[ $PUSH -eq 1 ]] ; then
            docker push $REGISTRY/$NAMESPACE/bkrepo-$SERVICE:$VERSION
        fi
    done
    ls -a
fi

# 构建init镜像
if [[ $ALL -eq 1 || $INIT -eq 1 ]] ; then
    log "构建init镜像..."
    rm -rf $tmp_dir/*
    cp -rf $IMAGE_DIR/init/init-mongodb.sh $tmp_dir/
    cp -rf $ROOT_DIR/support-files/sql/init-data.js $tmp_dir/
     cp -rf $ROOT_DIR/support-files/sql/init-data-tenant.js $tmp_dir/
    cp -rf $ROOT_DIR/support-files/sql/init-data-ext.js $tmp_dir/
    docker build -f $IMAGE_DIR/init/init.Dockerfile -t $REGISTRY/$NAMESPACE/bkrepo-init:$VERSION $tmp_dir --no-cache --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/$NAMESPACE/bkrepo-init:$VERSION
    fi
fi

# 构建init-rbac镜像
if [[ $ALL -eq 1 || $INIT_RBAC -eq 1 ]] ; then
    log "构建init-rbac镜像..."
    rm -rf $tmp_dir/*
    mkdir -p $tmp_dir/support-files/bkiam
    cp -rf $ROOT_DIR/support-files/bkiam/* $tmp_dir/support-files/bkiam
    docker build -f init/init-rbac.Dockerfile -t $REGISTRY/bkrepo-init-rbac:$VERSION $tmp_dir --no-cache --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/bkrepo-init-rbac:$VERSION
    fi
fi

echo "BUILD SUCCESSFUL!"
