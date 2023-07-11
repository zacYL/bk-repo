# 查询包的依赖项
功能描述：查询包的依赖项

### 请求地址
```
/repo-maven/ext/dependencies/{projectId}/{repoName}
```

### 请求方法
`GET`
### 请求参数

#### 路径参数

| 字段        | 类型     | 必填  | 描述         |
|-----------|--------|-----|------------|
| projectId | String | 是   | 项目id       |
| repoName  | String | 是   | 仓库名称       |

#### 查询参数

| 字段         | 类型     | 必填  | 描述         |
|------------|--------|-----|------------|
| packageKey | String | 是   | 包唯一key     |
| version    | String | 否   | 包版本        |
| pageNumber | Int    | 否   | 页码数, 默认为1  |
| pageSize   | Int    | 否   | 每页大小, 默认20 |

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
  "data": {
    "pageNumber": 1,
    "pageSize": 10,
    "totalRecords": 19,
    "totalPages": 2,
    "records": [
      {
        "groupId": "com.tencent.bk.devops.atom",
        "artifactId": "jolokia-war",
        "version": "1.7.0",
        "type": "war",
        "classifier": null,
        "scope": null,
        "optional": false
      },
      {
        "groupId": "io.javaslang",
        "artifactId": "javaslang",
        "version": "2.0.6",
        "type": "jar",
        "classifier": null,
        "scope": "provided",
        "optional": false
      }
    ],
    "count": 19,
    "page": 1
  },
  "traceId": ""
}
```

#### data 字段说明

分页信息

| 字段           | 说明                     |
|--------------|------------------------|
| pageNumber   | 页码(从1页开始)              |
| pageSize     | 每页多少条                  |
| totalRecords | 总记录条数                  |
| totalPages   | 总页数                    |
| records      | **数据列表**               |
| count        | 已废弃, 由`totalRecords`代替 |
| page         | 已废弃, 由`pageNumber`代替   |

##### records 字段说明

依赖列表

| 字段         | 描述             |
|------------|----------------|
| groupId    | 坐标: groupId    |
| artifactId | 坐标: artifactId |
| version    | 坐标: version    |
| type       | 包类型            |
| classifier | 构件类型           |
| scope      | 作用域            |
| optional   | 依赖项是否可选        |
