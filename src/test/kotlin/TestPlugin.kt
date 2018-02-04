
import com.badlogic.gdx.Version
import org.gradle.internal.impldep.org.junit.Assert.assertTrue
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
    fixture = ProjectFixture(false)
  }

  afterEachTest {
    fixture.destroy()
  }

  given("a project with the plugin applied") {

    beforeEachTest {
      fixture.buildFile("")
    }

    on("running the texturePackerSettingsHelp task") {

      val result = fixture.build("texturePackerSettingsHelp")

      it("displays the available settings") {
        assertTrue(result.output, result.output.contains("filterMin: \"Nearest\""))
      }

    }

    on("running the gdxVersion task") {

      val result = fixture.build("gdxVersion")

      it("should display the bundled GDX version") {
        assertTrue(result.output, result.output.contains("\n${Version.VERSION}\n"))
      }

    }

  }

  given("a project with a forced GDX version") {

    beforeEachTest {

      fixture.buildFile("""

        buildscript {
          repositories {
            mavenCentral()
            flatDir dirs: "libs"
          }
          dependencies {
            classpath "com.github.blueboxware:LibGDXGradlePlugin:1.0"
            classpath ("com.badlogicgames.gdx:gdx-tools:1.9.2") {
              force = true
            }
          }
        }

        apply plugin: 'com.github.blueboxware.gdx'

      """, false)

      fixture.project.copy {
        it.from(File("build/libs").absolutePath) {
          it.include("LibGDXGradlePlugin-*.jar")
        }
        it.into(fixture.output["../libs"])
      }

    }

    on("running the gdxVersion task") {

      val result = fixture.build("gdxVersion")

      it("should display the forced GDX version") {
        assertTrue(result.output, result.output.contains("\n1.9.2 (default: ${Version.VERSION})\n"))
      }

    }

  }

})