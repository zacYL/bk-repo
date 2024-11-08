# 查询maven包版本详情
功能描述：查询maven包版本详情



## 请求

#### 接口方法

`GET`

#### 接口地址

`/api/open/CPack/repo-maven/ext/version/detail/{projectId}/{repoName}`

#### 请求头

| 字段                  | 类型   | 必填 | 描述               |
| --------------------- | ------ | ---- | ------------------ |
| X-DEVOPS-ACCESS-TOKEN | String | 是   | Devops用户访问令牌 |

#### 路径参数

| 字段      | 类型   | 必填 | 描述     |
| --------- | ------ | ---- | -------- |
| projectId | String | 是   | 项目ID   |
| repoName  | String | 是   | 仓库名称 |

#### 查询参数

| 字段       | 类型   | 必填 | 描述      |
| ---------- | ------ | ---- | --------- |
| packageKey | String | 是   | 包唯一key |
| version    | String | 是   | 包版本    |

#### 请求体

无

#### 请求示例

```bash
curl -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' \
    'https://devops.example.com/api/open/CPack/repo-maven/ext/version/detail/{projectId}/{repoName}?packageKey=gav://org.apache.commons:commons-lang3&version=3.12.0'
```



## 响应

#### 响应示例

```json
{
  "code": 0,
  "message": null,
  "data": {
    "basic": {
      "groupId": "com.tencent.bk.devops.atom",
      "artifactId": "bksdk",
      "version": "1.0.0",
      "size": 42786,
      "fullPath": "/com/tencent/bk/devops/atom/bksdk/1.0.0/bksdk-1.0.0.jar",
      "lastModifiedBy": "anonymous",
      "lastModifiedDate": "2020-09-27T14:43:06.083",
      "downloadCount": 10,
      "sha256": "e8e3e3b50daf0c2638a69e0b6ca3a1eee0b367bcabef5ae5bd7983700b9a6d58",
      "md5": "358d9a0f68b641bf0a25289d1795022b",
      "stageTag": [
        "@release"
      ],
      "description": null
    },
    "metadata": {}
  },
  "traceId": ""
}
```

#### 响应体

| 字段      | 说明     |
|---------|--------|
| code    | 返回码    |
| message | 错误信息   |
| data    | 数据     |
| traceId | 链路追踪id |

##### data 字段说明

basic: 基础信息

| 字段               | 说明                 |
|------------------|--------------------|
| groupId          | groupId            |
| artifactId       | artifactId         |
| version          | version            |
| size             | 节点大小               |
| fullPath         | 节点完整路径             |
| sha256           | 节点sha256           |
| md5              | 节点md5              |
| stageTag         | 晋级状态标签             |
| downloadCount    | 下载次数               |
| lastModifiedBy   | 上次修改者              |
| lastModifiedDate | 上次修改时间             |
| description      | 描述信息               |
