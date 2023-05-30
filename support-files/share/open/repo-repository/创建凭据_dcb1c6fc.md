# 创建凭据
功能描述：创建凭据

### 请求地址
```
/repo-repository/api/storage/credentials
```

### 请求方法
`POST`
### 请求参数



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| credentials     | StorageCredentialsReq   | 凭据信息 |
| key     | string   | 存储凭据key |
| region     | string   | S3或COS类型存储凭据所在区域 |
#### StorageCredentialsReq

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| cache     | CachePropertiesReq   |  |
| key     | string   |  |
| upload     | UploadPropertiesReq   |  |
#### CachePropertiesReq

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| enabled     | boolean   |  |
| expireDays     | integer , format:int32  |  |
| loadCacheFirst     | boolean   |  |
| path     | string   |  |
#### UploadPropertiesReq

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| localPath     | string   |  |
| location     | string   |  |

### 返回结果

