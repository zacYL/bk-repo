# 根据名称类型查询仓库

功能描述：根据名称查询一个仓库

### 请求地址

```
/repo-repository/api/repo/info/{projectId}/{repoName}
```

### 请求方法

`GET`

### 请求参数

#### 路径参数

| 字段        | 类型     | 必填  | 描述   |
|-----------|--------|-----|------|
| projectId | String | 是   | 所属项目 |
| repoName  | String | 是   | 仓库名称 |

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
    "projectId": "test-xiaoyushen",
    "name": "scan_tool",
    "type": "DOCKER",
    "category": "LOCAL",
    "public": true,
    "description": "扫描器仓库",
    "configuration": {
      "type": "local",
      "webHook": {
        "webHookList": []
      },
      "cleanStrategy": null,
      "settings": {
        "system": false
      }
    },
    "storageCredentialsKey": null,
    "createdBy": "xiaoyushen",
    "createdDate": "2023-05-22T11:08:21.931",
    "lastModifiedBy": "xiaoyushen",
    "lastModifiedDate": "2023-05-22T11:08:21.931",
    "hasPermission": true,
    "permission": null,
    "artifacts": null,
    "quota": null,
    "used": 0,
    "coverStrategy": null
  },
  "traceId": ""
}
```
#### data 字段说明

仓库信息

| 字段                    | 说明                       |
|-----------------------|--------------------------|
| projectId             | 所属项目id                   |
| name                  | 仓库名称                     |
| type                  | 仓库类型                     |
| category              | 仓库类别                     |
| public                | 是否公开                     |
| description           | 简要描述                     |
| configuration         | 仓库配置信息(不同类别的仓库配置信息的结构不同) |
| storageCredentialsKey | 存储凭证key                  |
| createdBy             | 创建者                      |
| createdDate           | 创建日期                     |
| lastModifiedBy        | 上次修改者                    |
| lastModifiedDate      | 上次修改日期                   |
| hasPermission         | 在权限中心是否有查看权限             |
| permission            | 对当前用户的最高权限               |
| artifacts             | 制品数量                     |
| quota                 | 仓库配额                     |
| used                  | 仓库已使用容量                  |
| coverStrategy         | 覆盖策略                     |


