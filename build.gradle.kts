import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  kotlin("jvm") version "1.7.10"
  id("java")
  id("java-gradle-plugin")
  id("maven-publish")
  id("com.gradle.plugin-publish") version "0.12.0"
  id("com.github.blueboxware.tocme") version "1.7"
  id("com.github.gmazzo.buildconfig") version "3.1.0"
}

group = properties("group")
version = properties("pluginVersion")

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("com.badlogicgames.gdx:gdx-tools:" + properties("gdxVersion"))
  implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:" + properties("gdxVersion"))
  implementation("com.badlogicgames.gdx:gdx-platform:" + properties("gdxVersion") + ":natives-desktop")
  implementation("com.badlogicgames.gdx:gdx-freetype-platform:" + properties("gdxVersion") + ":natives-desktop")
  implementation("commons-io:commons-io:2.6")
  implementation("org.apache.commons:commons-text:1.2")

  testImplementation(gradleTestKit())
  testImplementation(kotlin("test"))

  testImplementation("io.kotest:kotest-runner-junit5:" + properties("kotestVersion"))
  testImplementation("io.kotest:kotest-assertions-core:" + properties("kotestVersion"))
  testImplementation("io.kotest:kotest-framework-datatest:" + properties("kotestVersion"))
}

tasks {

  withType<Test> {
    useJUnitPlatform()
  }

  register("createReadmes") {

    inputs.file("README.md.src")
    inputs.file("gradle.properties")

    outputs.files(fileTree("dir" to ".", "include" to "*.md"))

    dependsOn(project.tasks.named("insertTocs"))

    doLast {

      val baseText = file("README.md.src")
              .readText()
              .replace(Regex("(?s)<exclude>.*?</exclude>\\s?"), "")
              .replace(Regex("(?s)</?test[^>]*>\\s*"), "")
              .replace("<releasedVersion>", properties("releasedPluginVersion"))
              .replace("<pluginVersion>", properties("pluginVersion"))
              .replace("<gdxVersion>", properties("gdxVersion"))

      file("README.md").writeText(
              baseText
                      .replace(Regex("(?s)<kotlin>.*?</kotlin>"), "")
                      .replace(Regex("(?s)```kotlin.*?```\\n?"), "")
                      .replace(Regex("(?s)</?groovy>"), "")
      )
      file("README-kotlin.md").writeText(
              baseText
                      .replace(Regex("(?s)<groovy>.*?</groovy>"), "")
                      .replace(Regex("(?s)```groovy.*?```\\n?"), "")
                      .replace(Regex("(?s)</?kotlin>"), "")
      )

    }

  }

}

buildConfig {
  packageName("com.github.blueboxware")
  buildConfigField("String", "GDX_VERSION", "\"" + properties("gdxVersion") + "\"")
}

gradlePlugin {
  plugins {
    register("gdxPlugin") {
      id = "com.github.blueboxware.gdx"
      implementationClass = "com.github.blueboxware.gdxplugin.GdxPlugin"
      displayName = "LibGDX Gradle plugin"
      version = properties("pluginVersion")
    }
  }
}

pluginBundle {
  website = "https://github.com/BlueBoxWare/LibGDXGradlePlugin"
  vcsUrl = "https://github.com/BlueBoxWare/LibGDXGradlePlugin.git"
  description = "Plugin to create Texture Packs, Bitmap Fonts, Nine patches and Distance Fields for use with LibGDX"
  tags = listOf("LibGDX")
}

tocme {
  docs("README.md.src")
}




