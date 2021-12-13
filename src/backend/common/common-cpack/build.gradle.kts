dependencies {
    api (project(":common:common-service"))
    api (project(":repository:api-repository"))
    api (project(":auth:api-auth"))
    api (project(":common:common-devops:api"))
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("cn.hutool:hutool-extra:5.5.4")
    implementation("cn.hutool:hutool-core:5.5.4")
    implementation("com.google.zxing:core:3.4.1")
}