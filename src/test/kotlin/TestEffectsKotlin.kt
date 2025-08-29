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
internal object TestEffectsKotlin: BehaviorSpec({

  lateinit var fixture: ProjectFixture

  val buildFile = """
        plugins {
            id("com.github.blueboxware.gdx") version "${ProjectFixture.getVersion()}"
        }

        bitmapFonts {
          create("roboto") {
            inputFont = file("in/etc/roboto.ttf")
            outputFile = file("out/<name>.fnt")
            sizes(64)
            settings {
              effects = listOf(
                color {
                  color = color("00ffff")
                },
                <effect>
              )
            }
          }
        }
      """

  beforeContainer {
    fixture = ProjectFixture(tempdir(), useKotlin = true, addClassPath = true)
    fixture.addFile("etc/roboto.ttf")
  }

  Given("an outline effect") {

    beforeContainer {

      fixture.buildFile(buildFile.replace("<effect>", """
        outline {
          width = 4f
          color = color("#FFFF00")
          join = JoinRound
        }
      """).replace("<name>", "outline"))

    }

    When("building") {

      fixture.build("generateRobotoFont")

      Then("should create the correct font") {
        fixture.assertBuildSuccess()
        fixture.assertFontEquals("bitmapFont/outline.fnt", "outline.fnt")
      }

    }

  }

  Given("a wobble effect") {

    beforeContainer {

      fixture.buildFile(buildFile.replace("<effect>", """
        wobble {
          width = 4f
          color = color("#FFFF00")
          detail = 8f
          amplitude = 4f
        }
      """).replace("<name>", "wobble"))

    }

    When("building") {

      fixture.build("generateRobotoFont")

      Then("should create the correct font") {
        fixture.assertBuildSuccess()
        fixture.assertFontEquals("bitmapFont/wobble.fnt", "wobble.fnt", checkTextures = false)
      }

    }

  }

  Given("a zigzag effect") {

    beforeContainer {

      fixture.buildFile(buildFile.replace("<effect>", """
        zigzag {
          width = 3f
          color = color("#FFFF00")
          wavelength = 5f
          amplitude = 3f
          join = JoinMiter
        }
      """).replace("<name>", "zigzag"))

    }

    When("building") {

      fixture.build("generateRobotoFont")

      Then("should create the correct font") {
        fixture.assertBuildSuccess()
        fixture.assertFontEquals("bitmapFont/zigzag.fnt", "zigzag.fnt")
      }

    }

  }

  Given("a distance field effect") {

    beforeContainer {

      fixture.buildFile("""
        plugins {
            id("com.github.blueboxware.gdx") version "${ProjectFixture.getVersion()}"
        }

        bitmapFonts {
          create("roboto") {
            inputFont = file("in/etc/roboto.ttf")
            outputFile = file("out/distanceField.fnt")
            sizes(32)
            settings {
              effects = listOf(
                distanceField {
                  color = color("ff0000")
                  scale = 32
                  spread = 4f
                }
              )
            }
          }
        }
      """)

    }

    When("building") {

      fixture.build("generateRobotoFont")

      Then("should create the correct font") {
        fixture.assertBuildSuccess()
        fixture.assertFontEquals("bitmapFont/distanceField.fnt", "distanceField.fnt")
      }

    }

  }

})
