import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  `kotlin-dsl`
  id("java")
  id("java-gradle-plugin")
  id("maven-publish")
  id("com.gradle.plugin-publish") version "1.3.1"
  id("com.github.blueboxware.tocme") version "1.8"
  id("com.github.gmazzo.buildconfig") version "5.6.7"
}

group = properties("group")
version = properties("pluginVersion")

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("com.badlogicgames.gdx:gdx-tools:" + properties("gdxVersion"))
  implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:" + properties("gdxVersion"))
  implementation("com.badlogicgames.gdx:gdx-platform:" + properties("gdxVersion") + ":natives-desktop")
  implementation("com.badlogicgames.gdx:gdx-freetype-platform:" + properties("gdxVersion") + ":natives-desktop")
  implementation("commons-io:commons-io:2.19.0")

  testImplementation(gradleTestKit())
  testImplementation(kotlin("test"))

  testImplementation("io.kotest:kotest-runner-junit5:" + properties("kotestVersion"))
  testImplementation("io.kotest:kotest-assertions-core:" + properties("kotestVersion"))
  testImplementation("io.kotest:kotest-framework-datatest:" + properties("kotestVersion"))
}

buildConfig {
  packageName("com.github.blueboxware")
  buildConfigField("String", "GDX_VERSION", "\"" + properties("gdxVersion") + "\"")
}

abstract class TestTask : Test() {
  @get:Input
  abstract val useBuildCache: Property<Boolean>

  @get:Input
  abstract val useConfigurationCache: Property<Boolean>

  @get:Input
  abstract val version: Property<String>

  override fun getGroup(): String = "verification"

  override fun getFilter(): TestFilter? {
    return DefaultTestFilter().includeTestsMatching("*")
  }

  @TaskAction
  override fun executeTests() {
    systemProperty("gdxplugin.cache.build", useBuildCache.get())
    systemProperty("gdxplugin.cache.configuration", useConfigurationCache.get())
    systemProperty("gdxplugin.gradle.version", version.get())

    super.executeTests()
  }

}

for (useBuildCache in listOf(true, false)) {
  for (useConfCache in listOf(true, false)) {
    for (version in listOf("8.5", "8.14.3", "9.0.0")) {
      val name = buildString {
        append("test")
        if (useConfCache) append("ConfCache")
        if (useBuildCache) append("BuildCache")
        append("Version")
        append(version)
      }
      val task = tasks.register<TestTask>(name) {
        this.useBuildCache = useBuildCache
        this.useConfigurationCache = useConfCache
        this.version = version
      }
      tasks.test {
        dependsOn(task)
      }
    }
  }
}

tasks {

  test {
    exclude("*")
  }

  withType<Test>().configureEach {
    useJUnitPlatform()

    dependsOn("publishToMavenLocal")
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

gradlePlugin {
  website.set("https://github.com/BlueBoxWare/LibGDXGradlePlugin")
  vcsUrl.set("https://github.com/BlueBoxWare/LibGDXGradlePlugin.git")
  plugins {
    create("gdxPlugin") {
      id = "com.github.blueboxware.gdx"
      implementationClass = "com.github.blueboxware.gdxplugin.GdxPlugin"
      displayName = "LibGDX Gradle plugin"
      description = "Plugin to create Texture Packs, Bitmap Fonts, Nine patches and Distance Fields for use with LibGDX"
      version = properties("pluginVersion")
      tags.set(listOf("LibGDX"))
    }
  }
}

tocme {
  docs("README.md.src")
}




