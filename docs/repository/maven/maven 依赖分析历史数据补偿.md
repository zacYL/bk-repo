# maven 依赖分析历史数据补偿

[toc]

## 使用方法

更新服务后使用制品库管理员账号调用下面接口：

例：curl -uadmin:bkrepo {bkrepoHost}/maven/debug/dependencies/foreach

等待一段时间后接口没有报错正常返回就代表成功，正常返回数据如下

```json
{
    "code": 0,
    "message": null,
    "data": true,
    "traceId": ""
}
```



## 注意事项

注意不要关闭终端窗口，制品库服务中如果存在大量maven制品可能会运行很长时间

如果意外关闭了终端窗口或者遇到http连接超时，可以重复执行