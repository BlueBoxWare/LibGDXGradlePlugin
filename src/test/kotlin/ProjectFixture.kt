
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.internal.impldep.org.junit.Assert.assertEquals
import org.gradle.internal.impldep.org.junit.Assert.assertTrue
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
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
internal class ProjectFixture(copyFiles: Boolean = true) {

  private val testDataDir = File("src/test/testData")

  private var tempDir: TemporaryFolder = TemporaryFolder().apply { create() }
  private var buildFile: File = tempDir["build.gradle"]

  var project: Project = ProjectBuilder.builder().withProjectDir(tempDir.root).build()

  var input: File = tempDir["in"]
  var output: File = tempDir["out"]
  var expected: File = testDataDir["results"]

  var gradleVersion: GradleVersion = GradleVersion.current()

  private val buildFileHeader = """
            plugins {
              id 'com.github.blueboxware.gdx'
            }
  """

  init {
    if (copyFiles) {
      project.copy {
        it.from(testDataDir.absolutePath) {
          it.exclude(expected.name)
          it.exclude("etc")
        }
        it.into(input)
      }
    }
  }

  fun destroy() {
    tempDir.delete()
  }

  fun buildFile(contents: String, includeHeader: Boolean = true) {
    buildFile.writeText((if (includeHeader) buildFileHeader else "") + contents)
  }

  fun getBuildFile() = buildFile.readText().removePrefix(buildFileHeader)

  fun build(vararg arguments: String): BuildResult =
          GradleRunner
                  .create()
                  .withPluginClasspath()
                  .withProjectDir(tempDir.root)
                  .withGradleVersion(gradleVersion.version)
                  .withArguments(*arguments)
                  .build()

  fun addFile(fileName: String) {
    project.copy {
      it.from(testDataDir.absolutePath) {
        it.include(fileName)
      }
      it.into(input)
    }
  }

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