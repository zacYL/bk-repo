dependencies {
    api(project(":common:common-api"))
    implementation("net.canway:dependency-check-simple:0.2.0") {
        exclude(group = "junit", module = "junit")
    }
}
