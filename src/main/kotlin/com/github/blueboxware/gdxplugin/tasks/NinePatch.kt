package com.github.blueboxware.gdxplugin.tasks

import com.github.blueboxware.gdxplugin.GdxPlugin
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.*
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
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
open class NinePatch: DefaultTask() {

  @Input
  @Optional
  var left: Int? = null

  @Input
  @Optional
  var right: Int? = null

  @Input
  @Optional
  var top: Int? = null

  @Input
  @Optional
  var bottom: Int? = null

  @Input
  @Optional
  var paddingLeft: Int? = null

  @Input
  @Optional
  var paddingRight: Int? = null

  @Input
  @Optional
  var paddingTop: Int? = null

  @Input
  @Optional
  var paddingBottom: Int? = null

  @InputFile
  var image: File? = null

  @Suppress("MemberVisibilityCanBePrivate")
  var output: File? = null

  @Input
  var auto: Boolean = false

  @Input
  var edgeDetect: Boolean = false

  @Input
  var fuzziness: Float = 0f

  @Input
  @Optional
  var centerX: Int? = null

  @Input
  @Optional
  var centerY: Int? = null

  init {
    description = "Create a nine patch"
    group = GdxPlugin.TASK_GROUP
  }

  @OutputFile
  fun getActualOutputFile(): File = output ?: run {
    image?.let { inputFile ->
      val baseName = FilenameUtils.removeExtension(inputFile.absolutePath)
      project.file("$baseName.9.png")
    } ?: throw GradleException("Please specify an input image")
  }

  @TaskAction
  fun generate() {

    image?.let { actualImage ->
      ImageIO.read(actualImage)?.let { srcImage ->
        val width = srcImage.width
        val height = srcImage.height

        if (auto) {
          if (edgeDetect) {
            guess(edgeDetect(srcImage))
          } else {
            guess(srcImage)
          }
        }

        val top = top ?: 0
        val bottom = bottom ?: 0
        val right = right ?: 0
        val left = left ?: 0

        checkArg(left >= width, "left offset", "width", width)
        checkArg(right >= width, "right offset", "width", width)
        checkArg(top >= height, "top offset", "height", height)
        checkArg(bottom >= height, "bottom offset", "height", height)

        checkArg(paddingLeft ?: 0 >= width, "left padding", "width", width)
        checkArg(paddingRight ?: 0 >= width, "right padding", "width", width)
        checkArg(paddingTop ?: 0 >= height, "top padding", "height", height)
        checkArg(paddingBottom ?: 0 >= height, "bottom padding", "height", height)

        BufferedImage(width + 2, height + 2, BufferedImage.TYPE_INT_ARGB).let {dstImage ->
          dstImage.createGraphics().let { graphics ->
            graphics.drawImage(srcImage, 1, 1, null)
            graphics.color = Color(0, 0, 0, 0)
            graphics.drawRect(0, 0, width + 2, height + 2)
            graphics.color = Color.BLACK
            graphics.drawLine(left + 1, 0, width - right, 0)
            graphics.drawLine(0, top + 1, 0, height - bottom )
            if (paddingLeft != null || paddingRight != null || paddingTop != null || paddingBottom != null) {
              graphics.drawLine((paddingLeft ?: left) + 1, height + 1, width - (paddingRight ?: right) , height + 1)
              graphics.drawLine(width + 1, (paddingTop ?: top) + 1, width + 1, height - (paddingBottom ?: bottom))
            }
            ImageIO.write(dstImage, "png", getActualOutputFile())
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

  private fun guess(image: BufferedImage) {

    val fuzziness = fuzziness.coerceIn(0f, 100f)

    val centerX = centerX ?: left ?: right?.let { image.width - it - 1 } ?: image.width / 2
    val centerY = centerY ?: top ?: bottom?.let { image.height - it - 1 } ?: image.height / 2

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

    val r =  convolveOp.filter(image, null)

    if (debug) {
      ImageIO.write(r, "png", File(temporaryDir, "edge.png"))
    }

    return r

  }

}