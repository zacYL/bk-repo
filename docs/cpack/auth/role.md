# Auth接口

[toc]

## 系统级用户组

- API: GET /auth/api/sys/role/list

- API 名称: role_list

- 功能说明：**该接口只有管理员有权限调用**
  
  - 中文：用户列表
  - English：role list
  
- 请求体

  ```json
  此接口请求体为空
  ```
  
- 请求字段说明

  
  
- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": [
          {
              "roleId": "306a8efe13444212bd13bbad3e0ea9d3",
              "type": "SYSTEM",
              "name": "企鹅企鹅",
              "projectId": null,
              "repoName": null,
              "admin": false,
              "users": [],
              "description": null
          }
      ],
      "traceId": ""
  }
  ```

- data 字段说明

  | 字段        | 类型         | 说明                 | Description   |
  | ----------- | ------------ | -------------------- | ------------- |
  | roleId      | string       | 用户组ID             | Role id       |
  | type        | string       | 用户类型：SYSTEM     | Role type     |
  | name        | string       | 用户组名称           | Role name     |
  | projectId   | string       | 用户组所属项目：null | Role project  |
  | repoName    | string       | 用户组所属仓库：null | role reponame |
  | admin       | boolean      | 是否为管理角色       | Admin         |
  | users       | list<String> | 关联用户             | user list     |
  | description | string       | 描述信息             | description   |



## 项目下用户组列表

- API: GET /auth/api/sys/role/list/{projectId}

- API 名称: project_role_list

- 功能说明：
  - 中文：项目下用户组列表
  - English：project role list
  
- 请求体
  此接口请求体为空

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明 | Description  |
  | --------- | ------ | -------- | ------ | ---- | ------------ |
  | projectId | string | 是       | 无     |      | proejct name |

- 响应体

  ``` json
  {
      "code": 0,
      "message": null,
      "data": [
          {
              "roleId": "306a8efe13444212bd13bbad3e0ea9d3",
              "type": "SYSTEM",
              "name": "企鹅企鹅",
              "projectId": null,
              "repoName": null,
              "admin": false,
              "users": [],
              "description": null
          }
      ],
      "traceId": ""
  }
  ```

- data 字段说明

  | 字段        | 类型         | 说明                 | Description   |
  | ----------- | ------------ | -------------------- | ------------- |
  | roleId      | string       | 用户组ID             | Role id       |
  | type        | string       | 用户类型：SYSTEM     | Role type     |
  | name        | string       | 用户组名称           | Role name     |
  | projectId   | string       | 用户组所属项目：null | Role project  |
  | repoName    | string       | 用户组所属仓库：null | role reponame |
  | admin       | boolean      | 是否为管理角色       | Admin         |
  | users       | list<String> | 关联用户             | user list     |
  | description | string       | 描述信息             | description   |



