package com.github.blueboxware.gdxplugin

import com.badlogic.gdx.Version
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.github.blueboxware.BuildConfig
import com.github.blueboxware.gdxplugin.dsl.BitmapFontConfiguration
import com.github.blueboxware.gdxplugin.dsl.DistanceFieldConfiguration
import com.github.blueboxware.gdxplugin.dsl.NinePatchConfiguration
import com.github.blueboxware.gdxplugin.dsl.PackTexturesConfiguration
import com.github.blueboxware.gdxplugin.tasks.*
import org.gradle.api.*
import org.gradle.api.internal.initialization.ScriptHandlerInternal
import org.gradle.kotlin.dsl.container
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.GradleVersion
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
abstract class GdxPlugin : Plugin<Project> {

  @Inject
  abstract fun getBuildScript(): ScriptHandlerInternal

  override fun apply(project: Project) {

    if (GradleVersion.current() < GradleVersion.version("8.5")) {
      throw GradleException("The com.github.blueboxware.gdx plugin requires Gradle version 8.5 or higher")
    }

    val objects = project.objects

    val allPacksTask = project.tasks.register(ALL_PACKS_TASK_NAME) {
      dependsOn(closure { _: Task ->
        project.tasks.filterIsInstance<PackTextures>().toTypedArray()
      })
      description = "Create or update all texture packs"
      group = TASK_GROUP
    }

    val allDistanceFieldsTask = project.tasks.register(ALL_DF_FIELDS_TASK_NAME) {
      dependsOn(closure { _: Task ->
        project.tasks.filterIsInstance<DistanceField>().toTypedArray()
      })
      description = "Create or update all distance fields"
      group = TASK_GROUP
    }

    val allFontsTask = project.tasks.register(ALL_BM_FONTS_TASK_NAME) {
      dependsOn(closure { _: Task ->
        project.tasks.filterIsInstance<BitmapFont>().toTypedArray()
      })
      description = "Create or update all bitmap fonts"
      group = TASK_GROUP
    }

    val allNinePatchesTask = project.tasks.register(ALL_NINE_PATCHES_TASK_NAME) {
      dependsOn(closure { _: Task ->
        project.tasks.filterIsInstance<NinePatch>().toTypedArray()
      })
      description = "Create or update all nine patches"
      group = TASK_GROUP
    }

    val allAssetsTask = project.tasks.register(ALL_ASSETS_TASK_NAME) {
      dependsOn(allDistanceFieldsTask, allFontsTask, allPacksTask, allNinePatchesTask)
      description = "Create or update all assets (fonts, distance fields and texture packs)"
      group = TASK_GROUP
    }
    project.tasks.findByName(LifecycleBasePlugin.BUILD_TASK_NAME)?.dependsOn(allAssetsTask)
    val classPath = getBuildScript().configurations.getByName("classpath")

    val bitmapFontsContainer = project.container<BitmapFontConfiguration> { name ->
      objects.newInstance<BitmapFontConfiguration>(name)
    }
    project.extensions.add("bitmapFonts", bitmapFontsContainer)

    bitmapFontsContainer.configureEach {
      val taskName = "generate" + name.capitalize() + "Font"
      val config = this
      project.tasks.register<BitmapFont>(taskName) {
        description = "Generate $name bitmap font"
        configuration.set(config)
        this.classPath.set(classPath)
      }
    }

    val ninePatchesContainer = project.container<NinePatchConfiguration> { name ->
      objects.newInstance<NinePatchConfiguration>(name)
    }
    project.extensions.add("ninePatch", ninePatchesContainer)

    ninePatchesContainer.configureEach {
      val taskName = "generate" + name.capitalize() + "NinePatch"
      val config = this
      project.tasks.register<NinePatch>(taskName) {
        description = "Generate $name nine patch"
        configuration.set(config)
      }
    }

    val distanceFieldContainer = project.container<DistanceFieldConfiguration> { name ->
      objects.newInstance<DistanceFieldConfiguration>(name)
    }
    project.extensions.add("distanceFields", distanceFieldContainer)

    distanceFieldContainer.configureEach {
      val taskName = "generate" + name.capitalize() + "DistanceField"
      val config = this
      project.tasks.register<DistanceField>(taskName) {
        description = "Generate $name distance field using libGDX's DistanceFieldGenerator"
        configuration.set(config)
      }

    }

    val packTexturesContainer = project.container<PackTexturesConfiguration> { name ->
      objects.newInstance<PackTexturesConfiguration>(name, project.copySpec())
    }
    project.extensions.add("texturePacks", packTexturesContainer)

    packTexturesContainer.configureEach {
      val config = this
      val taskName = "pack" + name.capitalize() + "Textures"
      project.tasks.register<PackTextures>(taskName) {
        description = "Pack $name textures using libGDX's TexturePacker"
        configuration.set(config)
      }
    }

    val packTexturesConfig = project.extensions.create("packTextures", PackTexturesConfiguration::class.java, "packTextures", project.copySpec())
    packTexturesConfig.packFileName.set("pack.atlas")
    project.tasks.register<PackTextures>("packTextures").configure {
      configuration.set(packTexturesConfig)
    }

    project.tasks.register<DefaultTask>("gdxVersion") {
      description = "Show the GDX version used by gdxPlugin"
      group = TASK_GROUP
      doFirst {
        // Don't inline Version.VERSION
        val usedVersion = Version.VERSION
        val defaultVersion = BuildConfig.GDX_VERSION
        if (usedVersion == defaultVersion) {
          println(usedVersion)
        } else {
          println("$usedVersion (default: $defaultVersion)")
        }
      }
    }

    project.tasks.register<DefaultTask>("texturePackerSettingsHelp") {
      description = "Show the available TexturePacker settings and their defaults"
      group = TASK_GROUP
      doFirst {
        TexturePacker.Settings().let { defaultSettings ->
          println("TexturePacker settings and their defaults:")
          defaultSettings.javaClass.fields.forEach { field ->
            println("\t" + field.name + ": " + prettyPrint(field.get(defaultSettings)))
          }
        }
      }
    }
  }

  companion object {
    // val LOGGER: Logger = Logging.getLogger(GdxPlugin::class.java)

    const val ALL_PACKS_TASK_NAME = "createAllTexturePacks"
    const val ALL_DF_FIELDS_TASK_NAME = "createAllDistanceFields"
    const val ALL_BM_FONTS_TASK_NAME = "createAllFonts"
    const val ALL_NINE_PATCHES_TASK_NAME = "createAllNinePatches"
    const val ALL_ASSETS_TASK_NAME = "createAllAssets"

    const val TASK_GROUP = "libGDX"
  }

}
