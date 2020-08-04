
import com.badlogic.gdx.Version
import org.gradle.internal.impldep.org.junit.Assert.assertTrue
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File

/*
 * Copyright 2018 Blue Box Ware
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
internal object TestPlugin: Spek({

  lateinit var fixture: ProjectFixture

  beforeEachTest {
    fixture = ProjectFixture()
  }

  afterEachTest {
    fixture.destroy()
  }

  given("a project with the plugin applied") {

    beforeEachTest {
      fixture.buildFile("")
    }

    on("running the texturePackerSettingsHelp task") {

      fixture.build("texturePackerSettingsHelp")

      it("displays the available settings") {
        fixture.assertBuildOutputContains("filterMin: \"Nearest\"")
      }

    }

    on("running the gdxVersion task") {

      fixture.build("gdxVersion")

      it("should display the bundled GDX version") {
        fixture.assertBuildOutputContains("\n${Version.VERSION}\n")
      }

    }

    on("using Gradle version < 3.5") {

      fixture.gradleVersion = "3.4"
      val result = try {
        fixture.build()
        null
      } catch (e: UnexpectedBuildFailure) {
        e.message
      }

      it("should give an error") {
        assertTrue(result?.contains("version 3.5 or higher") == true)
      }

    }

  }

  given("a project with a forced GDX version") {

    beforeEachTest {

      fixture.buildFile("""

        buildscript {

          ext {
        	gdxVersion = "1.9.2"
          }

          repositories {
            flatDir dirs: "libs"
            mavenCentral()
          }
          dependencies {
            classpath "com.github.blueboxware:LibGDXGradlePlugin:${ProjectFixture.getVersion()}"
            classpath("com.badlogicgames.gdx:gdx-tools:${'$'}gdxVersion") {
              force = true
            }
                classpath("com.badlogicgames.gdx:gdx-backend-lwjgl:${'$'}gdxVersion") {
              force = true
            }
            classpath("com.badlogicgames.gdx:gdx-platform:${'$'}gdxVersion") {
              force = true
            }
          }
        }

        apply plugin: 'com.github.blueboxware.gdx'

      """, false)

      fixture.project.copy { copySpec ->
        copySpec.from(File("build/libs").absolutePath) {
          it.include("LibGDXGradlePlugin-*.jar")
        }
        copySpec.into(fixture.output["../libs"])
      }

    }

    on("running the gdxVersion task") {

      fixture.build("gdxVersion")

      it("should display the forced GDX version") {
        fixture.assertBuildOutputContains("\n1.9.2 (default: ${Version.VERSION})\n")
      }

    }

  }

})