import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.BitmapFont
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.internal.impldep.org.junit.Assert.*
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
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
internal class ProjectFixture(
    private val tempDir: File, private val useKotlin: Boolean = false, addClassPath: Boolean = false
) {

    val testDataDir = File("src/test/testData")

    private var buildFile: File = if (useKotlin) tempDir["build.gradle.kts"] else tempDir["build.gradle"]

    var project: Project = ProjectBuilder.builder().withProjectDir(tempDir).build()

    var input: File = tempDir["in"]
    var output: File = tempDir["out"]
    var expected: File = testDataDir["results"]

    var gradleVersion: String = "7.4.2"

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
        if (useKotlin && !TEST_RELEASED) {

            tempDir["settings.gradle.kts"].writeText(
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
            it.f()
            it.exclude(expected.name)
            it.exclude("readme")
            it.into(input)
        }
    }

    fun addFile(fileName: String) {
        project.copy { copySpec ->
            copySpec.from(testDataDir.absolutePath) {
                it.include(fileName)
            }
            copySpec.into(input)
        }
    }

    fun buildFile(contents: String, includeHeader: Boolean = true) {
        buildFile.writeText((if (includeHeader) buildFileHeader else "") + contents)
    }

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
            }.withProjectDir(tempDir).withGradleVersion(gradleVersion)
            .withArguments("-b${buildFile.name}", "--stacktrace", *args.toTypedArray())
            .withDebug(true) // https://github.com/gradle/gradle/issues/6862
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

    fun assertBuildSuccess(task: String = latestTask ?: throw AssertionError()) =
        assertEquals(TaskOutcome.SUCCESS, latestBuildResult?.task(task.prefixIfNecessary(":"))?.outcome)

    fun assertBuildFailure(errorText: String? = null, task: String = latestTask ?: throw AssertionError()) {
        assertEquals(TaskOutcome.FAILED, latestBuildResult?.task(task.prefixIfNecessary(":"))?.outcome)
        errorText?.let {
            assertBuildOutputContains(it)
        }
    }

    fun assertBuildUpToDate(task: String = latestTask ?: throw AssertionError()) =
        assertEquals(TaskOutcome.UP_TO_DATE, latestBuildResult?.task(task.prefixIfNecessary(":"))?.outcome)

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
                "diff",
                "-w",
                expectedFile.absolutePath,
                actualFile.absolutePath
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
        assertArrayEquals(expectedGlyphs.map { it.id }.sorted().toTypedArray(),
            actualGlyphs.map { it.id }.sorted().toTypedArray()
        )

        val expImages = expectedData.getImagePaths().map { ImageIO.read(File(it)) }
        val actualImages = actualData.getImagePaths().map { ImageIO.read(File(it)) }

        expectedGlyphs.forEach { expGlyph ->
            val actGlyph = actualData.getGlyph(expGlyph.id.toChar())
            assertEquals("width", expGlyph.width, actGlyph.width)
            assertEquals("height", expGlyph.height, actGlyph.height)
            assertEquals("xoffset", expGlyph.xoffset, actGlyph.xoffset)
            assertEquals("yoffset", expGlyph.yoffset, actGlyph.yoffset)
            assertEquals("xadvance", expGlyph.xadvance, actGlyph.xadvance)
            assertArrayEquals("kerning", expGlyph.kerning, actGlyph.kerning)

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
