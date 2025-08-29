package com.github.blueboxware.gdxplugin.tasks

import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.dsl.BitmapFontConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import java.io.File
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
abstract class BitmapFont : DefaultTask() {

  @get:Nested
  abstract val configuration: Property<BitmapFontConfiguration>

  @get:Internal
  abstract val classPath: Property<FileCollection>

  @get:Inject
  abstract val execOperations: ExecOperations

  @get:Inject
  abstract val fileOperations: FileOperations

  @OutputFiles
  fun getActualOutputFiles() = fileOperations.immutableFiles(getActualOutputFontSpecs().map { it.file })

  @InputFile
  @Optional
  @PathSensitive(PathSensitivity.RELATIVE)
  fun getActualInputFile() = configuration.flatMap { configuration ->
    @Suppress("UnstableApiUsage")
    configuration.inputFont.filter { it is File }
  }

  @Input
  fun getInputSize() = configuration.map { configuration ->
    configuration.outputFonts.map { it.fontSize }.joinToString()
  }

  private data class ActualOutputFontSpec(val size: Int, val file: File)

  init {
    description = "Create a bitmap font using Hiero"
    group = GdxPlugin.TASK_GROUP

    logging.captureStandardOutput(LogLevel.LIFECYCLE)
    logging.captureStandardError(LogLevel.ERROR)
  }

  @TaskAction
  fun generate() {

    val configuration = configuration.get()

    if (configuration.outputFonts.isEmpty()) {
      throw GradleException("No output size(s) specified")
    }

    val tmpSettingsFile = File(temporaryDir, "font.settings")
    configuration.settings.get().toHieroSettings().apply {
      glyphText = configuration.characters.getOrNull() ?: configuration.NEHE
      when (configuration.inputFont.get()) {
        is File -> {
          font2File = (configuration.inputFont.get() as File).absolutePath
          isFont2Active = true
        }

        is String -> fontName = configuration.inputFont.get() as String
        else -> throw GradleException("inputFont should be either a String (name of a system font) or a file (a TTF file)")
      }
    }.save(tmpSettingsFile)

    execOperations.javaexec {
      mainClass.set("com.github.blueboxware.gdxplugin.utils.FontGenerator")
      classpath = classPath.get()
      args(
        listOf(tmpSettingsFile.absolutePath) + getActualOutputFontSpecs().map { it.size.toString() + ":" + it.file.absolutePath })
    }

  }

  private fun getActualOutputFontSpecs(): List<ActualOutputFontSpec> {

    val configuration = configuration.get()

    val startFile = (configuration.outputFile.orNull as? File) ?: ((configuration.outputFile.orNull as? String)
      ?: configuration.defaultName.orNull)?.let { fileOperations.file(it) } ?: throw GradleException(
      "No output file specified for BitmapFont $name"
    )
    val baseName = startFile.absolutePath.removeSuffix(".fnt")

    return configuration.outputFonts.map {
      val file = (it.file as? File) ?: (it.file as? String)?.let { str -> fileOperations.file(str) }
      ?: if (configuration.outputFonts.size > 1) {
        File(baseName + it.fontSize + "px.fnt")
      } else {
        File("$baseName.fnt")
      }
      ActualOutputFontSpec(it.fontSize, file)
    }

  }

}
