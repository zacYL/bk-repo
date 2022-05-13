dependencies {
    api(project(":common:common-api"))
    api(project(":common:common-checker:api-checker"))
    api("net.canway:dependency-check-simple:0.2.6") {
        exclude(group = "junit", module = "junit")
    }
}