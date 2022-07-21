# 更新凭据
功能描述：更新凭据

### 请求地址
```
/api/storage/credentials/{credentialsKey}
```

### 请求方法
`PUT`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| credentialsKey     | string   | true       | credentialsKey |



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| credentials     | StorageCredentialsReq   | 更新的存储凭据内容 |

### 返回结果

