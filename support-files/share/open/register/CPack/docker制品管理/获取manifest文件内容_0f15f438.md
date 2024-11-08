# 获取manifest文件内容
功能描述：获取manifest文件内容



## 请求

#### 接口方法

`GET`

#### 接口地址

`/api/open/CPack/repo-oci/ext/manifest/{projectId}/{repoName}/{imageName}/{tag}`

#### 请求头

| 字段                  | 类型   | 必填 | 描述               |
| --------------------- | ------ | ---- | ------------------ |
| X-DEVOPS-ACCESS-TOKEN | String | 是   | Devops用户访问令牌 |

#### 路径参数

| 字段           | 类型     | 必填  | 描述    |
|--------------|--------|-----|-------|
| projectId    | String | 是   | 项目ID |
| repoName     | String | 是   | 仓库名称  |
| imageName    | String | 是   | 镜像名称（可能为多层路径） |
| tag          | String | 是   | 镜像tag（版本号） |

#### 查询参数

无

#### 请求体

无

#### 请求示例

```bash
curl -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' \
 'https://devops.example.com/api/open/CPack/repo-oci/ext/manifest/{projectId}/{repoName}/{imageName}/{tag}
```



## 响应

#### 响应示例

```json
{
  "code": 0,
  "message": null,
  "data": "{\n   \"schemaVersion\": 2,\n   \"mediaType\": \"application/vnd.docker.distribution.manifest.v2+json\",\n   \"config\": {\n      \"mediaType\": \"application/vnd.docker.container.image.v1+json\",\n      \"size\": 7918,\n      \"digest\": \"sha256:3f3447deacaa5bacab184fabbf821a785e13303b2e340465bdf7815b0284497b\"\n   },\n   \"layers\": [\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 50496029,\n         \"digest\": \"sha256:e83e8f2e82cc31391cd0cb4f5ba574ba5eb9708fc0f5dcc34fef53b03ef28f31\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 870,\n         \"digest\": \"sha256:0f23deb01b847d9dd0fc43ede2d2dacda423b95fdbf64e0ce21a6f542f6a167e\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 983706,\n         \"digest\": \"sha256:f5bda3b184ea984d363fc64e635076bcb405620effc23b2eac44f23e662bfd57\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 4585198,\n         \"digest\": \"sha256:ed17edbc6604e6a5d584a08fa036d3d0711dde7676b4a6fe0adeb03b148ba5e7\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 2653,\n         \"digest\": \"sha256:33a94a6acfa729dcf5f76be5966ecdc31692840ba7619f81d82d5e6dfc717c03\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 333,\n         \"digest\": \"sha256:3686cf92b89d34f1d5ef195eae6471104f3a875645541473e013e18d4b595e5e\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 25532514,\n         \"digest\": \"sha256:f81535a6a8bfbe8280ee0edbb4f5e8472a38785d2c36fd5c59b85996314f8ad4\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 320,\n         \"digest\": \"sha256:4bffb03ea5e2bb6d0867f6130190cdd297dc62dcd87b40f33ef7242ae6c76106\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 48449817,\n         \"digest\": \"sha256:49348ef8dcaad9bf9de316da06cf84291bcbeb9dd40b063fc9dfe852e4e3f0fc\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 5389,\n         \"digest\": \"sha256:509d665d0cf5e0b271d7d16a0f0fd46a59006b6c62a8e860701622fa702a2826\"\n      },\n      {\n         \"mediaType\": \"application/vnd.docker.image.rootfs.diff.tar.gzip\",\n         \"size\": 121,\n         \"digest\": \"sha256:adc919b937fd60ace9e14a4d9963cdadb2f1939b0c5501cb83caf17f50eb4cfd\"\n      }\n   ]\n}",
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

manifest文件内容
