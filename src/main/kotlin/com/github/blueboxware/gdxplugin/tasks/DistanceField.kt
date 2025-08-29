package com.github.blueboxware.gdxplugin.tasks

import com.badlogic.gdx.tools.distancefield.DistanceFieldGenerator
import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.dsl.DistanceFieldConfiguration
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.awt.Color
import java.io.File
import java.io.FileNotFoundException
import javax.imageio.IIOException
import javax.imageio.ImageIO


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
abstract class DistanceField : DefaultTask() {

  @get:Nested
  abstract val configuration: Property<DistanceFieldConfiguration>

  init {
    description = "Create a Distance Field from an images using libGDX's DistanceFieldGenerator"
    group = GdxPlugin.TASK_GROUP

    logging.captureStandardOutput(LogLevel.LIFECYCLE)
    logging.captureStandardError(LogLevel.ERROR)
  }

  @TaskAction
  fun generate() {

    val config = configuration.get()

    val inputFile = config.inputFile.orNull?.asFile

    if (inputFile == null || !inputFile.isFile) {
      throw InvalidUserDataException("Please specify the input image using the inputFile parameter")
    }

    if (!inputFile.exists()) {
      throw FileNotFoundException("File does not exist: '${inputFile.absolutePath}'")
    }

    val realOutputFormat = getActualOutputFormat()

    getActualOutputFile().orNull?.let { realOutputFile ->

      if (!ImageIO.getImageWritersByFormatName(realOutputFormat).hasNext()) {
        throw InvalidUserDataException("Invalid output format: '$realOutputFormat'")
      }

      val color = config.color.orNull ?: "ffffff"
      val downScale = config.downscale.orNull ?: 1
      val spread = config.spread.orNull ?: 1f

      ImageIO.read(inputFile)?.let { inputImage ->
        val generator = DistanceFieldGenerator().apply {
          this.color = Color(Integer.parseInt(color.removePrefix("#"), 16))
          this.spread = spread
          this.downscale = downScale
        }

        generator.generateDistanceField(inputImage)?.let { outputImage ->
          var jvm = "Unknown JDK"
          try {
            jvm = System.getProperty("java.vm.name")
          } catch (e: Exception) {
            // Nothing
          }
          val type = if (realOutputFormat == "jpg") "JPG" else inputImage.type.toString()
          try {
            ImageIO.write(outputImage, realOutputFormat, realOutputFile).let {
              if (!it) {
                throw GradleException("$jvm does not have a writer for image type $type")
              }
            }
          } catch (e: IIOException) {
            if (realOutputFormat == "jpg" && outputImage.colorModel.hasAlpha()) {

              throw GradleException("$jvm does not support creating jpegs with alpha.")
            }
          }
        }
      }

    }

  }

  private fun getActualOutputFormat(): String = configuration.get().outputFormat.orNull?.removePrefix(".")
    ?: configuration.get().outputFile.orNull?.asFile?.let { FilenameUtils.getExtension(it.absolutePath) }
    ?: "png"

@OutputFile
@Optional
internal fun getActualOutputFile() =
  configuration.flatMap { configuration ->
    configuration.outputFile.asFile.orElse(
      configuration.inputFile.map { inputFile ->
        val baseName = FilenameUtils.removeExtension(inputFile.asFile.absolutePath) + "-df"
        val extension = getActualOutputFormat()
        File("$baseName.$extension")
      }
    )
  }

}
