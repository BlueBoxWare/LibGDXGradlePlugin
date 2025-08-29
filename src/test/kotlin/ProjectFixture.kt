import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.BitmapFont
import io.kotest.common.ExperimentalKotest
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.internal.impldep.org.junit.Assert.*
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import java.io.File
import javax.imageio.ImageIO

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
@OptIn(ExperimentalKotest::class)
internal class ProjectFixture(
  private val tempDir: File, private val useKotlin: Boolean = false, addClassPath: Boolean = false
) {

  companion object {
    @Suppress("MayBeConstant", "RedundantSuppression")
    val TEST_RELEASED = false

    val GRADLE_VERSION: String = System.getProperty("gdxplugin.gradle.version") ?: GradleVersion.current().version

    val useBuildCache = System.getProperty("gdxplugin.cache.build") == "true"
    val useConfigurationCache = System.getProperty("gdxplugin.cache.configuration") == "true"

    internal fun getVersion() = if (TEST_RELEASED) getReleasedVersion() else getCurrentVersion()
  }

  val testDataDir = File("src/test/testData")

  private var buildFile: File = if (useKotlin) tempDir["build.gradle.kts"] else tempDir["build.gradle"]

  var project: Project = ProjectBuilder.builder().withProjectDir(tempDir).build()

  var input: File = tempDir["in"]
  var output: File = tempDir["out"]
  var cache: File = tempDir["cache"]
  var settingsFile: File = if (useKotlin)
  { tempDir["settings.gradle.kts"] } else { tempDir["settings.gradle"]}
  var expected: File = testDataDir["results"]

  private var latestBuildResult: BuildResult? = null
  private var latestTask: String? = null

  private val buildFileHeader = if (useKotlin) "" else """
      ${
    if (addClassPath && !TEST_RELEASED) """
        buildscript {
          repositories {
            mavenLocal()
            mavenCentral()
          }
          dependencies {
            classpath "com.github.blueboxware:LibGDXGradlePlugin:${getVersion()}"
          }
        }
      """ else ""
  }
      plugins {
        id 'com.github.blueboxware.gdx' version '${getVersion()}'
      }
  """

  init {
    settingsFile.writeText(
      """
        buildCache {
            local {
                directory = "${cache.toURI()}"
            }
        }      

      """.trimIndent()
    )
    if (useKotlin && !TEST_RELEASED) {
      settingsFile.appendText(
        """
        pluginManagement {
          repositories {
            mavenLocal()
            mavenCentral()
          }
        }
      """.trimIndent()
      )
    }
  }

  fun copyFiles(f: CopySpec.() -> Unit) {
    project.copy {
      f()
      exclude(expected.name)
      exclude("readme")
      into(input)
    }
  }

  fun addFile(fileName: String) {
    project.copy {
      from(testDataDir.absolutePath) {
        include(fileName)
      }
      into(input)
    }
  }

  fun buildFile(contents: String, includeHeader: Boolean = true) {
    buildFile.writeText((if (includeHeader) buildFileHeader else "") + contents)
  }

  fun buildFileReplace(old: String, new: String) = buildFile(getBuildFile().replace(old, new))

  fun getBuildFile() = buildFile.readText().removePrefix(buildFileHeader)

  fun build(taskName: String? = null, vararg extraArguments: String, shouldFail: Boolean = false): BuildResult {
    val args = extraArguments.toMutableList()
    taskName?.let { args.add(taskName) }
    latestTask = taskName
    val runner = GradleRunner.create().apply {
      // https://github.com/gradle/kotlin-dsl/issues/492
      if (!useKotlin && !TEST_RELEASED) {
        withPluginClasspath()
      }
    }.withProjectDir(tempDir).withGradleVersion(GRADLE_VERSION).withArguments(
      "--stacktrace",
      "--info",
      "-Dorg.gradle.caching.debug=true",
      "-Dorg.gradle.configuration-cache.integrity-check=true",
      "--warning-mode",
      "all",
      if (useBuildCache) "--build-cache" else "--no-build-cache",
      if (useConfigurationCache) "--configuration-cache" else "--no-configuration-cache",
      *args.toTypedArray()
    ).withDebug(!useBuildCache && !useConfigurationCache)
    latestBuildResult = if (shouldFail) {
      runner.buildAndFail()
    } else {
      runner.build()
    }
    return latestBuildResult ?: throw AssertionError("No")
  }

  fun assertBuildOutputContains(vararg strings: String) =
    latestBuildResult?.output?.let { output -> assertTrue(output, strings.any { output.contains(it) }) }
      ?: throw AssertionError("No build output")


  fun assertBuildFailure(errorText: String? = null, task: String = latestTask ?: throw AssertionError()) {
    assertBuildOutcome(TaskOutcome.FAILED, task)
    errorText?.let {
      assertBuildOutputContains(it)
    }
  }

  fun assertBuildSuccess(task: String = latestTask ?: throw AssertionError()) =
    assertBuildOutcome(TaskOutcome.SUCCESS, task)

  fun assertBuildUpToDate(task: String = latestTask ?: throw AssertionError()) =
    assertBuildOutcome(TaskOutcome.UP_TO_DATE, task)

  fun assertBuildFromCache(task: String = latestTask ?: throw AssertionError()) =
    if (useBuildCache) {
      assertBuildOutcome(TaskOutcome.FROM_CACHE, task)
    } else {
      assertBuildSuccess(task)
    }

  fun assertBuildOutcome(outcome: TaskOutcome, task: String) =
    assertEquals(outcome, latestBuildResult?.task(task.prefixIfNecessary(":"))?.outcome)

  fun assertConfigurationCacheUsed() {
    if (useConfigurationCache) {
      assertTrue(latestBuildResult?.output, latestBuildResult?.output?.contains("Reusing configuration cache.") == true)
    }
  }

  fun assertFileEquals(expectedFileName: String, actualFileName: String) =
    assertFileEquals(expected[expectedFileName], output[actualFileName])

  private fun runExternalCommand(vararg args: String): Pair<String, Int> {
    val cmd = Runtime.getRuntime().exec(args)
    val result = cmd.waitFor()
    return Pair(cmd.inputStream?.reader()?.readText() ?: "", result)
  }

  private fun assertFileEquals(expectedFile: File, actualFile: File, showFullContents: Boolean = false) {
    if (!expectedFile.exists()) {
      expectedFile.createNewFile()
      expectedFile.writeText(actualFile.readText())
    } else {
      val (diff, result) = runExternalCommand("diff", "-d", expectedFile.absolutePath, actualFile.absolutePath)
      val (_, whitespaceOnly) = runExternalCommand(
        "diff", "-w", expectedFile.absolutePath, actualFile.absolutePath
      )

      if (result == 1 || expectedFile.readText() != actualFile.readText()) {

        var msg = "Actual file '${actualFile.name}' differs from expected file '${expectedFile.name}':\n"
        if (whitespaceOnly == 0) {
          msg += "Whitespace only.\n"
        }
        msg += "=== DIFF =====================================================================================\n"
        msg += diff

        if (showFullContents) {
          msg += "=== ACTUAL ===================================================================================\n"
          msg += actualFile.readText()
          msg += "\n\n"

          msg += "=== EXPECTED =================================================================================\n"
          msg += expectedFile.readText()
          msg += "\n\n"
        }
        msg += "==============================================================================================\n"
        fail(msg)
      }
    }
  }

  fun assertFileEqualsBinary(expectedFileName: String, actualFileName: String) {
    assertFileEqualsBinary(expected[expectedFileName], output[actualFileName])
  }

  fun assertFileEqualsBinary(expectedFile: File, actualFile: File) {
    checkFilesExist(expectedFile, actualFile)
    assertTrue(
      "Actual output file '${actualFile.name}' differs from expected output file '${expectedFile.name}",
      FileUtils.contentEquals(expectedFile, actualFile)
    )
  }

  fun assertFontEquals(expectedFile: String, actualFile: String, checkTextures: Boolean = true) {
    assertFontEquals(expected[expectedFile], output[actualFile], checkTextures)
  }

  fun assertNinePatchEquals(
    expectedSplits: List<Int>, expectedPads: List<Int>?, expectedImageFile: File, actualImageFile: File
  ) {
    getRect(actualImageFile).let {
      assertEquals(expectedSplits, it.splits.toList())
      if (expectedPads != null) {
        assertEquals(expectedPads, it.pads.toList())
      }
      val expectedImage = ImageIO.read(expectedImageFile)
      val actualImage = it.getImage(null)
      assertEquals(expectedImage.width, actualImage.width)
      assertEquals(expectedImage.height, actualImage.height)
      for (x in 0 until expectedImage.width) {
        for (y in 0 until expectedImage.height) {
          if (expectedImage.getRGB(x, y) != actualImage.getRGB(x, y)) {
            throw AssertionError("Expected image and actual image differ at $x, $y")
          }
        }
      }
    }
  }

  private fun assertFontEquals(expectedFile: File, actualFile: File, checkTextures: Boolean = true) {
    checkFilesExist(expectedFile, actualFile)

    val expectedData = BitmapFont.BitmapFontData(FileHandle(expectedFile), false)
    val actualData = BitmapFont.BitmapFontData(FileHandle(actualFile), false)

    assertEquals(expectedData.padTop, actualData.padTop)
    assertEquals(expectedData.padBottom, actualData.padBottom)
    assertEquals(expectedData.padLeft, actualData.padLeft)
    assertEquals(expectedData.padRight, actualData.padRight)
    assertEquals(expectedData.lineHeight, actualData.lineHeight)
    assertEquals(expectedData.capHeight, actualData.capHeight)

    val expectedGlyphs = expectedData.glyphs.flatMap { it?.toList() ?: listOf() }.filterNotNull()
    val actualGlyphs = actualData.glyphs.flatMap { it?.toList() ?: listOf() }.filterNotNull()
    assertArrayEquals(
      expectedGlyphs.map { it.id }.sorted().toTypedArray(),
      actualGlyphs.map { it.id }.sorted().toTypedArray()
    )

    val expImages = expectedData.getImagePaths().map { ImageIO.read(File(it)) }
    val actualImages = actualData.getImagePaths().map { ImageIO.read(File(it)) }

    expectedGlyphs.forEach { expGlyph ->
      val id = expGlyph.id
      val actGlyph = actualData.getGlyph(id.toChar())
      assertEquals("width of $id", expGlyph.width, actGlyph.width)
      assertEquals("height of $id", expGlyph.height, actGlyph.height)
      assertEquals("xoffset of $id", expGlyph.xoffset, actGlyph.xoffset)
      assertEquals("yoffset of $id", expGlyph.yoffset, actGlyph.yoffset)
      assertEquals("xadvance of $id", expGlyph.xadvance, actGlyph.xadvance)
      assertArrayEquals("kerning of $id", expGlyph.kerning, actGlyph.kerning)

      if (checkTextures) {
        for (x in 0 until expGlyph.width) {
          for (y in 0 until expGlyph.height) {
            if (expImages[expGlyph.page].getRGB(x, y) != actualImages[actGlyph.page].getRGB(x, y)) {
              throw AssertionError("Texture for ${expGlyph.id} differs")
            }
          }
        }
      }

    }
  }

  fun assertFilesExist(vararg fileNames: String) {
    fileNames.forEach {
      assertTrue("File '$it' doesn't exist", output[it].exists())
    }
  }

  private fun checkFilesExist(expectedFile: File, actualFile: File) {
    assertTrue("File with actual results doesn't exist ('${actualFile.absolutePath}')", actualFile.exists())
    if (!expectedFile.exists()) {
      expectedFile.writeText(actualFile.readText())
      assertTrue("File with expected results doesn't exist ('${expectedFile.absolutePath}'). Created.", false)
    }
  }


}
