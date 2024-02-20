import com.badlogic.gdx.Version
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import java.io.File

/*
 * Copyright 2021 Blue Box Ware
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
internal object TestPlugin: BehaviorSpec({

  lateinit var fixture: ProjectFixture

  beforeContainer {
    fixture = ProjectFixture(tempdir())
  }

  given("a project with the plugin applied") {

    beforeContainer {
      fixture.buildFile("")
    }

    `when`("running the texturePackerSettingsHelp task") {

      fixture.build("texturePackerSettingsHelp")

      then("displays the available settings") {
        fixture.assertBuildOutputContains("filterMin: \"Nearest\"")
      }

    }

    `when`("running the gdxVersion task") {

      fixture.build("gdxVersion")

      then("should display the bundled GDX version") {
        fixture.assertBuildOutputContains("\n${Version.VERSION}\n")
      }

    }


  }

  given("a project with a forced GDX version") {

    beforeContainer {

      fixture.buildFile("""

        buildscript {

          ext {
        	gdxVersion = "1.10.0"
          }

          repositories {
            flatDir dirs: "libs"
            mavenCentral()
          }
          dependencies {
            classpath "com.github.blueboxware:LibGDXGradlePlugin:${ProjectFixture.getVersion()}"
            classpath "org.jetbrains.kotlin:kotlin-stdlib:1.9.22"
            classpath("com.badlogicgames.gdx:gdx-tools") {
               version {
                 strictly("${'$'}gdxVersion")
               }
            }
            classpath("com.badlogicgames.gdx:gdx-backend-lwjgl3") {
              version {
                strictly("${'$'}gdxVersion")
              }
            }
            classpath("com.badlogicgames.gdx:gdx-platform") {
              version {
                strictly("${'$'}gdxVersion")
              }
            }
          }
        }

        plugins {
          id 'org.jetbrains.kotlin.jvm' version '1.9.22'
        }
        
        apply plugin: 'com.github.blueboxware.gdx'
        
        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/roboto.fnt')

                size 16
               

            }
        }

      """, false)

      fixture.addFile("etc/roboto.ttf")

      fixture.project.copy { copySpec ->
        copySpec.from(File("build/libs").absolutePath) {
          it.include("LibGDXGradlePlugin-*.jar")
        }
        copySpec.into(fixture.output["../libs"])
      }

    }

    `when`("running the gdxVersion task") {

      fixture.build("gdxVersion")

      then("should display the forced GDX version") {
        fixture.assertBuildOutputContains("\n1.10.0 (default: ${Version.VERSION})\n")
      }

    }

    `when`("running the BitmapFont task") {
      fixture.build("createAllFonts")

      then("should create the correct files") {
        fixture.assertFilesExist(
          "roboto.fnt",
          "roboto.png",
        )
      }
    }


  }


})
