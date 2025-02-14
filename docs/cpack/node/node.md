# CPack -Node节点操作接口

[toc]

## 批量删除节点

- API: DELETE /repository/api/node/batch/{projectId}/{repoName}

- API 名称: batch_delete_node

- 功能说明：
  - 中文：批量删除节点，同时支持删除目录和文件节点
  - English：batch delete node
  
- 请求体
  
  ```json
  ["/a/deployment.yaml", "/a/developLog.txt", "/a/b"]
  ```

* 请求体说明

  **参数上限为200**

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|是|无|项目名称|project name|
  |repoName|string|是|无|仓库名称|repo name|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```



## 统计批量删除节点数

- API: POST /repository/api/node/batch/{projectId}/{repoName}

- API 名称: count_batch_delete_node

- 功能说明：

  - 中文：统计批量删除节点数，支持传参目录和文件节点
  - English：count batch delete node

- 请求体

  ```json
  ["/a/deployment.yaml", "/a/developLog.txt", "/a/b"]
  ```

* 请求体说明

  **参数上限为200**

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明     | Description  |
  | --------- | ------ | -------- | ------ | -------- | ------------ |
  | projectId | string | 是       | 无     | 项目名称 | project name |
  | repoName  | string | 是       | 无     | 仓库名称 | repo name    |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": 3,
    "traceId": null
  }
  ```

* 响应字段说明

  | 字段 | 类型 | 说明   | Description |
  | ---- | ---- | ------ | ----------- |
  | data | Int  | 节点数 | nodes       |

  
