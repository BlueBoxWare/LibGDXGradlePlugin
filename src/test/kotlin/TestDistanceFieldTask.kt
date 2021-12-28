import com.github.blueboxware.gdxplugin.GdxPlugin
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import org.gradle.internal.impldep.junit.framework.TestCase
import org.gradle.util.GradleVersion


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
@OptIn(ExperimentalKotest::class)
@Suppress("unused")
internal object TestDistanceFieldTask: BehaviorSpec({

  lateinit var fixture: ProjectFixture

  beforeContainer {
    fixture = ProjectFixture(tempdir())
  }

  given("a minimal df task") {

    beforeContainer {

      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
          }
        }
      """
      )
      fixture.addFile("images1/empty.png")

    }

    `when`("listing tasks") {

      fixture.build("tasks")

      then("should contain the declared task") {
        fixture.assertBuildOutputContains("generateFooDistanceField")
      }

    }

    `when`("building") {

      fixture.build("generateFooDistanceField")

      then("should create the expected image") {
        fixture.assertFileEqualsBinary(
          fixture.expected["distanceField/df_white.png"],
          fixture.input["images1/empty-df.png"]
        )
      }

    }

    `when`("building twice") {

      fixture.build("generateFooDistanceField")
      fixture.build("generateFooDistanceField")

      then("should be up-to-date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    `when`("changing the color after a build") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
            color = "ff0000"
          }
        }
      """
      )
      fixture.build("generateFooDistanceField")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

      then("should create the expected image") {
        fixture.assertFileEqualsBinary(
          fixture.expected["distanceField/df_red.png"],
          fixture.input["images1/empty-df.png"]
        )
      }

    }

    `when`("changing the output file argument after a build") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
            outputFile = file('out/foo/df.png')
          }
        }
      """
      )
      fixture.build("generateFooDistanceField")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

      then("should create image at the expected location") {
        fixture.assertFileEqualsBinary("distanceField/df_white.png", "foo/df.png")
      }

    }

    `when`("changing the format after a build") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
            outputFormat = '.gif'
          }
        }
      """
      )
      fixture.build("generateFooDistanceField")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

      then("should use the correct output filename") {
        TestCase.assertTrue(fixture.input["images1/empty-df.gif"].exists())
      }

    }

    `when`("removing the output file after a build") {

      fixture.build("generateFooDistanceField")
      fixture.input["images1/empty-df.png"].delete()
      fixture.build("generateFooDistanceField")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    `when`("running ${GdxPlugin.ALL_DF_FIELDS_TASK_NAME}") {

      fixture.build(GdxPlugin.ALL_DF_FIELDS_TASK_NAME)

      then("should run the df task") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a df task with all arguments") {

    beforeContainer {

      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/etc/wat.jpg')
            downscale = 8
            spread = 12
            color = 'ff00ff'
            outputFormat = ".png"
          }
        }
      """
      )
      fixture.addFile("etc/wat.jpg")

    }

    `when`("build") {

      fixture.build("generateFooDistanceField")

      then("should build") {
        fixture.assertBuildSuccess()
      }

      then("should create a correct result") {
        fixture.assertFileEqualsBinary(fixture.expected["distanceField/wat-df.png"], fixture.input["etc/wat-df.png"])
      }

    }

    `when`("changing the spread after building") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile(fixture.getBuildFile().replace("12", "24"))
      fixture.build("generateFooDistanceField")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a df task with a jpg output extension") {

    beforeContainer {

      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/etc/wat.jpg')
            outputFile = file('out/df.jpg')
            spread = 16
            downscale = 8
          }
        }
      """
      )
      fixture.addFile("etc/wat.jpg")

    }

    `when`("building") {

      val isOpenJDK = System.getProperty("java.vm.name").contains("OpenJDK")

      fixture.build("generateFooDistanceField", shouldFail = isOpenJDK)

      then("should fail on OpenJDK, create the correct image otherwise") {
        if (isOpenJDK) {
          fixture.assertBuildFailure()
          fixture.assertBuildOutputContains(
            "does not have a writer for image type",
            "does not support creating jpegs with alpha"
          )
        } else {
          fixture.assertFileEqualsBinary("distanceField/df_wat.jpg", "df.jpg")
        }
      }

    }

  }

  given("a df task with a specified format") {

    beforeContainer {

      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
            outputFormat = 'gif'
          }
        }
      """
      )
      fixture.addFile("images1/empty.png")

    }

    `when`("removing the output file after a build") {

      fixture.build("generateFooDistanceField")
      fixture.input["images1/empty-df.gif"].delete()
      fixture.build("generateFooDistanceField")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    `when`("changing the output format after a build") {

      if (GradleVersion.version(fixture.gradleVersion) < GradleVersion.version("3.4")) {
        // https://github.com/gradle/gradle/issues/1079
        Thread.sleep(5000)
      }

      fixture.build("generateFooDistanceField")
      fixture.buildFile(fixture.getBuildFile().replace("gif", "png"))
      fixture.build("generateFooDistanceField")

      then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }


})
