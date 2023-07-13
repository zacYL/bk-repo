# 仓库 包数量 总览

功能描述：仓库 包数量 总览

### 请求地址

```
/repo-repository/api/software/package/search/overview
```

### 请求方法

`GET`

### 请求参数

#### 查询参数

| 字段          | 类型     | 必填  | 描述   |
|-------------|--------|-----|------|
| repoType    | String | 是   | 仓库类型 |
| projectId   | String | 否   | 项目id |
| repoName    | String | 否   | 仓库名称 |
| packageName | String | 否   | 包名   |

### 返回结果

| 字段      | 说明     |
|---------|--------|
| code    | 返回码    |
| message | 错误信息   |
| data    | 数据     |
| traceId | 链路追踪id |

#### 响应体示例

```json
{
  "code": 0,
  "message": null,
  "data": [
    {
      "projectId": "test",
      "repos": [
        {
          "repoName": "docker-local",
          "repoCategory": "LOCAL",
          "packages": 2
        }
      ],
      "sum": 2
    },
    {
      "projectId": "bb8d9b",
      "repos": [
        {
          "repoName": "docker1",
          "repoCategory": "LOCAL",
          "packages": 4
        }
      ],
      "sum": 4
    }
  ],
  "traceId": ""
}
```
#### data 字段说明

项目包搜索结果总览

| 字段        | 描述       |
|-----------|----------|
| projectId | 项目id     |
| repos     | **仓库列表** |
| sum       | 项目包总数    |

##### repos 字段说明

仓库包搜索结果总览

| 字段           | 描述    |
|--------------|-------|
| repoName     | 仓库名称  |
| repoCategory | 仓库类别  |
| packages     | 仓库包总数 |
