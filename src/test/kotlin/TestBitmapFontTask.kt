import com.github.blueboxware.gdxplugin.GdxPlugin
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
internal object TestBitmapFontTask: BehaviorSpec({

  lateinit var fixture: ProjectFixture

  beforeContainer {
    fixture = ProjectFixture(tempdir(), addClassPath = true)
    fixture.addFile("etc/roboto.ttf")
  }

  Given("a bitmap font task container") {

    beforeContainer {

      fixture.buildFile("""

        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/roboto.fnt')

                size 16
                sizes 32, 64

                size 16, "out/custom16.fnt"
                size 32, file("out/custom32")

                settings {
                  renderType = FreeType
                }

            }

            arial {

                inputFont = 'Arial Black'

                outputFile = 'out/arial'

                size = 12
                sizes = [16, 64]

                characters = "abcdef"

                settings {

                    bold = true
                    italic = false
                    gamma = 2
                    paddingTop = 1
                    paddingBottom = 2
                    paddingLeft = 3
                    paddingRight = 4
                    paddingAdvanceX = 2
                    paddingAdvanceY = 3
                    glyphPageWidth = 64
                    glyphPageHeight = 128

                    effects = [
                        color {
                            color = color('0000ff')
                        },
                        shadow {
                            color = color("#ff0000")
                            xDistance = 4
                        }
                    ]

                }

            }

        }
      """)

    }

    When("listing tasks") {

      fixture.build("tasks")

      Then("should contain the expected tasks") {
        fixture.assertBuildOutputContains("generateNormalFont")
        fixture.assertBuildOutputContains("generateArialFont")
        fixture.assertBuildOutputContains(GdxPlugin.ALL_BM_FONTS_TASK_NAME)
        fixture.assertBuildOutputContains(GdxPlugin.ALL_ASSETS_TASK_NAME)
      }

    }

    When("building") {

      fixture.build("createAllFonts")

      Then("should create the correct files") {

        fixture.assertFilesExist(
          "roboto16px.fnt",
          "roboto16px.png",
          "roboto32px.fnt",
          "roboto32px.png",
          "roboto64px.fnt",
          "roboto64px.png",
          "arial12px.fnt",
          "arial12px.png",
          "arial16px.fnt",
          "arial16px.png",
          "arial64px.fnt",
          "arial64px1.png",
          "arial64px2.png",
          "arial64px3.png",
          "custom16.fnt",
          "custom16.png",
          "custom32.fnt",
          "custom32.png"
        )

      }

      Then("should create the correct first font") {

        fixture.assertFontEquals("bitmapFont/roboto32px.fnt", "roboto32px.fnt")

      }

      Then("should create the correct second font") {

        fixture.assertFontEquals("bitmapFont/arial16px.fnt", "arial16px.fnt")

      }

    }

    When("building twice") {

      fixture.build("generateArialFont")
      fixture.build("generateArialFont")

      Then("should use configuration cache") {
        fixture.assertConfigurationCacheUsed()
      }

      Then("should be up-to-date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    When("building twice and removing one of the .fnt files after the first build") {

      fixture.build("generateNormalFont")
      fixture.output["custom32.fnt"].delete()
      fixture.build("generateNormalFont")

      Then("should use configuration cache") {
        fixture.assertConfigurationCacheUsed()
      }

      Then("should use the cache") {
        fixture.assertBuildFromCache()
      }

    }

    When("building twice and changing one of the sizes after the first build") {

      fixture.build("generateNormalFont")
      fixture.buildFileReplace("64", "48")
      fixture.build("generateNormalFont")

      Then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    When("building twice and changing one of the output files after the first build") {

      fixture.build("generateNormalFont")
      fixture.buildFileReplace("custom32", "23motsuc")
      fixture.build("generateNormalFont")

      Then("should use the cache") {
        fixture.assertBuildFromCache()
      }

    }

  }

  Given("a font with outline and shadow effects") {

    beforeContainer {

      fixture.buildFile("""

        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/outlineAndShadow.fnt')

                size 56

                settings {
                  	effects = [
                      color {
                          color = color("ffff00");
                      },
                      outline {
                          width = 3
                          color = color("00ff00")
                          join = JoinRound
                      },
                      shadow {
                          color = color("aaaaaa")
                          opacity = 0.4
                          xDistance = 4
                          yDistance = 5
                          blurKernelSize = 2
                          blurPasses = 2
                      }
                    ]
			    }
            }
        }

      """)

    }

    When("building") {

      fixture.build("createAllFonts")

      Then("should create the correct font files") {
        fixture.assertFontEquals("bitmapFont/outlineAndShadow.fnt", "outlineAndShadow.fnt")
      }

    }

  }

  Given("a font with a distance field effect") {

    beforeContainer {

      fixture.buildFile("""

        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/distanceField.fnt')

                size 32

                settings {
                	effects = [
                      distanceField {
                          color = color("ff0000")
                          scale = 32
                          spread = 4
                      }
                    ]
			    }
            }
        }

      """)

    }

    When("building") {

      fixture.build("createAllFonts")

      Then("should create the correct font files") {
        fixture.assertFontEquals("bitmapFont/distanceField.fnt", "distanceField.fnt")
      }

    }

  }

  Given("a font with a gradient effect") {

    beforeContainer {

      fixture.buildFile("""

        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/gradient.fnt')

                size 32

                settings {
                	effects = [
                      gradient {
                        topColor = color("#00AAAA")
                        bottomColor = color("ff0000")
                        offset = 4
                        scale = 2
                        cyclic = true
                      }
                    ]
			    }
            }
        }

      """)

    }

    When("building") {

      fixture.build("createAllFonts")

      Then("should create the correct font files") {
        fixture.assertFontEquals("bitmapFont/gradient.fnt", "gradient.fnt")
      }

    }

    When("building twice and changing the effect in between") {

      fixture.build("generateNormalFont")
      fixture.buildFileReplace("true", "false")
      fixture.build("generateNormalFont")

      Then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  Given("a font with a zigzag effect") {

    beforeContainer {

      fixture.buildFile("""

        import static com.github.blueboxware.gdxplugin.dsl.Constants.*

        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/zigzag.fnt')

                size 64

                settings {
                	effects = [
                      color {
                        color = color("00ffff")
                      },
                      zigzag {
                        width = 3f
                        color = color("#FFFF00")
                        wavelength = 5f
                        amplitude = 3f
                        join = JoinMiter
                      }
                    ]
			    }
            }
        }

      """)

    }

    When("building") {

      fixture.build("generateNormalFont")

      Then("should create the correct font") {
        fixture.assertBuildSuccess()
        fixture.assertFontEquals("bitmapFont/zigzag.fnt", "zigzag.fnt")
      }

    }

  }

  Given("a font with a wobble effect") {

    beforeContainer {

      fixture.buildFile("""

        import static com.github.blueboxware.gdxplugin.dsl.Constants.*

        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/wobble.fnt')

                size 64

                settings {
                	effects = [
                      color {
                        color = color("00ffff")
                      },
                      wobble {
                        width = 4f
                        color = color("#FFFF00")
                        detail = 8f
                        amplitude = 4f
                      }
                    ]
			    }
            }
        }

      """)

    }

    When("building") {

      fixture.build("generateNormalFont")

      Then("should create the correct font") {
        fixture.assertBuildSuccess()
        fixture.assertFontEquals("bitmapFont/wobble.fnt", "wobble.fnt", checkTextures = false)
      }

    }

  }

})
