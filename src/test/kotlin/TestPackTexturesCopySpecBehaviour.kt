import com.github.blueboxware.gdxplugin.tasks.PackTextures
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.engine.spec.tempdir

internal object TestPackTexturesCopySpecBehaviour : BehaviorSpec({

  lateinit var fixture: ProjectFixture

  val buildFile = """
    plugins {
        id("com.github.blueboxware.gdx") version "${ProjectFixture.getVersion()}"
    }

    val c = copySpec {
      from("in/etc") {
        exclude("*.jpg")
        isCaseSensitive = true
      }

    }

    packTextures {
      with(c)
      into("out")
      from("in/images1") {
        from(file("in/images2")) {
          include("b*")
          includeEmptyDirs = false
        }
        from("in/images2") {
          from("in/images1") {
            exclude("sub")
            include("**/*")
            // rename("deb.png", "deb2.png")
          }
          from("in/images1/sub/subsub") {
            include("*")
          }
        }
        include("_")
      }
    }
      """

  beforeContainer {
    fixture = ProjectFixture(tempdir(), useKotlin = true, addClassPath = true)
    fixture.copyFiles {
      from(fixture.testDataDir.absolutePath)
    }
  }

  Given("an advanced CopySpec") {

    beforeContainer {
      fixture.buildFile(buildFile)
    }

    When("building") {

      fixture.build("packTextures")

      Then("should successfully build") {
        fixture.assertBuildSuccess()
      }

      Then("should create a correct .atlas") {
        fixture.assertFileEquals("packTexturesSpec/pack1.atlas", "pack.atlas")
      }

    }

    When("building twice") {

      fixture.build("packTextures")
      fixture.build("packTextures")

      Then("should use configuration cache") {
        fixture.assertConfigurationCacheUsed()
      }

      Then("should be up to date") {
        fixture.assertBuildUpToDate()
      }

    }

    When("building twice and changing the include of a nested spec") {

      fixture.build("packTextures")
      fixture.buildFileReplace("""include("*")""", "")
      fixture.build("packTextures")

      Then("it should build again") {
        fixture.assertBuildSuccess()
      }

      Then("should create a correct .atlas") {
        fixture.assertFileEquals("packTexturesSpec/pack2.atlas", "pack.atlas")
      }

    }

    When("building twice and changing the caseSensitivity of a reused spec") {

      fixture.build("packTextures")
      fixture.buildFileReplace("true", "false")
      fixture.build("packTextures")

      Then("it should build again") {
        fixture.assertBuildSuccess()
      }

    }

    When("building twice and changing the includeEmptyDirs of a nested spec") {

      fixture.build("packTextures")
      fixture.buildFileReplace("false", "true")
      fixture.build("packTextures")

      Then("it should build again") {
        fixture.assertBuildSuccess()
      }

    }

    When("building twice and deleting an input file") {

      fixture.build("packTextures")
      fixture.input["images1/empty.png"].delete()
      fixture.build("packTextures")

      Then("it should build again") {
        fixture.assertBuildSuccess()
      }

    }

    When("having a custom action with config cache disabled") {

      if (ProjectFixture.useConfigurationCache) {
        return@When
      }

      fixture.buildFileReplace("//", "")
      fixture.build("packTextures")
      fixture.build("packTextures")

      Then("it should be up-to-date") {
        fixture.assertBuildUpToDate()
      }

      Then("should create a correct .atlas") {
        fixture.assertFileEquals("packTexturesSpec/pack3.atlas", "pack.atlas")
      }

    }

    When("having a custom action with config cache enabled") {

      if (!ProjectFixture.useConfigurationCache) {
        return@When
      }

      fixture.buildFileReplace("//", "")
      fixture.build("packTextures", shouldFail = true)

      Then("it should fail") {
        fixture.assertBuildFailure(PackTextures.CONFIG_CACHE_ERROR_MSG)
      }

    }

    When("building twice and adding a custom action with config cache disabled") {

      if (ProjectFixture.useConfigurationCache) {
        return@When
      }

      fixture.build("packTextures")
      fixture.buildFileReplace("//", "")
      fixture.build("packTextures")

      Then("it should build again") {
        fixture.assertBuildSuccess()
      }

      Then("should create a correct .atlas") {
        fixture.assertFileEquals("packTexturesSpec/pack3.atlas", "pack.atlas")
      }

    }

    When("building twice and adding a custom action with config cache enabled") {

      if (!ProjectFixture.useConfigurationCache) {
        return@When
      }

      fixture.build("packTextures")
      fixture.buildFileReplace("//", "")
      fixture.build("packTextures", shouldFail = true)

      Then("it should fail") {
        fixture.assertBuildFailure(PackTextures.CONFIG_CACHE_ERROR_MSG)
      }

    }

  }

})