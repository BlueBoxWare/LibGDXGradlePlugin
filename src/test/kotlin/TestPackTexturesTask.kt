
import org.gradle.internal.impldep.junit.framework.TestCase.assertEquals
import org.gradle.internal.impldep.junit.framework.TestCase.assertTrue
import org.gradle.testkit.runner.TaskOutcome
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

      val result = fixture.build("packTextures")

      it("should successfully build") {
        assertEquals(TaskOutcome.SUCCESS, result.task(":packTextures")?.outcome)
      }

      it("should create a correct .atlas") {
        fixture.assertFileEquals("minimalSpec.atlas", "pack.atlas")
      }

    }

    on("building twice") {

      fixture.build("packTextures")
      val secondResult = fixture.build("packTextures")

      it("should be up to date the second time") {
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":packTextures")?.outcome)
      }

    }

    on("changing the input directory after build") {

      fixture.build("packTextures")
      fixture.input["images1/sub/image.png"].delete()
      val secondResult = fixture.build("packTextures")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":packTextures")?.outcome)
      }

      it("should create a new atlas") {
        fixture.assertFileEquals("minimalSpecAfterDeletingImage.atlas", "pack.atlas")
      }

    }

    on("changing the atlas after build") {

      fixture.build("packTextures")
      fixture.output["pack.atlas"].appendText("   ")
      val secondResult = fixture.build("packTextures")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":packTextures")?.outcome)
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
      val secondResult = fixture.build("packTextures")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":packTextures")?.outcome)
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
      val secondResult = fixture.build("packTextures")

      it("should be up to date the second time") {
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":packTextures")?.outcome)
      }

    }

    on("changing the settings after the build") {

      fixture.build("packTextures")

      fixture.buildFile(fixture.getBuildFile().replace("MipMapLinearNearest", "Linear"))

      val secondResult = fixture.build("-Dorg.gradle.debug=true", "packTextures")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":packTextures")?.outcome)
      }

      it("should create a correct new atlas") {
        fixture.assertFileEquals("withSettingsAndCustomName2.atlas", "textures.custom")
      }

    }

    on("deleting the atlas after the build") {

      fixture.build("packTextures")
      fixture.output["textures.custom"].delete()
      val secondResult = fixture.build("packTextures")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":packTextures")?.outcome)
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

      val result = fixture.build("tasks")

      it("should contain the declared tasks") {
        taskNames.forEach {
          assertTrue(result.output, result.output.contains(it))
        }
      }

    }

    on("building") {

      fixture.build(*taskNames)

      it("should create the correct packs") {
        fixture.assertFileEquals("namedContainerPack1.atlas", "pack1/pack1.atlas")
        fixture.assertFileEquals("namedContainerPack2.atlas", "pack2/test.assets")
        fixture.assertFileEquals("namedContainerPack3.atlas","pack3/pack3.atlas")
      }

    }

    on("changing one of the tasks after build") {

      fixture.build(*taskNames)
      fixture.buildFile(fixture.getBuildFile().replace("MipMapLinearLinear", "Nearest"))
      val result = fixture.build(*taskNames)

      it("should only build the changed task again") {
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":packPack1Textures")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":packPack2Textures")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":packPack3Textures")?.outcome)
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
      val secondResult = fixture.build("packTextures")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":packTextures")?.outcome)
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
        task('customTask', type: com.github.blueboxware.gdxplugin.tasks.PackTextures) {
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
      val secondResult = fixture.build("customTask")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":customTask")?.outcome)
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
      val secondResult = fixture.build("packTextures")

      it("should be up to date the second time") {
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":packTextures")?.outcome)
      }

    }

    on("removing an included file after building") {

      fixture.build("packTextures")
      fixture.input["images2/back.png"].delete()
      val secondResult = fixture.build("packTextures")

      it("should build again the second time") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":packTextures")?.outcome)
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

      fixture.build("packT")

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

})
