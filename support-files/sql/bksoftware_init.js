//删除制品库operation_log 表历史数据
//删除制品库operation_log 表历史数据,新增bksoftware 项目
db.operation_log.remove({})
db.project.insert(
    {
        "createdBy" : "admin",
        "createdDate" : ISODate("2021-07-01T02:47:41.204Z"),
        "lastModifiedBy" : "admin",
        "lastModifiedDate" : ISODate("2021-07-01T02:47:41.204Z"),
        "name" : "bksoftware",
        "displayName" : "bksoftware",
        "description" : "bksoftware"
    })