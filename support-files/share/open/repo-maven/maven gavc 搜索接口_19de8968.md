# maven gavc 搜索接口
功能描述：maven gavc 搜索接口

### 请求地址
```
/repo-maven/ext/search/gavc/{projectId}/{pageNumber}/{pageSize}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段         | 类型     | 必填  | 描述   |
|------------|--------|-----|------|
| projectId  | String | 是   | 项目id |
| pageNumber | Int    | 是   | 页码数  |
| pageSize   | Int    | 是   | 每页大小 |

#### 查询参数
g, a, v, c 四个查询条件不能全为空

| 字段    | 类型     | 必填  | 描述         |
|-------|--------|-----|------------|
| tag   | String | 是   | tag名称      |
| g     | string | 否   | groupID    |
| a     | string | 否   | artifactId |
| v     | string | 否   | version    |
| c     | string | 否   | classifier |
| repos | string | 否   | 仓库名列表      |

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
    "pageSize": 20,
    "totalRecords": 4,
    "totalPages": 1,
    "records": [
      {
        "uri": "http://127.0.0.1/maven/test/maven/com/mycompany/app/my-app/1.0-3/my-app-1.0-3.jar"
      },
      {
        "uri": "http://127.0.0.1/maven/test/maven/com/mycompany/app/my-app/1.0-3/my-app-1.0-3.pom"
      },
      {
        "uri": "http://127.0.0.1/maven/test/maven1/com/mycompany/app/my-app/1.0-3/my-app-1.0-3.jar"
      },
      {
        "uri": "http://127.0.0.1/maven/test/maven1/com/mycompany/app/my-app/1.0-3/my-app-1.0-3.pom"
      }
    ],
    "page": 1,
    "count": 4
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

文件下载链接列表
