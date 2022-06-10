# 服务部署

## scanner服务

### 配置

```yaml
# 上报的扫描结果比较大的时候需要配置
spring:
  servlet:
    multipart:
      max-request-size: 100MB

scanner:
  # 支持扫描的文件名后缀
  supportFileNameExt:
    - apk
    - ipa
    - jar
  # 用于生成扫描报告路径
  detailReportUrl: http://localhost
  spring:
    data:
      # 用于存储详细扫描报告
      mongodb:
        uri: mongoUri
```

## scanner-executor服务

### 配置

```yaml
scanner:
  executor:
    # 扫描执行器工作目录
    workDir: /work_dir
    # 单台机器最多执行的扫描任务数量
    maxTaskCount: 10
    # 允许扫描的最大文件大小
    fileSizeLimit: 10GB
    # 至少需要空闲的内存百分比，大于这个比例才能执行扫描任务
    atLeastFreeMemPercent: 0.2
    # 至少需要空闲的硬盘空间百分比，大于这个比例才能执行扫描任务
    atLeastUsableDiskSpacePercent: 0.3
    docker:
      enabled: true
      host: unix:///var/run/docker.sock
      version: 1.23
      connect-timeout: 5000
      read-timeout: 0
```

### 依赖

- docker daemon
- 扫描器依赖容器镜像时机器上要有对应的容器镜像，不存在镜像但是可以拉取到时scanner-executor服务会自动拉取
- 运行服务器最好是8C/16G 以上的配置，因为扫描器目前还比较消耗资源