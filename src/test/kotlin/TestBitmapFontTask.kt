import com.github.blueboxware.gdxplugin.GdxPlugin
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

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
internal object TestBitmapFontTask: Spek({

  lateinit var fixture: ProjectFixture

  beforeEachTest {
    fixture = ProjectFixture(addClassPath = true)
    fixture.addFile("etc/roboto.ttf")
  }

  afterEachTest {
    fixture.destroy()
  }

  given("a bitmap font task container") {

    beforeEachTest {

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

    on("listing tasks") {

      fixture.build("tasks")

      it("should contain the expected tasks") {
        fixture.assertBuildOutputContains("generateNormalFont")
        fixture.assertBuildOutputContains("generateArialFont")
        fixture.assertBuildOutputContains(GdxPlugin.ALL_BM_FONTS_TASK_NAME)
        fixture.assertBuildOutputContains(GdxPlugin.ALL_ASSETS_TASK_NAME)
      }

    }

    on("building") {

      fixture.build("createAllFonts")

      it("should create the correct files") {

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

      it("should create the correct first font") {

        fixture.assertFontEquals("bitmapFont/roboto32px.fnt", "roboto32px.fnt")

      }

      it("should create the correct second font") {

        fixture.assertFontEquals("bitmapFont/arial16px.fnt", "arial16px.fnt")

      }

    }

    on("building twice") {

      fixture.build("generateArialFont")
      fixture.build("generateArialFont")

      it("should be up to date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    on("building twice and removing one of the .fnt files after the first build") {

      fixture.build("generateNormalFont")
      fixture.output["custom32.fnt"].delete()
      fixture.build("generateNormalFont")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    on("building twice and changing one of the sizes after the first build") {

      fixture.build("generateNormalFont")
      fixture.buildFile(fixture.getBuildFile().replace("64", "48"))
      fixture.build("generateNormalFont")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    on("building twice and changing one of the output files after the first build") {

      fixture.build("generateNormalFont")
      fixture.buildFile(fixture.getBuildFile().replace("custom32", "23motsuc"))
      fixture.build("generateNormalFont")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a font with outline and shadow effects") {

    beforeEachTest {

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

    on("building") {

      fixture.build("createAllFonts")

      it("should create the correct font files") {
        fixture.assertFontEquals("bitmapFont/outlineAndShadow.fnt", "outlineAndShadow.fnt")
      }

    }

  }

  given("a font with a distance field effect") {

    beforeEachTest {

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

    on("building") {

      fixture.build("createAllFonts")

      it("should create the correct font files") {
        fixture.assertFontEquals("bitmapFont/distanceField.fnt", "distanceField.fnt")
      }

    }

  }

  given("a font with a gradient effect") {

    beforeEachTest {

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

    on("building") {

      fixture.build("createAllFonts")

      it("should create the correct font files") {
        fixture.assertFontEquals("bitmapFont/gradient.fnt", "gradient.fnt")
      }

    }

    on("building twice and changing the effect in between") {

      fixture.build("generateNormalFont")
      fixture.buildFile(fixture.getBuildFile().replace("true", "false"))
      fixture.build("generateNormalFont")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a font with a zigzag effect") {

    beforeEachTest {

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

    on("building") {

      fixture.build("generateNormalFont")

      it("should create the correct font") {
        fixture.assertBuildSuccess()
        fixture.assertFontEquals("bitmapFont/zigzag.fnt", "zigzag.fnt")
      }

    }

  }

})