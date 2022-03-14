dependencies {
    api(project(":common:common-api"))
    api(project(":common:common-artifact:artifact-service"))
    compileOnly("org.springframework.cloud:spring-cloud-openfeign-core")
}
