# maven web deploy

[toc]

## 服务模块名

module: **repository**



## 上传文件

* API: PUT /deploy/{projectId}/{repoName}/{filename}

* API 名称: deploy_artifact

- 功能说明：
  - 中文：上传制品
  - English：deploy artifact

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段      | 类型   | 默认值 | 是否必传 | 说明   | Description |
  | --------- | ------ | ------ | -------- | ------ | ----------- |
  | projectId | String | 无     | 是       | 项目名 | projectId   |
  | repoName  | String | 无     | 是       | 仓库名 | Repo name   |
  | filename  | String | 无     | 是       | 文件名 | filename    |

* 响应体

  ```json
  {
    "code" : 0,
    "message" : null,
    "data" : {
      "uuid" : "/commons-lang3-3.12.0.jar",
      "groupId" : "org.apache.commons",
      "artifactId" : "commons-lang3",
      "version" : "3.12.0",
      "classifier" : null,
      "type" : "jar"
    },
    "traceId" : ""
  }⏎
  ```

* data字段说明

  | 字段       | 类型   | 说明                               | Description |
  | ---------- | ------ | ---------------------------------- | ----------- |
  | uuid       | String | 唯一标识，需要在下一步传递给服务器 | uuid        |
  | groupId    | String | groupId                            | groupId     |
  | artifactId | String | artifactId                         | artifactId  |
  | version    | String | version                            | version     |
  | classifier | String | classifier                         | classifier  |
  | type       | String | type                               | type        |



## 确认上传信息后提交任务

* API: POST /deploy/{projectId}/{repoName}

* API 名称: verify_deploy_artifact

- 功能说明：
  - 中文：确认上传制品信息
  - English：verify deploy artifact

* 请求体

  ```json
  {
      "uuid" : "/commons-lang3-3.12.0.jar",
      "groupId" : "org.apache.commons",
      "artifactId" : "commons-lang3",
      "version" : "3.12.0",
      "classifier" : null,
      "type" : "jar"
  }
  ```

* 请求字段说明

  | 字段       | 类型   | 默认值 | 是否必传                 | 说明       | Description |
  | ---------- | ------ | ------ | ------------------------ | ---------- | ----------- |
  | projectId  | String | 无     | 是                       | 项目名     | projectId   |
  | repoName   | String | 无     | 是                       | 仓库名     | Repo name   |
  | uuid       | String | 无     | 是                       | 文件名     | filename    |
  | groupId    | String | 无     | 否（不传则由服务端解析） | groupId    | groupId     |
  | artifactId | String | 无     | 否（不传则由服务端解析） | artifactId | artifactId  |
  | version    | String | 无     | 否（不传则由服务端解析） | version    | version     |
  | classifier | String | 无     | 否（不传则由服务端解析） | classifier | classifier  |
  | type       | String | 无     | 否（不传则由服务端解析） | type       | type        |

  **`上一步接口服务返回type=pom 时，考虑实际需求，修改该字段将无效`**
  
  参数校验规则如下：`groupId`, `artifactId` 符合如下正则：`[a-zA-Z0-9_\-.]+` ; `version`不允许出现如下字符：`\/:"<>|?*[](){},`
* 响应体

  ```json
  {
    "projectId" : "test",
    "repo" : "maven",
    "created" : "2022-11-02T16:37:17.846",
    "createdBy" : "anonymous",
    "downloadUri" : "http://127.0.0.1/maven/test/maven//org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar",
    "mimeType" : "application/java-archive",
    "size" : "587402",
    "checksums" : {
      "sha1" : "c6842c86792ff03b9f1d1fe2aab8dc23aa6c6f0e",
      "md5" : "19fe50567358922bdad277959ea69545",
      "sha256" : "d919d904486c037f8d193412da0c92e22a9fa24230b9d67a57855c5c31c7e94e"
    },
    "originalChecksums" : {
      "sha256" : "d919d904486c037f8d193412da0c92e22a9fa24230b9d67a57855c5c31c7e94e"
    },
    "uri" : "http://127.0.0.1/maven/test/maven//org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar"
  }
  {
    "projectId" : "test",
    "repo" : "maven",
    "created" : "2022-11-02T16:37:18.173",
    "createdBy" : "anonymous",
    "downloadUri" : "http://127.0.0.1/maven/test/maven//org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.pom",
    "mimeType" : "application/x-maven-pom+xml",
    "size" : "30660",
    "checksums" : {
      "sha1" : "302d01a9279f7a400b1e767be60f12c02a5cf513",
      "md5" : "51f828b51a27ae904e020f679d8f8ce0",
      "sha256" : "82d31f1dcc4583effd744e979165b16da64bf86bca623fc5d1b03ed94f45c85a"
    },
    "originalChecksums" : {
      "sha256" : "82d31f1dcc4583effd744e979165b16da64bf86bca623fc5d1b03ed94f45c85a"
    },
    "uri" : "http://127.0.0.1/maven/test/maven//org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.pom"
  }
  {
    "projectId" : "test",
    "repo" : "maven",
    "created" : "2022-11-02T16:37:18.265",
    "createdBy" : "anonymous",
    "downloadUri" : "http://127.0.0.1/maven/test/maven//org/apache/commons/commons-lang3/maven-metadata.xml",
    "mimeType" : "application/xml",
    "size" : "465",
    "checksums" : {
      "sha1" : "9f8990146b09e7292993bf74a7909958ae92ca30",
      "md5" : "29078365d017e850dbfcd7d631539db6",
      "sha256" : "b853f3156dbbeb7b396a3ccfd8611f1c2d6b1ea98b8e93210b932a0dfe3ab085"
    },
    "originalChecksums" : {
      "sha256" : "b853f3156dbbeb7b396a3ccfd8611f1c2d6b1ea98b8e93210b932a0dfe3ab085"
    },
    "uri" : "http://127.0.0.1/maven/test/maven//org/apache/commons/commons-lang3/maven-metadata.xml"
  }
  ```

* 响应内容说明

  前端无需关注该内容，此返回为兼容原上传接口的数据协议。无报错即上传成功