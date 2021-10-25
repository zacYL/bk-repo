dependencies {
    api(project(":common:common-devops:api"))
    api(project(":common:common-artifact:artifact-service"))
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("cn.hutool:hutool-extra:5.5.4")
    implementation("cn.hutool:hutool-core:5.5.4")
    implementation("com.google.zxing:core:3.4.1")
}