# 查询docker镜像版本详情
功能描述：查询docker镜像版本详情



## 请求

#### 接口方法

`GET`

#### 接口地址

`/api/open/CPack/repo-oci/ext/version/detail/{projectId}/{repoName}`

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

| 字段         | 类型     | 必填  | 描述     |
|------------|--------|-----|--------|
| packageKey | String | 是   | 包唯一key |
| version    | String | 是   | 包版本    |

#### 请求体

无

#### 请求示例

```bash
curl -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' \
    'https://devops.example.com/api/open/CPack/repo-oci/ext/version/detail/{projectId}/{repoName}?packageKey=docker://alpine&version=latest'
```



## 响应

#### 响应示例

```json
{
  "code":0,
  "message":null,
  "data":{
    "basic":{
      "domain":"bkrepo.example.com",
      "size":2487,
      "version":"v1",
      "createdBy":"owen",
      "createdDate":"2020-09-17 03:48:42.896Z",
      "lastModifiedBy":"admin",
      "lastModifiedDate":"2020-09-10T14:49:37.904",
      "downloadCount":0,
      "sha256":"fce289e99eb9bca977dae136fbe2a82b6b7d4c372474c9235adc1741675f587e",
      "os":"linux"
    },
    "history":[
      {
        "created":"2019-01-01T01:29:27.416803627Z",
        "created_by":"/bin/sh -c #(nop) COPY file:f77490f70ce51da25bd21bfc30cb5e1a24b2b65eb37d4af0c327ddc24f0986a6 in / "
      },
      {
        "created":"2019-01-01T01:29:27.650294696Z",
        "created_by":"/bin/sh -c #(nop)  CMD [\"/hello\"]"
      }
    ],
    "metadata":{
      "docker.manifest":"v1",
      "sha256":"92c7f9c92844bbbb5d0a101b22f7c2a7949e40f8ea90c8b3bc396879d95e899a",
      "docker.repoName":"hello-world",
      "docker.manifest.digest":"sha256:92c7f9c92844bbbb5d0a101b22f7c2a7949e40f8ea90c8b3bc396879d95e899a",
      "docker.manifest.type":"application/vnd.docker.distribution.manifest.v2+json"
    },
    "layers":[
      {
        "mediaType":"application/vnd.docker.image.rootfs.diff.tar.gzip",
        "size":977,
        "digest":"sha256:1b930d010525941c1d56ec53b97bd057a67ae1865eebf042686d2a2d18271ced"
      }
    ]
  },
  "traceId":""
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

basic: 基础数据

| 字段       | 描述     |
|----------|--------|
| basic    | 基础数据   |
| history  | 镜像构建历史 |
| metadata | 元数据信息  |
| layers   | 层级信息   |
