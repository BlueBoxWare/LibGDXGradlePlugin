package com.github.blueboxware.gdxplugin.tasks

import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.utils.Json
import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.closure
import com.github.blueboxware.gdxplugin.collectionToList
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.DestinationRootCopySpec
import org.gradle.api.internal.file.copy.FileCopyAction
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.*
import org.gradle.util.ConfigureUtil
import org.gradle.util.GFileUtils
import java.io.File
import java.io.FileReader


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
open class PackTextures: AbstractCopyTask() {

  var packFileName: String = name
    @Input @Optional get

  @Suppress("MemberVisibilityCanBePrivate")
  var settingsFile: File? = null
    @InputFile @Optional get

  @Suppress("MemberVisibilityCanBePrivate")
  var usePackJson: Boolean? = false
    @Input @Optional get

  @Suppress("MemberVisibilityCanBePrivate")
  var settings: TexturePacker.Settings = TexturePacker.Settings()
    @Nested @Optional get

  init {
    description = "Pack textures using LibGDX's TexturePacker"
    group = GdxPlugin.TASK_GROUP

    logging.captureStandardOutput(LogLevel.LIFECYCLE)
    logging.captureStandardError(LogLevel.ERROR)

    onlyIf {
      !inputs.files.isEmpty
    }

    TexturePacker.Settings::class.java.fields.filter { it.name !in SETTINGS_TO_IGNORE }.map { it.name to closure { ->
      it.get(settings).let { value ->
        // Gradle < 3.5 doesn't like collections as properties
        // https://github.com/gradle/gradle/commits/master/subprojects/core/src/main/java/org/gradle/api/internal/changedetection/state/InputPropertiesSerializer.java
        collectionToList(value)?.joinToString() ?: value
      }
    } }.toMap().let {
      inputs.properties(it)
    }

    outputs.files(closure { ->
      val baseName = packFileName.removeSuffix(settings.atlasExtension)
      settings.scale.mapIndexed { index, _ ->
        File(getDestinationDir(), settings.getScaledPackFileName(baseName, index) + settings.atlasExtension)
      }
    })

  }

  @Suppress("unused")
  fun settings(closure: Closure<in TexturePacker.Settings>) {
    ConfigureUtil.configure(closure, settings)
  }

  @Suppress("unused")
  fun settings(closure: TexturePacker.Settings.() -> Unit) {
    settings.apply(closure)
  }

  override fun createCopyAction(): CopyAction = CopyAction { stream ->

    getDestinationDir()?.let { destinationDir ->

      GFileUtils.deleteDirectory(temporaryDir)

      if (usePackJson != true) {
        exclude("**/pack.json")
      }

      val fileCopyAction = FileCopyAction(fileLookup.getFileResolver(temporaryDir))
      val copyDidWork = fileCopyAction.execute(stream)

      if (copyDidWork.didWork) {
        if (settingsFile != null) {
          settings = Json().fromJson(TexturePacker.Settings::class.java, FileReader(settingsFile))
        }
        val outputFileName = packFileName + (settings.atlasExtension ?: ".atlas")
        TexturePacker.process(settings, temporaryDir.absolutePath, destinationDir.absolutePath, outputFileName)
      }

      return@CopyAction copyDidWork
    } ?: throw GradleException("Missing 'into' parameter")

  }

  @Suppress("MemberVisibilityCanBePrivate")
  fun getDestinationDir(): File? {
    return rootSpec.destinationDir
  }

  override fun createRootSpec(): DestinationRootCopySpec {
    return instantiator.newInstance(DestinationRootCopySpec::class.java, fileResolver, super.createRootSpec())
  }

  override fun getRootSpec(): DestinationRootCopySpec =
          super.getRootSpec() as DestinationRootCopySpec

  companion object {

    private val SETTINGS_TO_IGNORE = listOf("fast", "silent", "limitMemory", "ignore")


    @JvmStatic
    @JvmOverloads
    @Suppress("unused")
    fun createSettings(baseSettings: TexturePacker.Settings? = null, closure: Closure<in TexturePacker.Settings>): TexturePacker.Settings {
      val settings = TexturePacker.Settings()
      baseSettings?.let { settings.set(it) }
      ConfigureUtil.configure(closure, settings)
      return settings
    }

    @JvmStatic
    @JvmOverloads
    @Suppress("unused")
    fun createSettings(baseSettings: TexturePacker.Settings? = null, closure: TexturePacker.Settings.() -> Unit): TexturePacker.Settings {
      val settings = TexturePacker.Settings()
      baseSettings?.let { settings.set(it) }
      settings.apply(closure)
      return settings
    }

  }

}