import com.github.blueboxware.gdxplugin.GdxPlugin
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir
import org.gradle.internal.impldep.junit.framework.TestCase


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
internal object TestDistanceFieldTask: BehaviorSpec({

  lateinit var fixture: ProjectFixture

  beforeContainer {
    fixture = ProjectFixture(tempdir())
  }

  Given("a minimal df task") {

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

    When("listing tasks") {

      fixture.build("tasks")

      Then("should contain the declared task") {
        fixture.assertBuildOutputContains("generateFooDistanceField")
      }

    }

    When("building") {

      fixture.build("generateFooDistanceField")

      Then("should create the expected image") {
        fixture.assertFileEqualsBinary(
          fixture.expected["distanceField/df_white.png"],
          fixture.input["images1/empty-df.png"]
        )
      }

    }

    When("building twice") {

      fixture.build("generateFooDistanceField")
      fixture.build("generateFooDistanceField")

      Then("should use configuration cache") {
        fixture.assertConfigurationCacheUsed()
      }

      Then("should be up-to-date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    When("changing the color after a build") {

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

      Then("should build again") {
        fixture.assertBuildSuccess()
      }

      Then("should create the expected image") {
        fixture.assertFileEqualsBinary(
          fixture.expected["distanceField/df_red.png"],
          fixture.input["images1/empty-df.png"]
        )
      }

    }

    When("changing the output file argument after a build") {

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

      Then("should use the cache") {
        fixture.assertBuildFromCache()
      }

      Then("should create image at the expected location") {
        fixture.assertFileEqualsBinary("distanceField/df_white.png", "foo/df.png")
      }

    }

    When("changing the format after a build") {

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

      Then("should build again") {
        fixture.assertBuildSuccess()
      }

      Then("should use the correct output filename") {
        TestCase.assertTrue(fixture.input["images1/empty-df.gif"].exists())
      }

    }

    When("removing the output file after a build") {

      fixture.build("generateFooDistanceField")
      fixture.input["images1/empty-df.png"].delete()
      fixture.build("generateFooDistanceField")

      Then("should use the cache") {
        fixture.assertBuildFromCache()
      }

    }

    When("running ${GdxPlugin.ALL_DF_FIELDS_TASK_NAME}") {

      fixture.build(GdxPlugin.ALL_DF_FIELDS_TASK_NAME)

      Then("should run the df task") {
        fixture.assertBuildSuccess()
      }

    }

  }

  Given("a df task with all arguments") {

    beforeContainer {

      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/etc/wat.jpg')
            downscale = 8
            spread = 12f
            color = 'ff00ff'
            outputFormat = ".png"
          }
        }
      """
      )
      fixture.addFile("etc/wat.jpg")

    }

    When("build") {

      fixture.build("generateFooDistanceField")

      Then("should build") {
        fixture.assertBuildSuccess()
      }

      Then("should create a correct result") {
        fixture.assertFileEqualsBinary(fixture.expected["distanceField/wat-df.png"], fixture.input["etc/wat-df.png"])
      }

    }

    When("changing the spread after building") {

      fixture.build("generateFooDistanceField")
      fixture.buildFileReplace("12", "24")
      fixture.build("generateFooDistanceField")

      Then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  Given("a df task with a jpg output extension") {

    beforeContainer {

      fixture.buildFile(
        """
        distanceFields {
          'foo' {
            inputFile = file('in/etc/wat.jpg')
            outputFile = file('out/df.jpg')
            spread = 16f
            downscale = 8
          }
        }
      """
      )
      fixture.addFile("etc/wat.jpg")

    }

    When("building") {

      val isOpenJDK = System.getProperty("java.vm.name").contains("OpenJDK")

      fixture.build("generateFooDistanceField", shouldFail = isOpenJDK)

      Then("should fail on OpenJDK, create the correct image otherwise") {
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

  Given("a df task with a specified format") {

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

    When("removing the output file after a build") {

      fixture.build("generateFooDistanceField")
      fixture.input["images1/empty-df.gif"].delete()
      fixture.build("generateFooDistanceField")

      Then("should use configuration cache") {
        fixture.assertConfigurationCacheUsed()
      }

      Then("should use the cache") {
        fixture.assertBuildFromCache()
      }

    }

    When("changing the output format after a build") {

      fixture.build("generateFooDistanceField")
      fixture.buildFileReplace("gif", "png")
      fixture.build("generateFooDistanceField")

      Then("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

})
