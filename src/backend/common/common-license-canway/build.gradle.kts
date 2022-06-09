

dependencies {
    implementation(project(":common:common-service"))
    implementation(project(":common:common-api"))
    implementation("net.canway.license:sdk-license:4.3.10-platform")
    api("io.github.openfeign:feign-jackson")
    api("io.github.openfeign:feign-jaxrs")
}