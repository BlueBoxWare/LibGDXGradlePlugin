
import org.gradle.internal.impldep.junit.framework.TestCase.assertTrue
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

internal object TestPackTexturesTask: Spek({

  lateinit var fixture: ProjectFixture

  beforeEachTest {
    fixture = ProjectFixture()
  }

  afterEachTest {
    fixture.destroy()
  }

  given("a minimal packTextures task") {

    beforeEachTest {
      fixture.buildFile("""
        packTextures {
          from 'in'
          into 'out'
        }
      """)
    }

    on("building") {

      fixture.build("packTextures")

      it("should successfully build") {
        fixture.assertBuildSuccess()
      }

      it("should create a correct .atlas") {
        fixture.assertFileEquals("minimalSpec.atlas", "pack.atlas")
      }

    }

    on("building twice") {

      fixture.build("packTextures")
      fixture.build("packTextures")

      it("should be up to date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    on("changing the input directory after build") {

      fixture.build("packTextures")
      fixture.input["images1/sub/image.png"].delete()
      fixture.build("packTextures")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

      it("should create a new atlas") {
        fixture.assertFileEquals("minimalSpecAfterDeletingImage.atlas", "pack.atlas")
      }

    }

    on("changing the atlas after build") {

      fixture.build("packTextures")
      fixture.output["pack.atlas"].appendText("   ")
      fixture.build("packTextures")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    on("changing the spec after the build") {

      fixture.build("packTextures")
      fixture.buildFile("""
        packTextures {
          from 'in/images1'
          into 'out'
        }
      """)
      fixture.build("packTextures")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a packTextures task with a settingsFile specified") {

    beforeEachTest {

      fixture.buildFile("""
        packTextures {
          from 'in/images2'
          into 'out'
          settingsFile = file('in/images1/sub/pack.json')
        }
      """)

    }

    on("building") {

      fixture.build("packTextures")

      it("should create a correct atlas with the settings from the settings file") {
        fixture.assertFileEquals("withSettingsFile.atlas", "pack.atlas")
      }

    }

  }

  given("a packTextures task with usePackJson") {

    beforeEachTest {

      fixture.buildFile("""
        packTextures {
          from 'in'
          into 'out'
          usePackJson = true
        }
      """)

    }

    on("building") {

      fixture.build("packTextures")

      it("should create a correct atlas with the settings from the pack.json files") {
        fixture.assertFileEquals("withUsePackJson.atlas", "pack.atlas")
      }

    }

  }

  given("a packTextures task with a settings block and a custom name") {

    beforeEachTest {

      fixture.buildFile("""
       packTextures {
          from 'in'
          into 'out'
          packFileName = 'textures.custom'

          settings {
            filterMin = "MipMapLinearNearest"
            filterMag = "MipMap"
            format = "RGB565"
            useIndexes = false
            atlasExtension = ".custom"
          }
       }
      """)

    }

    on("building") {

      fixture.build("packTextures")

      it("should create a correct atlas with the custom name") {
        fixture.assertFileEquals("withSettingsAndCustomName.atlas", "textures.custom")
      }

    }

    on("building twice") {

      fixture.build("packTextures")
      fixture.build("packTextures")

      it("should be up to date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    on("changing the settings after the build") {

      fixture.build("packTextures")

      fixture.buildFile(fixture.getBuildFile().replace("MipMapLinearNearest", "Linear"))

      fixture.build("packTextures")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

      it("should create a correct new atlas") {
        fixture.assertFileEquals("withSettingsAndCustomName2.atlas", "textures.custom")
      }

    }

    on("deleting the atlas after the build") {

      fixture.build("packTextures")
      fixture.output["textures.custom"].delete()
      fixture.build("packTextures")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a named container") {

    val taskNames = arrayOf("packPack1Textures", "packPack2Textures", "packPack3Textures")

    beforeEachTest {

      fixture.buildFile("""

        texturePacks {

          pack1 {
            from 'in/images1'
            into 'out/pack1'
          }

          pack2 {
            from 'in/images2'
            into 'out/pack2'
            packFileName = "test.assets"

            settings {
              atlasExtension = ".assets"
              filterMin = "MipMapLinearLinear"
            }
          }

          pack3 {
            from 'in'
            into 'out/pack3'
          }

        }

      """)

    }

    on("listing tasks") {

      fixture.build("tasks")

      it("should contain the declared tasks") {
        taskNames.forEach {
          fixture.assertBuildOutputContains(it)
        }
      }

    }

    on("building") {

      fixture.build(extraArguments = *taskNames)

      it("should create the correct packs") {
        fixture.assertFileEquals("namedContainerPack1.atlas", "pack1/pack1.atlas")
        fixture.assertFileEquals("namedContainerPack2.atlas", "pack2/test.assets")
        fixture.assertFileEquals("namedContainerPack3.atlas","pack3/pack3.atlas")
      }

    }

    on("changing one of the tasks after build") {

      fixture.build(extraArguments =  *taskNames)
      fixture.buildFile(fixture.getBuildFile().replace("MipMapLinearLinear", "Nearest"))
      fixture.build(extraArguments = *taskNames)

      it("should only build the changed task again") {
        fixture.assertBuildUpToDate("packPack1Textures")
        fixture.assertBuildSuccess("packPack2Textures")
        fixture.assertBuildUpToDate("packPack3Textures")
      }

    }

  }

  given("a task with multiple scales with suffixes") {

    beforeEachTest {

      fixture.buildFile("""
        packTextures {
          from 'in'
          into 'out'

          settings {
            scale = [1, 2, 3]
            scaleSuffix = ["one", "two", "three"]
            scaleResampling = ["bilinear", "nearest", "bicubic"]
          }
        }
      """.trimIndent())

    }

    on("building") {

      fixture.build("packTextures")

      it("should create the correct packs") {
        fixture.assertFileEquals("multipleScales1.atlas", "packone.atlas")
        fixture.assertFileEquals("multipleScales2.atlas", "packtwo.atlas")
        fixture.assertFileEquals("multipleScales3.atlas", "packthree.atlas")
      }

    }

    on("removing one of the atlases and building again") {

      fixture.build("packTextures")
      fixture.output["packone.atlas"].delete()
      fixture.output["packtwo.atlas"].delete()
      fixture.output["packthree.atlas"].delete()
      fixture.build("packTextures")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    on("removing the suffix spec and building again") {

      fixture.build("packTextures")
      fixture.buildFile(fixture.getBuildFile().replace("one\", \"two\", \"three", "\", \"\", \""))
      fixture.build("packTextures")

      it("should create the packs in subdirectories") {
        fixture.assertFileEquals("multipleScalesSubdir1.atlas", "1/pack.atlas")
        fixture.assertFileEquals("multipleScalesSubdir2.atlas", "2/pack.atlas")
        fixture.assertFileEquals("multipleScalesSubdir3.atlas", "3/pack.atlas")
      }

    }

  }

  given("a custom task") {

    beforeEachTest {

      fixture.buildFile("""
        import com.github.blueboxware.gdxplugin.tasks.PackTextures

        task('customTask', type: PackTextures) {
          into 'out'
          from 'in/images2'

          settings {
              atlasExtension = ".assets"
              filterMin = "MipMapLinearLinear"
          }
        }
      """)

    }

    on("building") {

      fixture.build("customTask")

      it("should create a correct atlas") {
        fixture.assertFileEquals("customTask.atlas", "customTask.assets")
      }

    }

    on("changing the pack name after build") {

      fixture.build("customTask")
      fixture.buildFile(fixture.getBuildFile().replace("settings", "packFileName = 'foo'\nsettings"))
      fixture.build("customTask")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

      it("should use the new name") {
        assertTrue(fixture.output["foo.assets"].exists())
      }

    }

  }

  given("a packTextures task with filtering and renaming") {

    beforeEachTest {

      fixture.buildFile("""
        def d = copySpec {
          from('in/images2') {
            include 'b*'
            rename 'ba(.*)\\.(.*)', 'ba$1$1.$2'
          }
        }

        packTextures {
          from('in/images1') {
            exclude('sub/subsub')
          }
          into 'out'
          exclude '**/empty*'
          with d
        }
      """)

    }

    on("building") {

      fixture.build("packTextures")

      it("should create the correct atlas") {
        fixture.assertFileEquals("filteringAndRenaming.atlas", "pack.atlas")
      }

    }

    on("removing an excluded file after building") {

      fixture.build("packTextures")
      fixture.input["images2/add.png"].delete()
      fixture.build("packTextures")

      it("should be up to date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    on("removing an included file after building") {

      fixture.build("packTextures")
      fixture.input["images2/back.png"].delete()
      fixture.build("packTextures")

      it("should build again the second time") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a packTextures with (almost) all settings used") {

    beforeEachTest {

      fixture.buildFile("""
        packTextures {
          from 'in'
          into 'out'
          settings {
              paddingX = 4
              paddingY = 6
              edgePadding = false
              duplicatePadding = true
              rotation = true
              minWidth = 32
              minHeight = 128
              maxWidth = 2048
              maxHeight = 1024
              square = true
              stripWhitespaceX = true
              stripWhitespaceY = true
              alphaThreshold = 2
              filterMin = "MipMap"
              filterMag = "Linear"
              wrapX = "Repeat"
              wrapY = "MirroredRepeat"
              format = "RGBA4444"
              alias = false
              outputFormat = "jpg"
              jpegQuality = 0.1
              ignoreBlankImages = false
              fast = true
              debug = true
              combineSubdirectories = true
              flattenPaths = true
              premultiplyAlpha = true
              useIndexes = false
              bleed = true
              bleedIterations = 8
              limitMemory = true
              grid = true
              scale = [1, 2]
              scaleSuffix = ["1", "2"]
              scaleResampling = ["bilinear", "nearest"]
              atlasExtension = ".atlas"
          }
        }
      """)

    }

    on("building") {

      fixture.build("packTextures")

      it("should create a correct atlases") {
        fixture.assertFileEquals("withAllSettings1.atlas", "pack1.atlas")
        fixture.assertFileEquals("withAllSettings2.atlas", "pack2.atlas")
      }

      it("should create a correct png's") {
        fixture.assertFileEqualsBinary("withAllSettings1.jpg", "pack1.jpg")
        fixture.assertFileEqualsBinary("withAllSettings2.jpg", "pack2.jpg")
      }

    }

  }

  given("some custom settings objects") {

    beforeEachTest {

      fixture.buildFile("""
        import com.github.blueboxware.gdxplugin.tasks.PackTextures

        def packSettings = PackTextures.createSettings {
            filterMin = 'MipMapLinearNearest'
            filterMag = 'Nearest'
            maxWidth = 2048
            maxHeight = 2048
            scale = [1, 2, 3]
            outputFormat = "jpg"
        }

        def scaledPackSettings = PackTextures.createSettings(packSettings) {
            scaleSuffix = ["Normal", "Scaled", "Foo"]
            scaleResampling = ["bicubic", "bicubic", "bicubic"]
            filterMag = 'Linear'
        }

        texturePacks {
          pack1 {
            from 'in/images2'
            into 'out/pack1'
            settings = scaledPackSettings
          }
          pack2 {
            from 'in/images1'
            into 'out/pack2'
            settings = PackTextures.createSettings(scaledPackSettings) {
              debug = true
            }
          }
        }
        """)

    }

    on("building") {

      fixture.build("packPack1Textures", "packPack2Textures")

      it("should create the correct atlases and images") {
        fixture.assertFileEquals("customSettings/pack1Foo.atlas", "pack1/pack1Foo.atlas")
        fixture.assertFileEquals("customSettings/pack2Scaled.atlas", "pack2/pack2Scaled.atlas")
        fixture.assertFileEqualsBinary("customSettings/pack1Scaled.jpg", "pack1/pack1Scaled.jpg")
        fixture.assertFileEqualsBinary("customSettings/pack2Normal2.jpg", "pack2/pack2Normal2.jpg")
      }

    }

  }

})
