
repositories{
    maven { setUrl("http://artifact.canway.net/maven-public/")}
}

dependencies {
    implementation(project(":common:common-service"))
    implementation("net.canway.license:sdk-license:1.0.0-SNAPSHOT")
}