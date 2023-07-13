# 获取docker仓库地址
功能描述：获取docker仓库地址

### 请求地址
```
/repo-docker/ext/addr
```

### 请求方法
`GET`
### 请求参数

无

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
  "data": "docker.bkrepo.center.canway.com",
  "traceId": ""
}
```

#### data 字段说明

docker仓库地址
