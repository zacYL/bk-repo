# Maven 依赖项接口

[toc]

## 服务模块名

module: **maven**



## 查询包的依赖项

* API: GET /ext/dependencies/{projectId}/{repoName}?packageKey={packageKey}&version={version}&pageNumber={pageNumber}&pageSize={pageSize}

* API 名称: artifact_dependencies

- 功能说明：
  - 中文：查询包的依赖项
  - English：artifact dependencies

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 默认值 | 是否必传 | 说明         | Description |
  | ---------- | ------ | ------ | -------- | ------------ | ----------- |
  | projectId  | String | 无     | 是       | 项目名       | projectId   |
  | repoName   | String | 无     | 是       | 仓库名       | Repo name   |
  | packageKey | String | 无     | 是       | 包唯一标识符 | packageKey  |
  | version    | String | 无     | 是       | 包版本       | version     |
  | pageNumber | Int    | 1      | 否       | 页数         | page number |
  | pageSize   | Int    | 20     | 否       | 每页数据数   | Page size   |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "pageNumber": 1,
          "pageSize": 20,
          "totalRecords": 13,
          "totalPages": 1,
          "records": [
              {
                  "groupId": "org.springframework.data",
                  "artifactId": "spring-data-redis",
                  "version": "1.8.6.RELEASE",
                  "type": "jar",
                  "scope": "provided",
                  "classifier": null,
                  "optional": true
              },
              {
                  "groupId": "io.javaslang",
                  "artifactId": "javaslang",
                  "version": null,
                  "type": "jar",
                  "scope": "provided",
                  "classifier": null,
                  "optional": false
              }
          ],
          "page": 1,
          "count": 13
      },
      "traceId": ""
  }
  ```

* data字段说明

  | 字段       | 类型    | 说明       | Description |
  | ---------- | ------- | ---------- | ----------- |
  | groupId    | String  | groupId    | groupId     |
  | artifactId | String  | artifactId | artifactId  |
  | version    | String  | version    | version     |
  | classifier | String  | classifier | classifier  |
  | type       | String  | type       | type        |
  | scope      | String  | scope      | scope       |
  | optional   | boolean | optional   | optional    |



## 查询包的依赖插件

* API: GET /ext/plugins/{projectId}/{repoName}?packageKey={packageKey}&version={version}&pageNumber={pageNumber}&pageSize={pageSize}

* API 名称: artifact_plugins

- 功能说明：
  - 中文：查询包的依赖插件
  - English：artifact plugins

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 默认值 | 是否必传 | 说明         | Description |
  | ---------- | ------ | ------ | -------- | ------------ | ----------- |
  | projectId  | String | 无     | 是       | 项目名       | projectId   |
  | repoName   | String | 无     | 是       | 仓库名       | Repo name   |
  | packageKey | String | 无     | 是       | 包唯一标识符 | packageKey  |
  | version    | String | 无     | 是       | 包版本       | version     |
  | pageNumber | Int    | 1      | 否       | 页数         | page number |
  | pageSize   | Int    | 20     | 否       | 每页数据数   | Page size   |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "pageNumber": 1,
          "pageSize": 20,
          "totalRecords": 5,
          "totalPages": 1,
          "records": [
              {
                  "groupId": "org.apache.maven.plugins",
                  "artifactId": "maven-compiler-plugin",
                  "version": "3.5.1"
              },
              {
                  "groupId": "org.apache.maven.plugins",
                  "artifactId": "maven-source-plugin",
                  "version": null
              }
       
          ],
          "page": 1,
          "count": 5
      },
      "traceId": ""
  }
  ```

* data字段说明

  | 字段       | 类型   | 说明            | Description |
  | ---------- | ------ | --------------- | ----------- |
  | groupId    | String | groupId         | groupId     |
  | artifactId | String | artifactId      | artifactId  |
  | version    | String | version(可为空) | version     |



## 查询仓库中依赖该制品的制品列表

* API: GET /ext/dependencies/reverse/{projectId}/{repoName}?packageKey={packageKey}&version={version}&pageNumber={pageNumber}&pageSize={pageSize}

* API 名称: artifact_dependencies_reverse

- 功能说明：
  - 中文：查询依赖该制品的制品列表
  - English：artifact dependencies reverse

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段       | 类型   | 默认值 | 是否必传 | 说明         | Description |
  | ---------- | ------ | ------ | -------- | ------------ | ----------- |
  | projectId  | String | 无     | 是       | 项目名       | projectId   |
  | repoName   | String | 无     | 是       | 仓库名       | Repo name   |
  | packageKey | String | 无     | 是       | 包唯一标识符 | packageKey  |
  | version    | String | 无     | 是       | 包版本       | version     |
  | pageNumber | Int    | 1      | 否       | 页数         | page number |
  | pageSize   | Int    | 20     | 否       | 每页数据数   | Page size   |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "pageNumber": 1,
          "pageSize": 20,
          "totalRecords": 1,
          "totalPages": 1,
          "records": [
              {
                  "projectId": "test",
                  "repoName": "maven",
                  "packageKey": "gav://com.tencent.bk.devops.atom:bksdk",
                  "version": "1.0.0",
                  "dependencies": null
              }
          ],
          "page": 1,
          "count": 1
      },
      "traceId": ""
  }
  ```

* data字段说明

  | 字段         | 类型         | 说明                     | Description |
  | ------------ | ------------ | ------------------------ | ----------- |
  | projectId    | String       | 项目名                   | projectId   |
  | repoName     | String       | 仓库名                   | repo name   |
  | packageKey   | String       | 包版本唯一识别符         | package key |
  | version      | String       | 版本                     | version     |
  | dependencies | List<String> | 依赖项（这里统一返回空） | scope       |

