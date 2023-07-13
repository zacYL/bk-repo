# 删除repo下的指定镜像
功能描述：删除repo下的指定镜像

### 请求地址
```
/repo-docker/ext/package/delete/{projectId}/{repoName}
```

### 请求方法
`DELETE`
### 请求参数
#### 路径参数

| 字段        | 类型     | 必填  | 描述         |
|-----------|--------|-----|------------|
| projectId | String | 是   | 项目id       |
| repoName  | String | 是   | 仓库名称       |

#### 查询参数

| 字段         | 类型     | 必填  | 描述     |
|------------|--------|-----|--------|
| packageKey | String | 是   | 包唯一key |

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
  "traceId":"",
  "data":true
}
```

#### data 字段说明

是否删除成功
