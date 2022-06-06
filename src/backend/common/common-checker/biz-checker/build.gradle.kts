dependencies {
    api(project(":common:common-checker:api-checker"))
    implementation("net.canway:dependency-check-simple:0.4.0") {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.slf4j", module = "slf4j-api")
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }
}
