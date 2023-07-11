# 测试远程仓库URL

功能描述：测试远程仓库URL

### 请求地址

```
/repo-repository/api/repo/testremote
```

### 请求方法

`POST`

### 请求参数

#### 请求体参数

##### 一级参数说明

| 字段          | 类型     | 必填  | 描述      |
|-------------|--------|-----|---------|
| type        | String | 否   | 仓库类型    |
| url         | String | 是   | 远程仓库URL |
| credentials | Object | 否   | 仓库访问凭证  |
| network     | Object | 否   | 网络设置    |

##### 详细说明

> 请求体是一个json格式的对象`RemoteUrlRequest`, 其结构如下(仅展示结构, 不能这样传参):

```json
{
  "// type": "仓库类型",
  "type": "MAVEN",
  "// url": "远程仓库URL",
  "url": "https://repo.maven.apache.org/maven2/",
  "// credentials": "仓库访问凭证",
  "credentials": {
    "// username": "仓库用户名",
    "username": null,
    "// password": "仓库密码",
    "password": null
  },
  "// network": "网络配置",
  "network": {
    "// proxy": "代理设置",
    "proxy": {
      "// host": "代理IP",
      "host": "127.0.0.1",
      "// port": "端口",
      "port": "8888",
      "// username": "代理用户名",
      "username": null,
      "// password": "代理密码",
      "password": null
    },
    "// connectTimeout": "远程请求连接超时时间, 单位ms(默认为10000)",
    "connectTimeout": 10000,
    "// readTimeout": "远程请求读超时时间, 单位ms(默认为10000)",
    "readTimeout": 10000
  }
}
```

##### 请求体json示例

```json
{
  "type": "MAVEN",
  "url": "https://repo.maven.apache.org/maven2/",
  "credentials": {
    "username": null,
    "password": null
  },
  "network": {
    "proxy": null
  }
}
```

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
  "code" : 0,
  "message" : null,
  "data" : {
    "success" : true,
    "message" : "200 OK"
  },
  "traceId" : ""
}
```

#### data 字段说明

连接状态信息

| 字段      | 描述       |
|---------|----------|
| success | 是否成功     |
| message | 返回码和返回信息 |
