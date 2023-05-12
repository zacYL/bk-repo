# update
功能描述：update

### 请求地址
```
/api/remote/whitelist/switch/{type}
```

### 请求方法
`POST`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| type     | string   | true       | type;枚举：[COMPOSER, DOCKER, GENERIC, GIT, HELM, MAVEN, NONE, NPM, NUGET, OCI, PYPI, RDS, RPM] |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| status     | boolean   | true       | 是否 |



### 返回结果

