package com.github.blueboxware.gdxplugin.dsl

import com.badlogic.gdx.tools.hiero.HieroSettings
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont
import com.badlogic.gdx.tools.hiero.unicodefont.effects.*
import com.github.blueboxware.gdxplugin.RGB_REGEX
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.util.ConfigureUtil
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Stroke


@Suppress("MemberVisibilityCanBePrivate")
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
class BitmapFontSettings {

  @Input
  var bold: Boolean = false

  @Input
  var italic: Boolean = false

  @Input
  var mono: Boolean = false

  @Input
  var gamma: Float = 1.8f

  @Input
  var paddingTop: Int = 1

  @Input
  var paddingBottom: Int = 1

  @Input
  var paddingLeft: Int = 1

  @Input
  var paddingRight: Int = 1

  @Input
  var paddingAdvanceX: Int = -2

  @Input
  var paddingAdvanceY: Int = -2

  @Input
  var glyphPageWidth: Int = 512

  @Input
  var glyphPageHeight: Int = 512

  @Input
  var renderType: UnicodeFont.RenderType = UnicodeFont.RenderType.Java

  var effects: List<ConfigurableEffect> = listOf(ColorEffect(Color.WHITE))

  @Input
  fun getEffectAsString(): String {
    val builder = StringBuilder()
    for (effect in effects) {
      builder.appendln("#" + effect.javaClass.name)
      for (value in effect.values.filterIsInstance<ConfigurableEffect.Value>()) {
        builder.appendln(value.name + "=" + value.string)
      }
    }
    return builder.toString()
  }

  @Suppress("unused")
  fun color(string: String): Color {
    if (!string.matches(RGB_REGEX)) {
      throw GradleException("Invalid color specification: '$string' (should be 'rrggbb' or '#rrggbb')")
    }
    return string.removePrefix("#").let { EffectUtil.fromString(it) }
  }

  @Suppress("unused")
  fun color(closure: Closure<in ColorEffect>): ColorEffect =
          ConfigureUtil.configure(closure, ColorEffect())

  @Suppress("unused")
  fun color(closure: ColorEffect.() -> Unit): ColorEffect =
          ColorEffect().apply(closure)

  @Suppress("unused")
  fun distanceField(closure: Closure<in DistanceFieldEffect>): DistanceFieldEffect =
          ConfigureUtil.configure(closure, DistanceFieldEffect())

  @Suppress("unused")
  fun distanceField(closure: DistanceField.() -> Unit): DistanceFieldEffect =
          DistanceField().apply(closure).toGDX()

  @Suppress("unused")
  fun gradient(closure: Closure<in GradientEffect>): GradientEffect =
          ConfigureUtil.configure(closure, GradientEffect())

  @Suppress("unused")
  fun gradient(closure: GradientEffect.() -> Unit): GradientEffect =
          GradientEffect().apply(closure)

  @Suppress("unused")
  fun outline(closure: Closure<in OutlineEffect>): OutlineEffect =
          ConfigureUtil.configure(closure, OutlineEffect())

  @Suppress("unused")
  fun outline(closure: Outline.() -> Unit): OutlineEffect =
          Outline().apply(closure).toGDX()

  @Suppress("unused")
  fun wobble(closure: Closure<in OutlineWobbleEffect>): OutlineWobbleEffect =
          ConfigureUtil.configure(closure, OutlineWobbleEffect())

  @Suppress("unused")
  fun wobble(closure: Wobble.() -> Unit): OutlineWobbleEffect =
          Wobble().apply(closure).toGDX()

  @Suppress("unused")
  fun zigzag(closure: Closure<in OutlineZigzagEffect>): OutlineZigzagEffect =
          ConfigureUtil.configure(closure, OutlineZigzagEffect())

  @Suppress("unused")
  fun zigzag(closure: ZigZag.() -> Unit): OutlineZigzagEffect =
          ZigZag().apply(closure).toGDX()

  @Suppress("unused")
  fun shadow(closure: Closure<in ShadowEffect>): ShadowEffect =
          ConfigureUtil.configure(closure, ShadowEffect())

  @Suppress("unused")
  fun shadow(closure: ShadowEffect.() -> Unit): ShadowEffect =
          ShadowEffect().apply(closure)

  internal fun toHieroSettings(): HieroSettings {

    val hieroSettings = HieroSettings()
    hieroSettings.isBold = bold
    hieroSettings.isItalic = italic
    hieroSettings.isMono = mono
    hieroSettings.gamma = gamma
    hieroSettings.paddingTop = paddingTop
    hieroSettings.paddingBottom = paddingBottom
    hieroSettings.paddingLeft = paddingLeft
    hieroSettings.paddingRight = paddingRight
    hieroSettings.paddingAdvanceX = paddingAdvanceX
    hieroSettings.paddingAdvanceY = paddingAdvanceY
    hieroSettings.glyphPageWidth =glyphPageWidth
    hieroSettings.glyphPageHeight =glyphPageHeight
    hieroSettings.renderType = renderType.ordinal

    effects.forEach {
      hieroSettings.effects.add(it)
    }

    return hieroSettings

  }

  class Outline(
          var width: Float = 2f,
          var color: Color = Color.BLACK,
          var join: Int = BasicStroke.JOIN_BEVEL,
          var stroke: Stroke? = null
  ) {

    internal fun toGDX(): OutlineEffect =
            OutlineEffect().apply {
              color = this@Outline.color
              join = this@Outline.join
              stroke = this@Outline.stroke
              this.javaClass.getDeclaredField("width").let {
                it.isAccessible = true
                it.set(this, this@Outline.width)
              }
            }

  }

  class Wobble(
          var width: Float = 2f,
          var color: Color = Color.BLACK,
          var detail: Float = 1f,
          var amplitude: Float = 1f
  ) {

    internal fun toGDX(): OutlineWobbleEffect {
      val wobble = OutlineWobbleEffect()
      wobble.color = color
      wobble.javaClass.getDeclaredField("detail").let {
        it.isAccessible = true
        it.set(wobble, detail)
      }
      wobble.javaClass.getDeclaredField("amplitude").let {
        it.isAccessible = true
        it.set(wobble, amplitude)
      }
      wobble.javaClass.superclass.getDeclaredField("width").let {
        it.isAccessible = true
        it.set(wobble, width)
      }
      return wobble

    }

  }

  class ZigZag(
          var width: Float = 2f,
          var color: Color = Color.BLACK,
          var wavelength: Float = 3f,
          var amplitude: Float = 1f,
          var join: Int = BasicStroke.JOIN_BEVEL
  ) {

    internal fun toGDX(): OutlineZigzagEffect {
      val zigzag = OutlineZigzagEffect()
      zigzag.join = join
      zigzag.color = color
      zigzag.javaClass.getDeclaredField("wavelength").let {
        it.isAccessible = true
        it.set(zigzag, wavelength)
      }
      zigzag.javaClass.getDeclaredField("amplitude").let {
        it.isAccessible = true
        it.set(zigzag, amplitude)
      }
      zigzag.javaClass.superclass.getDeclaredField("width").let {
        it.isAccessible = true
        it.set(zigzag, width)
      }
      return zigzag

    }

  }

  class DistanceField(
          var color: Color = Color.WHITE,
          var scale: Int = 1,
          var spread: Float = 1f
  ) {

    internal fun toGDX(): DistanceFieldEffect {
      val df = DistanceFieldEffect()
      df.javaClass.getDeclaredField("color").let {
        it.isAccessible = true
        it.set(df, color)
      }
      df.javaClass.getDeclaredField("scale").let {
        it.isAccessible = true
        it.set(df, scale)
      }
      df.javaClass.getDeclaredField("spread").let {
        it.isAccessible = true
        it.set(df, spread)
      }
      return df
    }

  }

  @Suppress("unused", "PropertyName")
  val Java = UnicodeFont.RenderType.Java
  @Suppress("unused", "PropertyName")
  val Native = UnicodeFont.RenderType.Native
  @Suppress("unused", "PropertyName")
  val FreeType = UnicodeFont.RenderType.FreeType

  @Suppress("unused", "PropertyName")
  val JoinBevel = BasicStroke.JOIN_BEVEL
  @Suppress("unused", "PropertyName")
  val JoinMiter = BasicStroke.JOIN_MITER
  @Suppress("unused", "PropertyName")
  val JoinRound = BasicStroke.JOIN_ROUND

}