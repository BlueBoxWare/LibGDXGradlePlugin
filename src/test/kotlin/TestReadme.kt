
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
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
@RunWith(Parameterized::class)
internal class TestReadme(
        private val source: String,
        private val arg: String,
        useKotlin: Boolean,
        @Suppress("unused") private val id: String
) {

  private val fixture: ProjectFixture = ProjectFixture(useKotlin)

  init {
    fixture.project.copy {
      it.from(fixture.testDataDir["readme"].absolutePath)
      it.into(fixture.project.rootDir)
    }
  }

  @After
  fun destroy() {
//    fixture.destroy()
  }

  @Test
  fun test() {
    fixture.buildFile(source)
    arg.split(" ").forEach {
      with(fixture) {
        build(it)
        assertBuildSuccess()
        build(it)
        assertBuildUpToDate()
      }
    }
  }

  companion object {

    private val TEST_REGEX = Regex("<test(Groovy|Kotlin)([^>]*)>(.*?)</test(Groovy|Kotlin)>", option = RegexOption.DOT_MATCHES_ALL)
    private val ARG_REGEX = Regex("""arg="([^"]*)"""")
    private val ID_REGEX = Regex("""id="([^"]*)"""")

    @Parameterized.Parameters(name = "{3}")
    @JvmStatic
    fun tests(): List<Array<Any>> {

      val pluginVersion = getCurrentVersion()

      return TEST_REGEX.findAll(File("README.md.src").readText()).map { matchResult ->
        val useKotlin = matchResult.groupValues[1] == "Kotlin"
        val src = matchResult.groupValues[3]
                .replace(Regex("</?exclude>", RegexOption.DOT_MATCHES_ALL), "")
                .replace("<currentVersion>", pluginVersion)
        val args = ARG_REGEX.find(matchResult.groupValues[2])?.groupValues?.get(1) ?: throw AssertionError()
        val id = ID_REGEX.find(matchResult.groupValues[2])?.groupValues?.get(1) ?: "<unknown>"
        arrayOf(src, args, useKotlin, id + " (${matchResult.groupValues[1]})")
      }.toList()

    }

  }

}