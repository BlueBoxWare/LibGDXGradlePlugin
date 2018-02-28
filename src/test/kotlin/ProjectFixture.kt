
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.internal.impldep.org.junit.Assert.assertEquals
import org.gradle.internal.impldep.org.junit.Assert.assertTrue
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import java.io.File

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
internal class ProjectFixture(private val useKotlin: Boolean = false) {

  val testDataDir = File("src/test/testData")

  private var tempDir: TemporaryFolder = TemporaryFolder().apply { create() }
  private var buildFile: File = if (useKotlin) tempDir["build.gradle.kts"] else tempDir["build.gradle"]

  var project: Project = ProjectBuilder.builder().withProjectDir(tempDir.root).build()

  var input: File = tempDir["in"]
  var output: File = tempDir["out"]
  var expected: File = testDataDir["results"]

  var gradleVersion: GradleVersion = GradleVersion.current()

  private var latestBuildResult: BuildResult? = null
  private var latestTask: String? = null

  private val buildFileHeader = if (useKotlin) "" else """
            plugins {
              id 'com.github.blueboxware.gdx'
            }
  """

  init {
    if (useKotlin) {
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
    latestBuildResult = GradleRunner
            .create()
            .apply {
              // https://github.com/gradle/kotlin-dsl/issues/492
              if (!useKotlin) {
                withPluginClasspath()
              }
            }
            .withProjectDir(tempDir.root)
            .withGradleVersion(gradleVersion.version)
            .withArguments(args)
            .build()
    return latestBuildResult ?: throw AssertionError("No")
  }

  fun assertBuildOutputContains(substring: String) = assert(latestBuildResult?.output?.contains(substring) == true)

  fun assertBuildSuccess(task: String = latestTask ?: throw AssertionError()) =
          assertEquals(TaskOutcome.SUCCESS, latestBuildResult?.task(task.prefixIfNecessary(":"))?.outcome)

  fun assertBuildUpToDate(task: String = latestTask ?: throw AssertionError()) =
          assertEquals(TaskOutcome.UP_TO_DATE, latestBuildResult?.task(task.prefixIfNecessary(":"))?.outcome)

  fun assertFileEquals(expectedFileName: String, actualFileName: String) {
    assertFileEquals(expected[expectedFileName], output[actualFileName])
  }

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

  private fun checkFilesExist(expectedFile: File, actualFile: File) {
    assertTrue("File with expected results doesn't exist ('${expectedFile.absolutePath}')", expectedFile.exists())
    assertTrue("File with actual results doesn't exist ('${actualFile.absolutePath}')", actualFile.exists())
  }

}