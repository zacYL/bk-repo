# pypi 版本删除接口
功能描述：pypi 版本删除接口

### 请求地址
```
/repo-pypi/ext/version/delete/{projectId}/{repoName}
```

### 请求方法
`DELETE`
### 请求参数
#### 路径参数

| 字段          | 类型     | 必填  | 描述   |
|-------------|--------|-----|------|
| projectId   | String | 是   | 项目id |
| repoName    | String | 是   | 仓库名称 |

#### 查询参数

| 字段          | 类型     | 必填  | 描述     |
|-------------|--------|-----|--------|
| packageKey  | String | 是   | 包唯一key |
| version     | String | 否   | 包版本    |

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
  "code":0,
  "message":null,
  "data":null,
  "traceId":""
}
```
