# 获取helm域名地址
功能描述：获取helm域名地址

### 请求地址
```
/repo-helm/ext/address
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
  "data": {
    "domain": "http://bkrepo.center.canway.com/helm"
  },
  "traceId": ""
}
```

#### data 字段说明

helm 域名信息
