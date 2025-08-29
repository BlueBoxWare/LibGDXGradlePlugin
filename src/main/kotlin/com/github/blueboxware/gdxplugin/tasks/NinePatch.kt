package com.github.blueboxware.gdxplugin.tasks

import com.github.blueboxware.gdxplugin.GdxPlugin
import com.github.blueboxware.gdxplugin.capitalize
import com.github.blueboxware.gdxplugin.dsl.NinePatchConfiguration
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt


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
abstract class NinePatch @Inject constructor(
  private val fileOperations: FileOperations
) : DefaultTask() {

  @get:Nested
  abstract val configuration: Property<NinePatchConfiguration>

  init {
    description = "Create a nine patch"
    group = GdxPlugin.TASK_GROUP
  }

  @OutputFile
  fun getActualOutputFile() =
    configuration.flatMap { configuration ->
      configuration.output.asFile.orElse(
        configuration.image.map { imageFile ->
          FilenameUtils.removeExtension(imageFile.asFile.absolutePath).let {
            fileOperations.file("$it.9.png")
          }
        }
      )
    }

  @TaskAction
  fun generate() {

    val config = configuration.get()

    config.image.asFile.get().let { actualImage ->
      ImageIO.read(actualImage)?.let { srcImage ->
        val width = srcImage.width
        val height = srcImage.height

        val computed = if (config.auto.getOrElse(false)) {
          if (config.edgeDetect.getOrElse(false)) {
            guess(edgeDetect(srcImage))
          } else {
            guess(srcImage)
          }
        } else listOf(config.left.orNull, config.right.orNull, config.top.orNull, config.bottom.orNull)

        val left = computed.getOrNull(0) ?: 0
        val right = computed.getOrNull(1) ?: 0
        val top = computed.getOrNull(2) ?: 0
        val bottom = computed.getOrNull(3) ?: 0

        checkArg(left >= width, "left offset", "width", width)
        checkArg(right >= width, "right offset", "width", width)
        checkArg(top >= height, "top offset", "height", height)
        checkArg(bottom >= height, "bottom offset", "height", height)

        checkArg((config.paddingLeft.orNull ?: 0) >= width, "left padding", "width", width)
        checkArg((config.paddingRight.orNull ?: 0) >= width, "right padding", "width", width)
        checkArg((config.paddingTop.orNull ?: 0) >= height, "top padding", "height", height)
        checkArg((config.paddingBottom.orNull ?: 0) >= height, "bottom padding", "height", height)

        BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB).let { dstImage ->
          dstImage.createGraphics().let { graphics ->
            graphics.drawImage(srcImage, 1, 1, null)
            graphics.color = Color(0, 0, 0, 0)
            graphics.drawRect(0, 0, width + 2, height + 2)
            graphics.color = Color.BLACK
            graphics.drawLine(left + 1, 0, width - right, 0)
            graphics.drawLine(0, top + 1, 0, height - bottom)
            if (config.paddingLeft.orNull != null || config.paddingRight.orNull != null || config.paddingTop.orNull != null || config.paddingBottom.orNull != null) {
              graphics.drawLine(
                (config.paddingLeft.orNull ?: left) + 1,
                height + 1,
                width - (config.paddingRight.orNull ?: right),
                height + 1
              )
              graphics.drawLine(
                width + 1,
                (config.paddingTop.orNull ?: top) + 1,
                width + 1,
                height - (config.paddingBottom.orNull ?: bottom)
              )
            }
            ImageIO.write(dstImage, "png", getActualOutputFile().get())
          }
        }
      }
    }

  }

  private fun checkArg(cond: Boolean, arg: String, maxArg: String, maxValue: Int) {
    if (cond) {
      throw GradleException(arg.capitalize() + " must be smaller than image $maxArg (image $maxArg: $maxValue)")
    }
  }

  private fun guess(image: BufferedImage): List<Int?> {

    val config = configuration.get()

    val fuzziness = config.fuzziness.getOrElse(0f).coerceIn(0f, 100f)

    val centerX = config.centerX.orNull ?: config.left.orNull ?: config.right.orNull?.let { image.width - it - 1 }
    ?: (image.width / 2)
    val centerY = config.centerY.orNull ?: config.top.orNull ?: config.bottom.orNull?.let { image.height - it - 1 }
    ?: (image.height / 2)

    var left: Int? = config.left.orNull
    var right: Int? = config.right.orNull
    var top: Int? = config.top.orNull
    var bottom: Int? = config.bottom.orNull

    if (left == null) {
      var old: IntArray? = null
      for (x in centerX downTo 0) {
        val new = image.getColumn(x)
        if (old != null && diff(old, new) > fuzziness) {
          left = x + 1
          break
        }
        old = new
      }
    }
    if (right == null) {
      var old: IntArray? = null
      for (x in centerX until image.width) {
        val new = image.getColumn(x)
        if (old != null && diff(old, new) > fuzziness) {
          right = image.width - x
          break
        }
        old = new
      }
    }
    if (top == null) {
      var old: IntArray? = null
      for (y in centerY downTo 0) {
        val new = image.getRow(y)
        if (old != null && diff(old, new) > fuzziness) {
          top = y + 1
          break
        }
        old = new
      }
    }
    if (bottom == null) {
      var old: IntArray? = null
      for (y in centerY until image.height) {
        val new = image.getRow(y)
        if (old != null && diff(old, new) > fuzziness) {
          bottom = image.height - y
          break
        }
        old = new
      }
    }

    return listOf(left, right, top, bottom)
  }

  private fun BufferedImage.getColumn(x: Int): IntArray =
    (0 until height).map { getRGB(x, it) }.toIntArray()

  private fun BufferedImage.getRow(y: Int): IntArray =
    (0 until width).map { getRGB(it, y) }.toIntArray()

  private fun diff(a: IntArray, b: IntArray): Float =
    log10((((a.indices).map { colorDiff(a[it], b[it]) }).sum() / a.size)) / 0.0292724f

  private fun colorDiff(c1: Int, c2: Int): Float {
    val a1 = (c1 shr 24 and 0xFF).toFloat()
    val r1 = (c1 shr 16 and 0xFF).toFloat()
    val g1 = (c1 shr 8 and 0xFF).toFloat()
    val b1 = (c1 shr 0 and 0xFF).toFloat()

    val a2 = (c2 shr 24 and 0xFF).toFloat()
    val r2 = (c2 shr 16 and 0xFF).toFloat()
    val g2 = (c2 shr 8 and 0xFF).toFloat()
    val b2 = (c2 shr 0 and 0xFF).toFloat()

    if (a1 == 0f && a2 == 0f) {
      return 0f
    }

    return sqrt(2 * (a2 - a1).pow(2) + 2 * (r2 - r1).pow(2) + 4 * (g2 - g1).pow(2) + 3 * (b2 - b1).pow(2))
  }

  private fun edgeDetect(image: BufferedImage, debug: Boolean = false): BufferedImage {

    val kernel = arrayOf(
      0f, -1f, 0f,
      -1f, 4f, -1f,
      0f, -1f, 0f
    ).toFloatArray()
    val convolveOp = ConvolveOp(
      Kernel(3, 3, kernel),
      ConvolveOp.EDGE_NO_OP,
      RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    )

    val r = convolveOp.filter(image, null)

    if (debug) {
      ImageIO.write(r, "png", File(temporaryDir, "edge.png"))
    }

    return r

  }

}
