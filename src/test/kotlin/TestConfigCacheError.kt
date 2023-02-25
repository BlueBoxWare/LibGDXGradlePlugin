import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir

@EnabledIf(ConfigurationCache::class)
internal object TestConfigCacheError : BehaviorSpec({

    lateinit var fixture: ProjectFixture

    beforeContainer {
        fixture = ProjectFixture(tempdir(), addClassPath = true)
        fixture.addFile("etc/roboto.ttf")
    }

    given("a bitmap font task container") {

        beforeContainer {

            fixture.buildFile(
                """

        bitmapFonts {

            normal {

                inputFont = file('in/etc/roboto.ttf')

                outputFile = file('out/roboto.fnt')

            }

      

        }
      """
            )

        }

        `when`("building") {

            fixture.build("createAllFonts", shouldFail = true)

            then("should give an error") {

                fixture.assertBuildFailure(task = "generateNormalFont")
                fixture.assertBuildOutputContains(
                    "BitmapFont tasks are not compatible with Gradle's configuration cache."

                )

            }
        }
    }

})
