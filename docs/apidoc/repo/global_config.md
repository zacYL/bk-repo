# 全局配置接口

## 修改配置

- API: POST/api/config/update
- API 名称: update_global_config
- 功能说明：

  - 中文：修改全局配置
  - English：update global config
- 请求体

```
{
    "replicationNetworkRate":3
}
```

- 请求字段说明

| 字段                   | 类型 | 是否必须 | 默认值 | 说明         | Description              |
| ---------------------- | ---- | -------- | ------ | ------------ | ------------------------ |
| replicationNetworkRate | Long | 是       | 无     | 分发网络速率 | replication network rate |

- 响应体

```
{
    "code": 0,
    "message": null,
    "data": {
        "createdBy": "admin",
        "createdDate": "2022-10-18T13:58:59.637",
        "lastModifiedBy": "admin",
        "lastModifiedDate": "2022-10-18T14:27:22.947",
        "replicationNetworkRate": 3
    },
    "traceId": ""
}
```



## 获取配置

- API: get/api/config/info
- API 名称: get_global_config
- 功能说明：

  - 中文：获取全局配置
  - English：get global config
- 请求体：无请求体
- 响应体

```
{
    "code": 0,
    "message": null,
    "data": {
        "createdBy": "admin",
        "createdDate": "2022-10-18T13:58:59.637",
        "lastModifiedBy": "admin",
        "lastModifiedDate": "2022-10-18T14:27:22.947",
        "replicationNetworkRate": 3
    },
    "traceId": ""
}
```

