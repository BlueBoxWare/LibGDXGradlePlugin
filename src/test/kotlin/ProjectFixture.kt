
import org.gradle.api.Project
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
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
  private var expected: File = testDataDir["results"]

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
                  .withArguments(*arguments)
                  .build()

  fun assertFileEquals(expectedFileName: String, actualFileName: String)= assertFileEquals(expected[expectedFileName], output[actualFileName])

}