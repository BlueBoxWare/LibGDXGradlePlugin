import com.github.blueboxware.gdxplugin.GdxPlugin
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import org.gradle.internal.impldep.junit.framework.TestCase

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
@Suppress("unused")
internal object TestPackTexturesTask: BehaviorSpec({

  lateinit var fixture: ProjectFixture

  beforeContainer {
    fixture = ProjectFixture(tempdir())
    fixture.copyFiles {
      from(fixture.testDataDir.absolutePath) {
        it.exclude("etc")
        it.exclude("ninePatch")
      }
    }
  }

  given("nothing") {

    `when`("running the texturePackerSettingsHelp task") {

      fixture.buildFile("")
      print(fixture.build("texturePackerSettingsHelp").output)

      then("outputs the available settings") {
        fixture.assertBuildOutputContains("stripWhitespaceY")
        fixture.assertBuildOutputContains("atlasExtension")
        fixture.assertBuildOutputContains("legacyOutput")
      }

    }

  }

  given("a minimal packTextures task") {

    beforeContainer {
      fixture.buildFile("""
        packTextures {
          from 'in'
          into 'out'
        }
      """)
    }

    `when`("building") {

      fixture.build("packTextures")

      then("should successfully build") {
        fixture.assertBuildSuccess()
      }

      then("should create a correct .atlas") {
        fixture.assertFileEquals("packTextures/minimalSpec.atlas", "pack.atlas")
      }

    }

    `when`("building twice") {

      fixture.build("packTextures")
      fixture.build("packTextures")

      then("should be up-to-date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    `when`("changing the input directory after build") {

      fixture.build("packTextures")
      fixture.input["images1/sub/image.png"].delete()
      fixture.build("packTextures")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

      then("should create a new atlas") {
        fixture.assertFileEquals("packTextures/minimalSpecAfterDeletingImage.atlas", "pack.atlas")
      }

    }

    `when`("changing the atlas after build") {

      fixture.build("packTextures")
      fixture.output["pack.atlas"].appendText("   ")
      fixture.build("packTextures")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    `when`("changing the spec after the build") {

      fixture.build("packTextures")
      fixture.buildFile("""
        packTextures {
          from 'in/images1'
          into 'out'
        }
      """)
      fixture.build("packTextures")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a packTextures task with legacyOutput=false") {

    beforeContainer {
      fixture.buildFile(
        """
        packTextures {
          from 'in'
          into 'out'
          
          settings {
            legacyOutput = false
          }
          
          solid {
            name = "solid"
            width = 3
            height = 4
          }
        }
      """
      )
    }

    `when`("building") {

      fixture.build("packTextures")

      then("should create a correct .atlas") {
        fixture.assertFileEquals("packTextures/noLegacyOutput.atlas", "pack.atlas")
      }

    }

  }

  given("a packTextures task with a settingsFile specified") {

    beforeContainer {

      fixture.buildFile("""
        packTextures {
          from 'in/images2'
          into 'out'
          settingsFile = file('in/images1/sub/pack.json')
        }
      """)

    }

    `when`("building") {

      fixture.build("packTextures")

      then("should create a correct atlas with the settings from the settings file") {
        fixture.assertFileEquals("packTextures/withSettingsFile.atlas", "pack.atlas")
      }

    }

  }

  given("a packTextures task with usePackJson") {

    beforeContainer {

      fixture.buildFile("""
        packTextures {
          from 'in'
          into 'out'
          usePackJson = true
        }
      """)

    }

    `when`("building") {

      fixture.build("packTextures")

      then("should create a correct atlas with the settings from the pack.json files") {
        fixture.assertFileEquals("packTextures/withUsePackJson.atlas", "pack.atlas")
      }

    }

  }

  given("a packTextures task with a settings block and a custom name") {

    beforeContainer {

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

    `when`("building") {

      fixture.build("packTextures")

      then("should create a correct atlas with the custom name") {
        fixture.assertFileEquals("packTextures/withSettingsAndCustomName.atlas", "textures.custom")
      }

    }

    `when`("building twice") {

      fixture.build("packTextures")
      fixture.build("packTextures")

      then("should be up-to-date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    `when`("changing the settings after the build") {

      fixture.build("packTextures")

      fixture.buildFile(fixture.getBuildFile().replace("MipMapLinearNearest", "Linear"))

      fixture.build("packTextures")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

      then("should create a correct new atlas") {
        fixture.assertFileEquals("packTextures/withSettingsAndCustomName2.atlas", "textures.custom")
      }

    }

    `when`("deleting the atlas after the build") {

      fixture.build("packTextures")
      fixture.output["textures.custom"].delete()
      fixture.build("packTextures")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a named container") {

    val taskNames = arrayOf("packPack1Textures", "packPack2Textures", "packPack3Textures")

    beforeContainer {

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

    `when`("listing tasks") {

      fixture.build("tasks")

      then("should contain the declared tasks") {
        taskNames.forEach {
          fixture.assertBuildOutputContains(it)
        }
      }

    }

    `when`("building") {

      fixture.build(extraArguments = taskNames)

      then("should create the correct packs") {
        fixture.assertFileEquals("packTextures/namedContainerPack1.atlas", "pack1/pack1.atlas")
        fixture.assertFileEquals("packTextures/namedContainerPack2.atlas", "pack2/test.assets")
        fixture.assertFileEquals("packTextures/namedContainerPack3.atlas","pack3/pack3.atlas")
      }

    }

    `when`("changing one of the tasks after build") {

      fixture.build(extraArguments =  *taskNames)
      fixture.buildFile(fixture.getBuildFile().replace("MipMapLinearLinear", "Nearest"))
      fixture.build(extraArguments = *taskNames)

      then("should only build the changed task again") {
        fixture.assertBuildUpToDate("packPack1Textures")
        fixture.assertBuildSuccess("packPack2Textures")
        fixture.assertBuildUpToDate("packPack3Textures")
      }

    }

    `when`("running ${GdxPlugin.ALL_PACKS_TASK_NAME}") {

      fixture.build(GdxPlugin.ALL_PACKS_TASK_NAME)

      then("should run all texture pack tasks") {
        fixture.assertBuildSuccess("packPack1Textures")
        fixture.assertBuildSuccess("packPack2Textures")
        fixture.assertBuildSuccess("packPack3Textures")
      }

    }

    `when`("running ${GdxPlugin.ALL_PACKS_TASK_NAME}, changing one of the tasks after build and running ${GdxPlugin.ALL_PACKS_TASK_NAME} again") {

      fixture.build(GdxPlugin.ALL_PACKS_TASK_NAME)
      fixture.buildFile(fixture.getBuildFile().replace("MipMapLinearLinear", "Nearest"))
      fixture.build(GdxPlugin.ALL_PACKS_TASK_NAME)

      then("should only build the changed task again") {
        fixture.assertBuildUpToDate("packPack1Textures")
        fixture.assertBuildSuccess("packPack2Textures")
        fixture.assertBuildUpToDate("packPack3Textures")
      }

    }

  }

  given("a task with multiple scales with suffixes") {

    beforeContainer {

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

    `when`("building") {

      fixture.build("packTextures")

      then("should create the correct packs") {
        fixture.assertFileEquals("packTextures/multipleScales1.atlas", "packone.atlas")
        fixture.assertFileEquals("packTextures/multipleScales2.atlas", "packtwo.atlas")
        fixture.assertFileEquals("packTextures/multipleScales3.atlas", "packthree.atlas")
      }

    }

    `when`("removing one of the atlases and building again") {

      fixture.build("packTextures")
      fixture.output["packone.atlas"].delete()
      fixture.output["packtwo.atlas"].delete()
      fixture.output["packthree.atlas"].delete()
      fixture.build("packTextures")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    `when`("removing the suffix spec and building again") {

      fixture.build("packTextures")
      fixture.buildFile(fixture.getBuildFile().replace("one\", \"two\", \"three", "\", \"\", \""))
      fixture.build("packTextures")

      then("should create the packs in subdirectories") {
        fixture.assertFileEquals("packTextures/multipleScalesSubdir1.atlas", "1/pack.atlas")
        fixture.assertFileEquals("packTextures/multipleScalesSubdir2.atlas", "2/pack.atlas")
        fixture.assertFileEquals("packTextures/multipleScalesSubdir3.atlas", "3/pack.atlas")
      }

    }

  }

  given("a custom task") {

    beforeContainer {

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

    `when`("building") {

      fixture.build("customTask")

      then("should create a correct atlas") {
        fixture.assertFileEquals("packTextures/customTask.atlas", "customTask.assets")
      }

    }

    `when`("changing the pack name after build") {

      fixture.build("customTask")
      fixture.buildFile(fixture.getBuildFile().replace("settings", "packFileName = 'foo'\nsettings"))
      fixture.build("customTask")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

      then("should use the new name") {
        TestCase.assertTrue(fixture.output["foo.assets"].exists())
      }

    }

    `when`("running ${GdxPlugin.ALL_PACKS_TASK_NAME}") {

      fixture.build(GdxPlugin.ALL_PACKS_TASK_NAME)

      then("should run the custom task") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a packTextures task with filtering and renaming") {

    beforeContainer {

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

    `when`("building") {

      fixture.build("packTextures")

      then("should create the correct atlas") {
        fixture.assertFileEquals("packTextures/filteringAndRenaming.atlas", "pack.atlas")
      }

    }

    `when`("removing an excluded file after building") {

      fixture.build("packTextures")
      fixture.input["images2/add.png"].delete()
      fixture.build("packTextures")

      then("should be up-to-date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    `when`("removing an included file after building") {

      fixture.build("packTextures")
      fixture.input["images2/back.png"].delete()
      fixture.build("packTextures")

      then("should build again the second time") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a packTextures with (almost) all settings used") {

    beforeContainer {

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

    `when`("building") {

      fixture.build("packTextures")

      then("should create correct results") {
        fixture.assertFileEquals("packTextures/withAllSettings1.atlas", "pack1.atlas")
        fixture.assertFileEquals("packTextures/withAllSettings2.atlas", "pack2.atlas")
      }

      then("should create correct images") {
        fixture.assertFileEqualsBinary("packTextures/withAllSettings1.jpg", "pack1.jpg")
        fixture.assertFileEqualsBinary("packTextures/withAllSettings2.jpg", "pack2.jpg")
      }

    }

  }

  given("some custom settings objects (using PackTextures.createSettings)") {

    beforeContainer {

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

    `when`("building") {

      fixture.build("packPack1Textures", "packPack2Textures")

      then("should create the correct atlases and images") {
        fixture.assertFileEquals("packTextures/customSettings/pack1Foo.atlas", "pack1/pack1Foo.atlas")
        fixture.assertFileEquals("packTextures/customSettings/pack2Scaled.atlas", "pack2/pack2Scaled.atlas")
        fixture.assertFileEqualsBinary("packTextures/customSettings/pack1Scaled.jpg", "pack1/pack1Scaled.jpg")
        fixture.assertFileEqualsBinary("packTextures/customSettings/pack2Normal2.jpg", "pack2/pack2Normal2.jpg")
      }

    }

  }

  given("some custom settings objects (using packSettings)") {

    beforeContainer {

      fixture.buildFile("""
        import static com.github.blueboxware.gdxplugin.dsl.Utils.*

        def baseSettings = packSettings {
            filterMin = 'MipMapLinearNearest'
            filterMag = 'Nearest'
            maxWidth = 2048
            maxHeight = 2048
            scale = [1, 2, 3]
            outputFormat = "jpg"
        }

        def scaledPackSettings = packSettings(baseSettings) {
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
            settings = packSettings(scaledPackSettings) {
              debug = true
            }
          }
        }
        """)

    }

    `when`("building") {

      fixture.build("packPack1Textures", "packPack2Textures")

      then("should create the correct atlases and images") {
        fixture.assertFileEquals("packTextures/customSettings/pack1Foo.atlas", "pack1/pack1Foo.atlas")
        fixture.assertFileEquals("packTextures/customSettings/pack2Scaled.atlas", "pack2/pack2Scaled.atlas")
        fixture.assertFileEqualsBinary("packTextures/customSettings/pack1Scaled.jpg", "pack1/pack1Scaled.jpg")
        fixture.assertFileEqualsBinary("packTextures/customSettings/pack2Normal2.jpg", "pack2/pack2Normal2.jpg")
      }

    }

  }

  given("a pack textures task with solids") {

    beforeContainer {

      fixture.buildFile("""
        packTextures {

          into 'out'

          solid {
              name = "white"
          }

          solid {
              name = "red"
              color = color("#ff0000");
              width = 3
              height = 4
          }

          solid {
              name = "green"
              color = color("#00ff00ff");
              height = 4
          }

        }
      """)

    }

    `when`("building") {

      fixture.build("packTextures")

      then("should create the correct atlas") {
        fixture.assertFileEquals("packTextures/solids.atlas", "pack.atlas")
        fixture.assertFileEqualsBinary("packTextures/solids.png", "pack.png")
      }

    }

  }



})
