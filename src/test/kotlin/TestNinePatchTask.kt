
import com.github.blueboxware.gdxplugin.GdxPlugin
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
internal object TestNinePatchTask: Spek({

  lateinit var fixture: ProjectFixture

  beforeEachTest {
    fixture = ProjectFixture()
    fixture.copyFiles {
      from(fixture.testDataDir.absolutePath) {
        include("ninePatch/")
      }
    }
  }

  afterEachTest {
//    fixture.destroy()
  }

  given("a ninepatch with only defaults") {

    beforeEachTest {
      fixture.buildFile("""
        ninePatch {
          np {
            image = file('in/ninePatch/red.png')
          }
        }
      """)
    }

    on("building") {

      fixture.build("generateNpNinePatch")

      it("should create the correct ninepatch") {
        fixture.assertBuildSuccess()
        fixture.assertNinePatchEquals(
                listOf(0, 0, 0, 0),
                null,
                fixture.input["ninePatch/red.png"],
                fixture.input["ninePatch/red.9.png"]
        )

      }

    }

    on("building twice") {

      fixture.build("generateNpNinePatch")
      fixture.build("generateNpNinePatch")

      it("should be up to date the second time") {
        fixture.assertBuildUpToDate()
      }

    }

    on("building twice and adding a split") {

      fixture.build("generateNpNinePatch")
      fixture.buildFile("""
        ninePatch {
          np {
            image = file('in/ninePatch/red.png')
            top = 1
          }
        }
      """)
      fixture.build("generateNpNinePatch")

      it("should build again the second time") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a ninepatch with custom splits and default paddings") {

    beforeEachTest {
      fixture.buildFile("""
        ninePatch {
          np {
            image = file('in/ninePatch/red.jpg')
            output = file('out/red.9.png')
            top = 1
            bottom = 2
            left = 3
            right = 4
            paddingTop = 3
          }
        }
      """)
    }

    on("building") {

      fixture.build("generateNpNinePatch")

      it("should generate the correct ninepatch") {
        fixture.assertNinePatchEquals(
                listOf(3, 4, 1, 2),
                listOf(3, 4, 3, 2),
                fixture.input["ninePatch/red.jpg"],
                fixture.output["red.9.png"]
        )
      }

    }

    on("building twice and changing a split in between") {

      fixture.build("generateNpNinePatch")
      fixture.buildFile(fixture.getBuildFile().replace("4", "3"))
      fixture.build("generateNpNinePatch")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a ninepatch with custom paddings") {

    beforeEachTest {
      fixture.buildFile("""
        ninePatch {
          np {
            image = file('in/ninePatch/red.png')
            output = file('out/red.9.png')
            paddingTop = 3
            paddingBottom = 4
            paddingRight = 2
            paddingLeft = 5
          }
        }
      """)
    }

    on("building") {

      fixture.build("generateNpNinePatch")

      it("should generate the correct ninepatch") {
        fixture.assertNinePatchEquals(
                listOf(0, 0, 0, 0),
                listOf(5, 2, 3, 4),
                fixture.input["ninePatch/red.png"],
                fixture.output["red.9.png"]
        )
      }

    }

    on("building twice and changing the padding in between") {

      fixture.build("generateNpNinePatch")
      fixture.buildFile(fixture.getBuildFile().replace("3", "1"))
      fixture.build("generateNpNinePatch")

      it("should build again") {
        fixture.assertBuildSuccess()
      }

    }

  }

  given("a few ninePatches with auto") {

    beforeEachTest {
      fixture.buildFile("""
        ninePatch {
          np1 {
            image = file('in/ninePatch/overlap.png')
            auto = true
          }
          np2 {
            image = file('in/ninePatch/thingy.png')
            auto = true
          }
          np3 {
            image = file('in/ninePatch/resizable.png')
            auto = true
          }
          np4 {
            image = file('in/ninePatch/rect.png')
            auto = true
          }
        }
      """)
    }

    on("building") {

      fixture.build(GdxPlugin.ALL_NINE_PATCHES_TASK_NAME)

      it("should create the correct nine patches") {
        fixture.assertNinePatchEquals(
                listOf(40, 40, 40, 40),
                null,
                fixture.input["ninePatch/overlap.png"],
                fixture.input["ninePatch/overlap.9.png"]
        )
        fixture.assertNinePatchEquals(
                listOf(56, 56, 56, 56),
                null,
                fixture.input["ninePatch/thingy.png"],
                fixture.input["ninePatch/thingy.9.png"]
        )
        fixture.assertNinePatchEquals(
                listOf(12, 13, 1, 19),
                null,
                fixture.input["ninePatch/resizable.png"],
                fixture.input["ninePatch/resizable.9.png"]
        )
        fixture.assertNinePatchEquals(
                listOf(8, 8, 8, 8),
                null,
                fixture.input["ninePatch/rect.png"],
                fixture.input["ninePatch/rect.9.png"]
        )
      }

    }

  }

  given("a ninepatch with auto and custom center") {

    beforeEachTest {
      fixture.buildFile("""
        ninePatch {
          np {
            image = file('in/ninePatch/resizable.png')
            auto = true
            centerX = 5
            centerY = 76
            paddingLeft = 2
          }
        }
      """)
    }

    on("building") {

      fixture.build(GdxPlugin.ALL_NINE_PATCHES_TASK_NAME)

      it("should create the correct nine patch") {
        fixture.assertNinePatchEquals(
                listOf(1, 19, 75, 7),
                listOf(2, 19, 75, 7),
                fixture.input["ninePatch/resizable.png"],
                fixture.input["ninePatch/resizable.9.png"]
        )
      }

    }

  }

  given("a few nine patches with auto and fuzziness") {

    beforeEachTest {
      fixture.buildFile("""
        ninePatch {
          np1 {
            image = file('in/ninePatch/document.png')
            auto = true
            fuzziness = 4
          }
          np2 {
            image = file('in/ninePatch/gradient.jpg')
            auto = true
            fuzziness = 25
          }
        }
      """)
    }

    on("building") {

      fixture.build(GdxPlugin.ALL_NINE_PATCHES_TASK_NAME)

      it("should create the correct nine patches") {
        fixture.assertNinePatchEquals(
                listOf(5, 5, 3, 3),
                null,
                fixture.input["ninePatch/document.png"],
                fixture.input["ninePatch/document.9.png"]
        )
        fixture.assertNinePatchEquals(
                listOf(123, 89, 108, 56),
                null,
                fixture.input["ninePatch/gradient.jpg"],
                fixture.input["ninePatch/gradient.9.png"]
        )
      }

    }

  }

  given("a ninepatch with auto, fuzziness and custom center") {

    beforeEachTest {
      fixture.buildFile("""
        ninePatch {
          np {
            image = file('in/ninePatch/resizable.png')
            auto = true
            centerX = 5
            centerY = 76
            paddingLeft = 2
            fuzziness = 40
          }
        }
      """)
    }

    on("building") {

      fixture.build(GdxPlugin.ALL_NINE_PATCHES_TASK_NAME)

      it("should create the correct nine patch") {
        fixture.assertNinePatchEquals(
                listOf(1, 1, 1, 1),
                listOf(2, 1, 1, 1),
                fixture.input["ninePatch/resizable.png"],
                fixture.input["ninePatch/resizable.9.png"]
        )
      }

    }

  }

  given("a ninepatch with auto and custom left and top") {

    beforeEachTest {
      fixture.buildFile("""
        ninePatch {
          np {
            image = file('in/ninePatch/resizable.png')
            auto = true
            left = 2
            top = 2
          }
        }
      """)
    }

    on("building") {

      fixture.build(GdxPlugin.ALL_NINE_PATCHES_TASK_NAME)

      it("should create the correct nine patch") {
        fixture.assertNinePatchEquals(
                listOf(2, 19, 2, 19),
                null,
                fixture.input["ninePatch/resizable.png"],
                fixture.input["ninePatch/resizable.9.png"]
        )
      }

    }

  }

})