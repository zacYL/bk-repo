# trivy 扫描器说明



trivy主要是针对docker 镜像制品漏洞进行扫描。



> trivy工具能力说明：
>
> - trivy支持扫描指定镜像文件
> - trivy扫描镜像文件时，不支持解析V2版本的manifest.json文件
> - trivy支持指定缓存目录（trivy镜像漏洞库目录）
> - trivy支持跳过更新缓存目录
> - trivy支持扫描结果以json格式输出到指定目录

## trivy扫描工具扫描逻辑

1. 加载待扫描文件
   - scanner服务基于镜像的manifest.json(V2)文件生成对应镜像制品的扫描任务
   - scanner获取镜像任务，下载并解析manifest.json(V2)获取制品的所有layers
   - 生成manifest.json(V1)文件
   - 根据manifest.json(V1)及layers打包生成待扫描文件（镜像包.tar）
2. 执行扫描
   - 拉取trivy镜像扫描工具
   - 从默认仓库获取最新trivy镜像漏洞库trivy.db文件
   - 生成扫描指定制品的trivy容器
   - 运行容器进行扫描
   - 获取并解析扫描结果文件



## 创建扫描器说明

- API: POST /scanner/api/scanners
- API 名称: create_scanner
- 功能说明：
  - 中文：创建扫描器
  - English：create scanner
- 创建trivy扫描器请求体

```json
{
  "name": "default",
  "version": "1::1",
  "type": "trivy",
  "cacheDir": "/data/trivy/cache",
  "rootPath": "/data/trivy",
  "cleanWorkDir": false,
  "container": {
    "image": "aquasec/trivy:0.19.2",
    "workDir": "/data",
    "inputDir": "/package",
    "outputDir": "/output"
  }
}
```

- 请求字段说明
  详情参考[支持的扫描器](./supported-scanner.md)

| 字段                | 类型    | 是否必须 | 默认值   | 说明                                                         | Description               |
| ------------------- | ------- | -------- | -------- | ------------------------------------------------------------ | ------------------------- |
| name                | string  | 是       | 无       | 扫描器名                                                     | scanner name              |
| version             | string  | 是       | 无       | 扫描器版本，arrowhead扫描器版本和漏洞库版本用::分隔          | scanner version           |
| type                | string  | 是       | 无       | 扫描器类型                                                   | scanner type              |
| cacheDir            | string  | 是       | 无       | 指定trivy工具缓存目录，tiryv.db存储在相对缓存缓存目录的./db/trivy.db | scanner type              |
| rootPath            | string  | 是       | 无       | 扫描器工作根目录                                             | scanner work dir          |
| maxScanDuration     | number  | 否       | 600000   | 扫描超时时间，单位为毫秒                                     | max scan duration         |
| cleanWorkDir        | boolean | 否       | true     | 扫描结束后是否清理目录                                       | clean work dir after scan |
| container.image     | string  | 是       | 无       | 使用的arrowhead镜像tag                                       | arrowhead image tag       |
| container.workDir   | string  | 否       | /data    | 容器内工作目录根目录                                         | work dir                  |
| container.inputDir  | string  | 否       | /package | 容器内扫描时的输入目录，相对于工作目录                       | input dir                 |
| container.outputDir | string  | 否       | /output  | 容器内扫描时的输出目录，相对于工作目录                       | output dir                |



- 响应体

```json
{
    "code": 0,
    "message": null,
    "data": {
        "name": "trivyScanner",
        "version": "1::1",
        "cacheDir": "/data/trivy/cache",
        "rootPath": "/data/trivy",
        "cleanWorkDir": false,
        "container": {
            "image": "aquasec/trivy:0.19.2",
            "args": "",
            "workDir": "/data",
            "inputDir": "/package",
            "outputDir": "/output"
        },
        "type": "trivy",
        "maxScanDurationPerMb": 6000
    },
    "traceId": ""
}
```



## trivy安装使用注意事项

- scanner-executor服务所在服务器需要安装docker
- 需要通过 POST /scanner/api/scanners 接口注册trivy类型的扫描器
- 下载最新镜像漏洞库文件[trivy.db.gz](https://github.com/aquasecurity/trivy-db/releases)，解压获取triyv.db，重命名为trivy.db带版本文件（如trivy.db-2022-06-29）上传至镜像漏洞库管理（默认二进制仓库），即上传至public-global项目的vuldb-repo仓库trivy目录下。trivy扫描镜像时，会获取最新上传的trivy.db作为镜像漏洞库去扫描。



