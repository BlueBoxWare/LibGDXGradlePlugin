import com.badlogic.gdx.Version
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir

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
internal object TestPlugin : BehaviorSpec({

  lateinit var fixture: ProjectFixture

  beforeContainer {
    fixture = ProjectFixture(tempdir())
  }

  Given("a project with the plugin applied") {

    beforeContainer {
      fixture.buildFile("")
    }

    When("running the texturePackerSettingsHelp task") {

      fixture.build("texturePackerSettingsHelp")

      Then("displays the available settings") {
        fixture.assertBuildOutputContains("filterMin: \"Nearest\"")
      }

    }

    When("running the gdxVersion task") {

      fixture.build("gdxVersion")

      Then("should display the bundled GDX version") {
        fixture.assertBuildOutputContains("\n${Version.VERSION}\n")
      }

    }


  }

  Given("a project with a forced GDX version") {

    beforeContainer {

      fixture.buildFile(
        """

        buildscript {

          ext {
            gdxVersion = "1.12.0"
          }

          repositories {
            mavenCentral()
            mavenLocal()
          }
          dependencies {
            classpath "com.github.blueboxware:LibGDXGradlePlugin:${ProjectFixture.getVersion()}"
            configurations.all {
             resolutionStrategy {
                force "com.badlogicgames.gdx:gdx-tools:${'$'}gdxVersion"
                force "com.badlogicgames.gdx:gdx-backend-lwjgl3:${'$'}gdxVersion"
                force "com.badlogicgames.gdx:gdx-platform:${'$'}gdxVersion"
                force "com.badlogicgames.gdx:gdx-freetype-platform:${'$'}gdxVersion"
             }
             }
            }
        }

        apply plugin: 'com.github.blueboxware.gdx'

        
        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/roboto.fnt')

                size 16
               

            }
        }

      """, false
      )

      fixture.addFile("etc/roboto.ttf")

    }

    When("running the gdxVersion task") {

      fixture.build("gdxVersion")

      Then("should display the forced GDX version") {
        fixture.assertBuildOutputContains("\n1.12.0 (default: ${Version.VERSION})\n")
      }

    }

    When("running the BitmapFont task") {
      fixture.build("createAllFonts")

      Then("should create the correct files") {
        fixture.assertFilesExist(
          "roboto.fnt",
          "roboto.png",
        )
      }
    }

  }

})
