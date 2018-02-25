package com.github.blueboxware.gdxplugin

import com.badlogic.gdx.Version
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.github.blueboxware.gdxplugin.tasks.DistanceField
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import org.gradle.api.*
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.GradleVersion

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
@Suppress("unused")
class GdxPlugin: Plugin<Project> {

  override fun apply(project: Project) {

    if (GradleVersion.current() < GradleVersion.version("3.0")) {
      throw GradleException("The com.github.blueboxware.gdx plugin requires Gradle version 3.0 or higher")
    }

    project.tasks.create(ALL_PACKS_TASK_NAME).apply {
      dependsOn(closure { _: Task ->
        project.tasks.filter { it is PackTextures }.toTypedArray()
      })
      description = "Create or update all texture packs"
    }

    project.tasks.create(ALL_DF_FIELDS_TASK_NAME).apply {
      dependsOn(closure { _: Task ->
        project.tasks.filter { it is DistanceField }.toTypedArray()
      })
      description = "Create or update all distance fields"
    }

    val packTexturesTask = project.tasks.create("packTextures", PackTextures::class.java)
    packTexturesTask.packFileName = "pack.atlas"
    project.afterEvaluate {
      project.tasks.findByName(LifecycleBasePlugin.BUILD_TASK_NAME)?.dependsOn(packTexturesTask)
    }

    val packTexturesTasksContainer = project.container(PackTextures::class.java) {
      val name = "pack" + it.capitalize() + "Textures"
      val task = project.tasks.create(name, PackTextures::class.java).apply {
        description = "Pack $it textures using LibGDX's TexturePacker"
        packFileName = it
      }
      project.tasks.findByName(LifecycleBasePlugin.BUILD_TASK_NAME)?.dependsOn(task)
      task
    }
    project.extensions.add("texturePacks", packTexturesTasksContainer)

    val distanceFieldContainer = project.container(DistanceField::class.java) {
      val name = "generate" + it.capitalize() + "DistanceField"
      val task = project.tasks.create(name, DistanceField::class.java).apply {
        description = "Generate $it distance field using LibGDX's DistanceFieldGenerator"
      }
      project.tasks.findByName(LifecycleBasePlugin.BUILD_TASK_NAME)?.dependsOn(task)
      task
    }
    project.extensions.add("distanceFields", distanceFieldContainer)

    val gdxVersionTask = project.tasks.create("gdxVersion", DefaultTask::class.java)
    with(gdxVersionTask) {
      description = "Show the GDX version used by gdxPlugin"
      group = "help"
      doFirst {
        // Don't inline Version.VERSION
        val usedVersion = Version::class.java.getField("VERSION").get(null) as? String ?: "<unknown>"
        val defaultVersion = Version.VERSION
        if (usedVersion == defaultVersion) {
          println(usedVersion)
        } else {
          println("$usedVersion (default: $defaultVersion)")
        }
      }
    }

    val gdxTexturePackerSettingsHelp = project.tasks.create("texturePackerSettingsHelp", DefaultTask::class.java)
    with(gdxTexturePackerSettingsHelp) {
      description = "Show the available TexturePacker settings and their defaults"
      group = "help"
      doFirst {
        TexturePacker.Settings().let { defaultSettings ->
          println("TexturePacker settings and their defaults:")
          defaultSettings.javaClass.fields.forEach {field ->
            println("\t" + field.name + ": " + prettyPrint(field.get(defaultSettings)))
          }
        }
      }
    }
  }

  companion object {
    val LOGGER: Logger = Logging.getLogger(GdxPlugin::class.java)

    const val ALL_PACKS_TASK_NAME = "createAllTextPacks"
    const val ALL_DF_FIELDS_TASK_NAME = "createAllDistanceFields"
  }

}