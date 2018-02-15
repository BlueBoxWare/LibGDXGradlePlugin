
import org.gradle.internal.impldep.junit.framework.Assert.assertTrue
import org.gradle.util.GradleVersion
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

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
internal object TestDistanceFieldTask: Spek({

  lateinit var fixture: ProjectFixture

  beforeEachTest {
    fixture = ProjectFixture(false)
  }

  afterEachTest {
    fixture.destroy()
  }

  given("a minimal df task") {

    beforeEachTest {

      fixture.buildFile("""
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
          }
        }
      """)
      fixture.addFile("images1/empty.png")

    }

    on("listing tasks") {

      fixture.build("tasks")

      it("should contain the declared task") {
        fixture.assertBuildOutputContains("generateFooDistanceField")
      }

    }

    on("building") {

      fixture.build("generateFooDistanceField")

      it("should create the expected image") {
        fixture.assertFileEqualsBinary(fixture.expected["df_white.png"], fixture.input["images1/empty-df.png"])
      }

    }

    on("building twice") {

      fixture.build("generateFooDistanceField")
      fixture.build("generateFooDistanceField")

      it("should be up-to-date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    on("changing the color after a build") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile("""
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
            color = "ff0000"
          }
        }
      """)
      fixture.build("generateFooDistanceField")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

      it("should create the expected image") {
        fixture.assertFileEqualsBinary(fixture.expected["df_red.png"], fixture.input["images1/empty-df.png"])
      }

    }

    on("changing the output file argument after a build") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile("""
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
            outputFile = file('out/foo/df.png')
          }
        }
      """)
      fixture.build("generateFooDistanceField")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

      it("should create image at the expected location") {
        fixture.assertFileEqualsBinary("df_white.png", "foo/df.png")
      }

    }

    on("changing the format after a build") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile("""
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
            outputFormat = '.gif'
          }
        }
      """)
      fixture.build("generateFooDistanceField")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

      it("should use the correct output filename") {
        assertTrue(fixture.input["images1/empty-df.gif"].exists())
      }

    }

    on("removing the output file after a build") {

      fixture.build("generateFooDistanceField")
      fixture.input["images1/empty-df.png"].delete()
      fixture.build("generateFooDistanceField")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a df task with all arguments") {

    beforeEachTest {

      fixture.buildFile("""
        distanceFields {
          'foo' {
            inputFile = file('in/etc/wat.jpg')
            downscale = 8
            spread = 12
            color = 'ff00ff'
            outputFormat = ".png"
          }
        }
      """)
      fixture.addFile("etc/wat.jpg")

    }

    on("build") {

      fixture.build("generateFooDistanceField")

      it("should build") {
        fixture.assertBuildSuccess()
      }

      it("should create a correct result") {
        fixture.assertFileEqualsBinary(fixture.expected["wat-df.png"], fixture.input["etc/wat-df.png"])
      }

    }

    on("changing the spread after building") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile(fixture.getBuildFile().replace("12", "24"))
      fixture.build("generateFooDistanceField")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a df task with a jpg output extension") {

    beforeEachTest {

      fixture.buildFile("""
        distanceFields {
          'foo' {
            inputFile = file('in/etc/wat.jpg')
            outputFile = file('out/df.jpg')
            spread = 16
            downscale = 8
          }
        }
      """)
      fixture.addFile("etc/wat.jpg")

    }

    on("building") {

      fixture.build("generateFooDistanceField")

      it("should generate a jpg") {
        fixture.assertFileEqualsBinary("df_wat.jpg", "df.jpg")
      }

    }

  }

  given("a df task with a specified format") {

    beforeEachTest {

      fixture.buildFile("""
        distanceFields {
          'foo' {
            inputFile = file('in/images1/empty.png')
            outputFormat = 'gif'
          }
        }
      """)
      fixture.addFile("images1/empty.png")

    }

    on("removing the output file after a build") {

      fixture.build("generateFooDistanceField")
      fixture.input["images1/empty-df.gif"].delete()
      fixture.build("generateFooDistanceField")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

    on("changing the output format after a build") {

      if (fixture.gradleVersion < GradleVersion.version("3.4")) {
        // https://github.com/gradle/gradle/issues/1079
        Thread.sleep(5000)
      }

      fixture.build("generateFooDistanceField")
      fixture.buildFile(fixture.getBuildFile().replace("gif", "jpg"))
      fixture.build("generateFooDistanceField")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

})