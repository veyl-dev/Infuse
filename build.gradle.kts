plugins { java }

group = "dev.veyl.infusesmp"
version = "0.1.0-SNAPSHOT"

val paperApiVersion = providers.gradleProperty("paperApiVersion").getOrElse("1.21.1-R0.1-SNAPSHOT")
val targetJava = providers.gradleProperty("targetJava").getOrElse("21").toInt()
val targetApi = providers.gradleProperty("targetApi").getOrElse("1.21")

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(targetJava))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJava)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version, "apiVersion" to targetApi)
    }
}

tasks.jar {
    archiveBaseName.set("InfuseSMP-S3")
    archiveClassifier.set(if (targetApi == "1.21") "mc1.21" else "mc" + targetApi.replace(".", ""))
}
