# updateWhitelist
功能描述：updateWhitelist

### 请求地址
```
/api/remote/whitelist/{id}
```

### 请求方法
`POST`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| id     | string   | true       | id |



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| packageKey     | string   |  |
| type     | string   |  |
| versions     | array<string>   |  |

### 返回结果

