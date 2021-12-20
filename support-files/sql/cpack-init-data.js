//初始管理员账号，userId:admin , password：bkrepo
db.user.updateOne(
    { userId: "admin" },
    {
        $setOnInsert: {
            userId: "admin",
            name: "admin",
            pwd: "ed424c8ee8955e30b9975ed83fdfef61",
            admin: true,
            locked: false,
            tokens: [],
            roles: [],
            asstUsers: [],
            group: false,
            createdDate: new Date(),
            lastModifiedDate: new Date()
        }
    },
    { upsert: true }
);

// 初始化demo项目
db.project.updateOne(
    { name: "demo" },
    {
        $setOnInsert: {
            name: "demo",
            displayName: "demo",
            description: "示例项目",
            createdBy: "admin",
            createdDate: new Date(),
            lastModifiedBy: "admin",
            lastModifiedDate: new Date()
        }
    },
    { upsert: true }
);

// 初始化custom仓库
db.repository.updateOne(
    {
        projectId: "demo",
        name: "custom"
    },
    {
        $setOnInsert: {
            projectId: "demo",
            name: "custom",
            type: "GENERIC",
            category: "LOCAL",
            public: false,
            description: "generic custom repository",
            configuration: "{}",
            display: true,
            createdBy: "admin",
            createdDate: new Date(),
            lastModifiedBy: "admin",
            lastModifiedDate: new Date()
        }
    },
    { upsert: true }
);
