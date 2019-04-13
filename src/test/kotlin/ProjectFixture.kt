
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.BitmapFont
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.internal.impldep.org.junit.Assert.*
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
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
internal class ProjectFixture(private val useKotlin: Boolean = false, addClassPath: Boolean = false) {

  val testDataDir = File("src/test/testData")

  private var tempDir: TemporaryFolder = TemporaryFolder().apply { create() }
  private var buildFile: File = if (useKotlin) tempDir["build.gradle.kts"] else tempDir["build.gradle"]

  var project: Project = ProjectBuilder.builder().withProjectDir(tempDir.root).build()

  var input: File = tempDir["in"]
  var output: File = tempDir["out"]
  var expected: File = testDataDir["results"]

  var gradleVersion: String = "5.3.1"

  private var latestBuildResult: BuildResult? = null
  private var latestTask: String? = null

  private val buildFileHeader = if (useKotlin) "" else """
      ${if (addClassPath && !TEST_RELEASED) """
        buildscript {
          repositories {
            mavenLocal()
            mavenCentral()
          }
          dependencies {
            classpath "com.github.blueboxware:LibGDXGradlePlugin:${ProjectFixture.getVersion()}"
          }
        }
      """ else ""}

      plugins {
        id 'com.github.blueboxware.gdx' version '${ProjectFixture.getVersion()}'
      }
  """

  init {
    if (useKotlin && !TEST_RELEASED) {
      if (GradleVersion.version(gradleVersion) < GradleVersion.version("4.4")) {
        tempDir["settings.gradle"].writeText("""
          pluginManagement {
            repositories {
              maven {
                url 'file://${File(System.getProperty("user.home"), "/.m2/repository").absolutePath}'
              }
              maven {
                 url 'https://repo.maven.apache.org/maven2/'
              }
            }
          }
        """)
      } else {
        tempDir["settings.gradle.kts"].writeText("""
        pluginManagement {
          repositories {
            mavenLocal()
            mavenCentral()
          }
        }
      """.trimIndent())
      }
    }
  }

  fun destroy() {
    tempDir.delete()
  }

  fun copyFiles(f: CopySpec.() -> Unit) {
    project.copy {
      it.f()
      it.exclude(expected.name)
      it.exclude("readme")
      it.into(input)
    }
  }

  fun addFile(fileName: String) {
    project.copy {
      it.from(testDataDir.absolutePath) {
        it.include(fileName)
      }
      it.into(input)
    }
  }

  fun buildFile(contents: String, includeHeader: Boolean = true) {
    buildFile.writeText((if (includeHeader) buildFileHeader else "") + contents)
  }

  fun getBuildFile() = buildFile.readText().removePrefix(buildFileHeader)

  fun build(taskName: String? = null, vararg extraArguments: String): BuildResult {
    val args = extraArguments.toMutableList()
    taskName?.let { args.add(taskName) }
    latestTask = taskName
    val runner = GradleRunner
            .create()
            .apply {
              // https://github.com/gradle/kotlin-dsl/issues/492
              if (!useKotlin && !TEST_RELEASED) {
                withPluginClasspath()
              }
            }
            .withProjectDir(tempDir.root)
            .withGradleVersion(gradleVersion)
            .withArguments("-b${buildFile.name}", *args.toTypedArray())
//            .withDebug(true) // https://github.com/gradle/gradle/issues/6862
    latestBuildResult = runner.build()
    return latestBuildResult ?: throw AssertionError("No")
  }

  fun assertBuildOutputContains(substring: String) =
          latestBuildResult?.output?.let { assertTrue(it, it.contains(substring)) } ?: throw AssertionError("No build output")

  fun assertBuildSuccess(task: String = latestTask ?: throw AssertionError()) =
          assertEquals(TaskOutcome.SUCCESS, latestBuildResult?.task(task.prefixIfNecessary(":"))?.outcome)

  fun assertBuildUpToDate(task: String = latestTask ?: throw AssertionError()) =
          assertEquals(TaskOutcome.UP_TO_DATE, latestBuildResult?.task(task.prefixIfNecessary(":"))?.outcome)

  fun assertFileEquals(expectedFileName: String, actualFileName: String) =
          assertFileEquals(expected[expectedFileName], output[actualFileName])

  private fun assertFileEquals(expectedFile: File, actualFile: File) {
    checkFilesExist(expectedFile, actualFile)
    assertEquals(expectedFile.readText(), actualFile.readText())
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

  fun assertNinePatchEquals(expectedSplits: List<Int>, expectedPads: List<Int>?, expectedImageFile: File, actualImageFile: File) {
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
    assertArrayEquals(expectedGlyphs.map { it.id }.sorted().toTypedArray(), actualGlyphs.map { it.id }.sorted().toTypedArray())

    val expImages = expectedData.getImagePaths().map { ImageIO.read(File(it)) }
    val actualImages = actualData.getImagePaths().map { ImageIO.read(File(it)) }

    expectedGlyphs.forEach { expGlyph ->
      val actGlyph = actualData.getGlyph(expGlyph.id.toChar())
      assertEquals(expGlyph.width, actGlyph.width)
      assertEquals(expGlyph.height, actGlyph.height)
      assertEquals(expGlyph.xoffset, actGlyph.xoffset)
      assertEquals(expGlyph.yoffset, actGlyph.yoffset)
      assertEquals(expGlyph.xadvance, actGlyph.xadvance)
      assertArrayEquals(expGlyph.kerning, actGlyph.kerning)

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
    assertTrue("File with expected results doesn't exist ('${expectedFile.absolutePath}')", expectedFile.exists())
    assertTrue("File with actual results doesn't exist ('${actualFile.absolutePath}')", actualFile.exists())
  }

  companion object {
    const val TEST_RELEASED = false

    @Suppress("ConstantConditionIf")
    fun getVersion() = if (TEST_RELEASED) getReleasedVersion() else getCurrentVersion()
  }

}