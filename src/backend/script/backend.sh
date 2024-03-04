#!/bin/bash

MEM_OPTS="-XX:+UseContainerSupport -XX:InitialRAMPercentage=75.0 -XX:MaxRAMPercentage=75.0 -XX:-UseAdaptiveSizePolicy"
API_PORT=80

echo "create log dir"
ci_ms_log="/data/logs"
ci_ms_data="/data/local/"
mkdir -p "$ci_ms_log" "$ci_ms_data"
ln -srfT "$ci_ms_log" logs
ln -srfT "$ci_ms_data" data

echo "create java args"
java_env=() java_argv=() java_run="" JAVA_OPTS=${JAVA_TOOL_OPTIONS:-}
java_argv+=("-Dfatjar=/data/workspace/$MS_NAME/boot-$MS_NAME.jar") # 兼容fatjar文件名匹配进程.
java_run="org.springframework.boot.loader.JarLauncher"
for k in LANG USER HOME SHELL LOGNAME PATH HOSTNAME LD_LIBRARY_PATH ${!JAVA_*} ${!SPRING_*}; do
if [ -n "${!k-}" ]; then java_env+=("$k=${!k}"); fi # 如果定义, 则传递.
done
java_argv+=(
  "-Ddevops_gateway=$DEVOPS_GATEWAY"
  "-Dserver.port=$API_PORT" # 强制覆盖配置文件里的端口.
  "-Dbksvc=bk-ci-$MS_NAME"
  "-Dspring.cloud.kubernetes.config.sources[0].name=${SERVICE_PREFIX:-bkci}bkrepo-common"
  "-Dspring.cloud.kubernetes.config.sources[1].name=${SERVICE_PREFIX:-bkci}${MS_NAME}"
  "-Dservice-suffix="
  "-Dspring.main.allow-bean-definition-overriding=true"
  "-Dspring.application.name=$MS_NAME"
  "-Ddevops.service.mode=kubernetes"
  "-Dspring.profiles.active=dev,kubernetes"
)

echo "java_run  $java_run "
echo  "JAVA_OPTS $JAVA_OPTS "
echo  "MEM_OPTS $MEM_OPTS "
echo  "java_argv ${java_argv[@]} "
java -server "${java_argv[@]}" $MEM_OPTS $JAVA_OPTS $java_run