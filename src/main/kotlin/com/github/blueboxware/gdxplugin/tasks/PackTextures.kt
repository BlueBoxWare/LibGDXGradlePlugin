package com.github.blueboxware.gdxplugin.tasks

import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.utils.Json
import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.configure
import com.github.blueboxware.gdxplugin.createSolidColorImage
import com.github.blueboxware.gdxplugin.dsl.PackTexturesConfiguration
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.lambdas.SerializableLambdas
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileReader
import javax.inject.Inject


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
@CacheableTask
abstract class PackTextures @Inject constructor(
  private val projectLayout: ProjectLayout,
  private val fileSystemOperations: FileSystemOperations,
  private val providerFactory: ProviderFactory,
  private val buildFeatures: BuildFeatures
) : DefaultTask() {

  @get:Nested
  abstract val configuration: Property<PackTexturesConfiguration>

  @OutputFiles
  fun getOutputFiles(): Provider<FileCollection?> = configuration.flatMap { configuration ->
    configuration.settings.map { settings ->
      val baseName = configuration.packFileName.get().removeSuffix(settings.atlasExtension)
      val destinationDir = configuration.destinationDir?.absolutePath ?: return@map projectLayout.files()
      projectLayout.files(
        settings.scale.mapIndexed { index, _ ->
          File(destinationDir, settings.getScaledPackFileName(baseName, index) + settings.atlasExtension)
        })
    }
  }

  init {
    description = "Pack textures using libGDX's TexturePacker"
    group = GdxPlugin.TASK_GROUP

    logging.captureStandardOutput(LogLevel.LIFECYCLE)
    logging.captureStandardError(LogLevel.ERROR)

    TexturePacker.Settings::class.java.fields.filter { it.name !in SETTINGS_TO_IGNORE }.associate { field ->
      field.name to providerFactory.provider<Any> {
        field.get(configuration.get().settings.get())
      }
    }.let {
      inputs.properties(it)
    }

    outputs.doNotCacheIf("Has custom actions",
      SerializableLambdas.spec { configuration.get().hasCustomActions.get() }  )
  }

  @TaskAction
  fun execute() {

    val configuration = configuration.get()

    if (configuration.hasCustomActions.get() && buildFeatures.configurationCache.active.get()) {
      throw GradleException(CONFIG_CACHE_ERROR_MSG)
    }

    var settings = configuration.settings.get()
    val solidSpecs = configuration.solidSpecs.get()

    configuration.destinationDir?.let { destinationDir ->

      temporaryDir.deleteRecursively()

      val relativeTmpDir = projectLayout.projectDirectory.asFile.toPath().relativize(temporaryDir.toPath())
      val copyDidWork = fileSystemOperations.copy {
        into(projectLayout.projectDirectory)
        if (!configuration.usePackJson.get()) {
          exclude("**/pack.json")
        }
        with(configuration.apply { into(relativeTmpDir.toString()) })
      }

      createSolidTextures(temporaryDir)

      configuration.settingsFile.asFile.orNull?.let {
        settings = Json().fromJson(TexturePacker.Settings::class.java, FileReader(it))
      }

      val outputFileName = configuration.packFileName.get() + (settings.atlasExtension ?: ".atlas")
      val outputDir = projectLayout.projectDirectory.file(destinationDir.path)
      TexturePacker.process(settings, temporaryDir.absolutePath, outputDir.asFile.absolutePath, outputFileName)

      didWork = copyDidWork.didWork || solidSpecs.isNotEmpty()

    } ?: throw GradleException("Missing 'into' parameter in PackTextures configuration.")

  }

  private fun createSolidTextures(targetDir: File) {

    configuration.get().solidSpecs.get().forEach { solidSpec ->

      if (solidSpec.name == null) {
        throw GradleException("No name specified for solid color texture specification ($solidSpec)")
      }

      val outputFile = File(targetDir, solidSpec.name + ".png")

      createSolidColorImage(outputFile, solidSpec.color, solidSpec.width, solidSpec.height)
    }

  }

  companion object {

    private val SETTINGS_TO_IGNORE = listOf("fast", "silent", "limitMemory", "ignore")

    internal const val CONFIG_CACHE_ERROR_MSG = "Custom actions like rename() and filter() are not supported for the PackTextures task when the configuration cache is enabled. Create a custom task instead and disable the configuration cache for that task with Task.notCompatibleWithConfigurationCache()."

    @JvmStatic
    @JvmOverloads
    @Deprecated("Use packSettings() from utils.Utils")
    fun createSettings(
      baseSettings: TexturePacker.Settings? = null, closure: Closure<in TexturePacker.Settings>
    ): TexturePacker.Settings {
      val settings = TexturePacker.Settings()
      baseSettings?.let { settings.set(it) }
      settings.configure(closure)
      return settings
    }

    @JvmStatic
    @JvmOverloads
    @Deprecated("Use packSettings() from utils.Utils")
    fun createSettings(
      baseSettings: TexturePacker.Settings? = null, closure: TexturePacker.Settings.() -> Unit
    ): TexturePacker.Settings {
      val settings = TexturePacker.Settings()
      baseSettings?.let { settings.set(it) }
      settings.apply(closure)
      return settings
    }

  }

}
