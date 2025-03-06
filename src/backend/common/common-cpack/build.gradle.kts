dependencies {
    api(project(":common:common-service"))
    api(project(":repository:api-repository"))
    api(project(":common:common-metadata:metadata-service"))
    api(project(":auth:api-auth"))
    api(project(":common:common-devops:api"))
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("cn.hutool:hutool-extra:${Versions.Hutool}")
    implementation("cn.hutool:hutool-core:${Versions.Hutool}")
    implementation("com.google.zxing:core:3.4.1")
    implementation("org.apache.commons:commons-lang3")
}
