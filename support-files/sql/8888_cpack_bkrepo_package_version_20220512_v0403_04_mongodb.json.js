//自动清理功能统一刷新【package_version】表的【lastModifiedDate】时间
db.package_version.find({}).forEach(
    function(item){
        db.package_version.update({
            _id:item._id
        },
        {
            $set:{
                lastModifiedDate:new ISODate()
            }
        })
    }
);
