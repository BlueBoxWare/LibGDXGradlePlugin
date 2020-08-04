package com.github.blueboxware.gdxplugin.tasks

import com.badlogic.gdx.tools.distancefield.DistanceFieldGenerator
import com.github.blueboxware.gdxplugin.GdxPlugin
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.LogLevel
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
@Suppress("MemberVisibilityCanBePrivate")
open class DistanceField: DefaultTask() {

  var color: String = "ffffff"
    @Input get

  var outputFormat: String? = null
    @Input @Optional get

  var downscale: Int = 1
    @Input get

  var spread: Float = 1f
    @Input get

  var inputFile: File? = null
    @InputFile @Optional get

  var outputFile: File? = null

  init {
    description = "Create a Distance Field from an images using LibGDX's DistanceFieldGenerator"
    group = GdxPlugin.TASK_GROUP

    logging.captureStandardOutput(LogLevel.LIFECYCLE)
    logging.captureStandardError(LogLevel.ERROR)
  }

  @TaskAction
  fun generate() {

    if (inputFile == null || inputFile?.isFile != true) {
      throw InvalidUserDataException("Please specify the input image using the inputFile parameter")
    }

    inputFile?.let { realInputFile ->

      if (!realInputFile.exists()) {
        throw FileNotFoundException("File does not exist: '${realInputFile.absolutePath}'")
      }

      val realOutputFormat = getActualOutputFormat()

      getActualOutputFile()?.let { realOutputFile ->

        if (!ImageIO.getImageWritersByFormatName(realOutputFormat).hasNext()) {
          throw InvalidUserDataException("Invalid output format: '$realOutputFormat'")
        }

        ImageIO.read(realInputFile)?.let { inputImage ->
          val generator = DistanceFieldGenerator().apply {
            color = Color(Integer.parseInt(this@DistanceField.color.removePrefix("#"), 16))
            spread = this@DistanceField.spread
            downscale = this@DistanceField.downscale
          }

            generator.generateDistanceField(inputImage)?.let { outputImage ->
              try {
                ImageIO.write(outputImage, realOutputFormat, realOutputFile).let {
                  if (!it) {
                    throw GradleException("Could not find appropriate writer for image (type ${inputImage.type})")
                  }
                }
              } catch (e: IIOException) {
                if (realOutputFormat == "jpg" && outputImage.colorModel.hasAlpha()) {
                  throw GradleException("OpenJDK does not support creating jpegs with alpha.")
                }
              }
            }
        }

      }

    }

  }

  private fun getActualOutputFormat(): String = outputFormat?.removePrefix(".")
          ?: outputFile?.let { FilenameUtils.getExtension(it.absolutePath) }
          ?: "png"

  @OutputFile
  @Optional
  private fun getActualOutputFile(): File? = outputFile ?: run {
    inputFile?.let { inputFile ->
      val baseName = FilenameUtils.removeExtension(inputFile.absolutePath) + "-df"
      val extension = getActualOutputFormat()
      File("$baseName.$extension")
    }
  }

}