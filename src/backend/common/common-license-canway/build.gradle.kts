

dependencies {
    implementation(project(":common:common-service"))
    implementation(project(":common:common-api"))
    implementation("net.canway.license:sdk-license:6.0.0-SNAPSHOT")
    api("io.github.openfeign:feign-jackson")
    api("io.github.openfeign:feign-jaxrs")
}
