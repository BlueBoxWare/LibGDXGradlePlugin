import org.gradle.internal.impldep.junit.framework.Assert.assertTrue
import org.gradle.internal.impldep.junit.framework.TestCase
import org.gradle.internal.impldep.org.junit.Assert.assertEquals
import org.gradle.testkit.runner.TaskOutcome
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

      val result = fixture.build("tasks")

      it("should contain the declared task") {
        TestCase.assertTrue(result.output, result.output.contains("generateFooDistanceField"))
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
      val secondResult = fixture.build("generateFooDistanceField")

      it("should be up-to-date the second time") {
        assertEquals(TaskOutcome.UP_TO_DATE, secondResult.task(":generateFooDistanceField")?.outcome)
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
      val secondResult = fixture.build("generateFooDistanceField")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":generateFooDistanceField")?.outcome)
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
      val secondResult = fixture.build("generateFooDistanceField")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":generateFooDistanceField")?.outcome)
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
      val secondResult = fixture.build("generateFooDistanceField")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":generateFooDistanceField")?.outcome)
      }

      it("should use the correct output filename") {
        assertTrue(fixture.input["images1/empty-df.gif"].exists())
      }

    }

    on("removing the output file after a build") {

      fixture.build("generateFooDistanceField")
      fixture.input["images1/empty-df.png"].delete()
      val secondResult = fixture.build("generateFooDistanceField")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":generateFooDistanceField")?.outcome)
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
      val secondResult = fixture.build("generateFooDistanceField")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":generateFooDistanceField")?.outcome)
      }

    }

    on("changing the output format after a build") {

      fixture.build("generateFooDistanceField")
      fixture.buildFile(fixture.getBuildFile().replace("gif", "jpg"))
      val secondResult = fixture.build("generateFooDistanceField")

      it("should build again") {
        assertEquals(TaskOutcome.SUCCESS, secondResult.task(":generateFooDistanceField")?.outcome)
      }

    }

  }

})