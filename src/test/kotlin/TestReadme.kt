import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.IsStableType
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import java.io.File

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
internal object TestReadme : BehaviorSpec({

  val testRegex = Regex("<test(Groovy|Kotlin)([^>]*)>(.*?)</test(Groovy|Kotlin)>", option = RegexOption.DOT_MATCHES_ALL)
  val argRegex = Regex("""arg="([^"]*)"""")
  val idRegex = Regex("""id="([^"]*)"""")

  @IsStableType
  data class Test(val source: String, val args: String, val useKotlin: Boolean, val id: String, val noCache: Boolean)

  fun tests(): Sequence<Test> {

    val pluginVersion = ProjectFixture.getVersion()

   return testRegex.findAll(File("README.md.src").readText()).map { matchResult ->
      val useKotlin = matchResult.groupValues[1] == "Kotlin"
      val src = matchResult.groupValues[3].replace(Regex("</?exclude>", RegexOption.DOT_MATCHES_ALL), "")
        .replace("<currentVersion>", pluginVersion)
      val args = argRegex.find(matchResult.groupValues[2])?.groupValues?.get(1) ?: throw AssertionError()
      val id = idRegex.find(matchResult.groupValues[2])?.groupValues?.get(1) ?: "<unknown>"
      val noCache = matchResult.groupValues[2].contains("nocache")
      Test(src, args, useKotlin, id + " (${matchResult.groupValues[1]})", noCache)
    }.filter { !ProjectFixture.useConfigurationCache || !it.noCache }

  }

  lateinit var fixture: ProjectFixture

  Given("A fragment from the Readme") {

    withData(nameFn = { it.id }, tests()) { (src, args, useKotlin, _) ->
      withData(nameFn = { it }, args.split(" ")) {


        fixture = ProjectFixture(tempdir(), useKotlin, addClassPath = true)
        fixture.project.copy {
          from(fixture.testDataDir["readme"].absolutePath)
          into(fixture.project.rootDir)
        }

        fixture.buildFile(src)
        fixture.build(it)
        fixture.assertBuildSuccess()
        fixture.build(it)
        fixture.assertConfigurationCacheUsed()
        fixture.assertBuildUpToDate()

      }

    }

  }

})
