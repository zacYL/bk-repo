dependencies {
    api("org.springframework.boot:spring-boot-starter-data-redis")

    // 要用redis连接池 必须有pool依赖
    implementation("org.apache.commons:commons-pool2")
}