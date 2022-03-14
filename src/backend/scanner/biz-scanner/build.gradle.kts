dependencies {
    api(project(":common:common-service"))
    api(project(":common:common-mongo"))
    api(project(":common:common-checker"))
    api(project(":scanner:api-scanner"))
    api(project(":common:common-storage:storage-service"))
    api(project(":repository:api-repository"))
    api(project(":common:common-security"))
    api(project(":common:common-stream"))
    implementation("com.github.docker-java:docker-java:3.2.5")
    implementation("com.github.docker-java:docker-java-transport-okhttp:3.2.5")
}
