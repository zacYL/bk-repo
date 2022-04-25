dependencies {
    api(project(":common:common-api"))
    api("net.canway:dependency-check-simple:0.2.3") {
        exclude(group = "junit", module = "junit")
    }
}
