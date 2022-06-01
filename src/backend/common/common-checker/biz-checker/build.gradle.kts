dependencies {
    api(project(":common:common-checker:api-checker"))
    api("net.canway:dependency-check-simple:0.3.1") {
        exclude(group = "junit", module = "junit")
    }
}
