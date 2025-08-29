package com.github.blueboxware.gdxplugin.dsl

import com.badlogic.gdx.tools.hiero.HieroSettings
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont
import com.badlogic.gdx.tools.hiero.unicodefont.effects.*
import com.github.blueboxware.gdxplugin.RGB_REGEX
import com.github.blueboxware.gdxplugin.configure
import groovy.lang.Closure
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Stroke
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.reflect.full.createInstance


@Suppress("MemberVisibilityCanBePrivate")/*
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

  @Suppress("PropertyName")
  @Input
  var _effects: List<EffectWrapper> = listOf(EffectWrapper(ColorEffect(Color.white)))

  var effects: List<Effect>
    set(value) {
      _effects = value.map { EffectWrapper(it) }
    }
    @Internal get() = _effects.map { it.effect }

  fun color(string: String): Color {
    if (!string.matches(RGB_REGEX)) {
      throw GradleException("Invalid color specification: '$string' (should be 'rrggbb' or '#rrggbb')")
    }
    return string.removePrefix("#").let { EffectUtil.fromString(it) }
  }

  fun color(closure: Closure<in ColorEffect>): ColorEffect = ColorEffect().configure(closure)

  fun color(closure: ColorEffect.() -> Unit): ColorEffect = ColorEffect().apply(closure)

  @Suppress("unused")
  fun distanceField(closure: Closure<in DistanceFieldEffect>): DistanceFieldEffect =
    DistanceFieldEffect().configure(closure)

  @Suppress("unused")
  fun distanceField(closure: DistanceField.() -> Unit): DistanceFieldEffect = DistanceField().apply(closure).toGDX()

  @Suppress("unused")
  fun gradient(closure: Closure<in GradientEffect>): GradientEffect = GradientEffect().configure(closure)

  @Suppress("unused")
  fun gradient(closure: GradientEffect.() -> Unit): GradientEffect = GradientEffect().apply(closure)

  @Suppress("unused")
  fun outline(closure: Closure<in OutlineEffect>): OutlineEffect = OutlineEffect().configure(closure)

  @Suppress("unused")
  fun outline(closure: Outline.() -> Unit): OutlineEffect = Outline().apply(closure).toGDX()

  @Suppress("unused")
  fun wobble(closure: Closure<in OutlineWobbleEffect>): OutlineWobbleEffect = OutlineWobbleEffect().configure(closure)

  @Suppress("unused")
  fun wobble(closure: Wobble.() -> Unit): OutlineWobbleEffect = Wobble().apply(closure).toGDX()

  @Suppress("unused")
  fun zigzag(closure: Closure<in OutlineZigzagEffect>): OutlineZigzagEffect = OutlineZigzagEffect().configure(closure)

  @Suppress("unused")
  fun zigzag(closure: ZigZag.() -> Unit): OutlineZigzagEffect = ZigZag().apply(closure).toGDX()

  @Suppress("unused")
  fun shadow(closure: Closure<in ShadowEffect>): ShadowEffect = ShadowEffect().configure(closure)

  @Suppress("unused")
  fun shadow(closure: ShadowEffect.() -> Unit): ShadowEffect = ShadowEffect().apply(closure)

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
    hieroSettings.glyphPageWidth = glyphPageWidth
    hieroSettings.glyphPageHeight = glyphPageHeight
    hieroSettings.renderType = renderType.ordinal

    hieroSettings.effects.addAll(effects)

    return hieroSettings

  }

  class Outline(
    var width: Float = 2f,
    var color: Color = Color.BLACK,
    var join: Int = BasicStroke.JOIN_BEVEL,
    var stroke: Stroke? = null
  ) {

    internal fun toGDX(): OutlineEffect = OutlineEffect().apply {
      color = this@Outline.color
      join = this@Outline.join
      stroke = this@Outline.stroke
      setPrivateField("width", this@Outline.width)
    }

  }

  class Wobble(
    var width: Float = 2f, var color: Color = Color.BLACK, var detail: Float = 1f, var amplitude: Float = 1f
  ) {

    internal fun toGDX(): OutlineWobbleEffect {
      val wobble = OutlineWobbleEffect()
      wobble.color = color
      wobble.setPrivateField("detail", detail)
      wobble.setPrivateField("amplitude", amplitude)
      wobble.setPrivateField("width", width, wobble.javaClass.superclass)
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
      zigzag.setPrivateField("wavelength", wavelength)
      zigzag.setPrivateField("amplitude", amplitude)
      zigzag.setPrivateField("width", width, zigzag.javaClass.superclass)
      return zigzag

    }

  }

  class DistanceField(
    var color: Color = Color.WHITE, var scale: Int = 1, var spread: Float = 1f
  ) {

    internal fun toGDX(): DistanceFieldEffect {
      val df = DistanceFieldEffect()
      df.setPrivateField("color", color)
      df.setPrivateField("scale", scale)
      df.setPrivateField("spread", spread)
      return df
    }

  }

  @Suppress("unused", "PropertyName")
  @get:[Internal]
  val Java = UnicodeFont.RenderType.Java

  @Suppress("unused", "PropertyName")
  @get:[Internal]
  val Native = UnicodeFont.RenderType.Native

  @Suppress("unused", "PropertyName")
  @get:[Internal]
  val FreeType = UnicodeFont.RenderType.FreeType

  @Suppress("unused", "PropertyName")
  @get:[Internal]
  val JoinBevel = BasicStroke.JOIN_BEVEL

  @Suppress("unused", "PropertyName")
  @get:[Internal]
  val JoinMiter = BasicStroke.JOIN_MITER

  @Suppress("unused", "PropertyName")
  @get:[Internal]
  val JoinRound = BasicStroke.JOIN_ROUND

}

private fun Any.setPrivateField(name: String, value: Any?, clazz: Class<*>? = null) {
  (clazz ?: javaClass).getDeclaredField(name).let {
    it.isAccessible = true
    it.set(this, value)
  }
}

class EffectWrapper(var effect: Effect) : Serializable {

  private fun writeObject(out: ObjectOutputStream) {
    out.writeUTF(effect.javaClass.canonicalName)
    (effect as? ConfigurableEffect)?.let { configurableEffect ->
      out.writeInt(configurableEffect.values.size)
      for (value in configurableEffect.values.filterIsInstance<ConfigurableEffect.Value>()) {
          @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") out.writeUTF(value.`object`::class.simpleName)
          out.writeUTF(value.name)
          out.writeUTF(value.string)
      }
    }
  }

  private fun readObject(oi: ObjectInputStream) {
    val name = oi.readUTF()
    effect = Class.forName(name).kotlin.createInstance() as Effect
    val values = mutableListOf<ConfigurableEffect.Value>()
    val size = oi.readInt()
    repeat(size) {
      val className = oi.readUTF()
      val name = oi.readUTF()
      val valueString = oi.readUTF()
      val value = when (className) {
        Int::class.simpleName -> Integer.valueOf(valueString)
        Float::class.simpleName -> java.lang.Float.valueOf(valueString)
        Boolean::class.simpleName -> java.lang.Boolean.valueOf(valueString)
        Color::class.simpleName -> EffectUtil.fromString(valueString)
        String::class.simpleName -> valueString
        else -> throw GradleException("LibGDXGradle Plugin: Unsupported bitmap font effect attribute: $name (type: $className)")
      }
      values.add(DefaultValue(name, value))
    }
    (effect as? ConfigurableEffect)?.values = values
  }

  companion object {
    class DefaultValue(@Suppress("PropertyName") val _name: String, val value: Any?) : ConfigurableEffect.Value {
      override fun getName(): String = _name

      override fun setString(value: String?) {
      }

      override fun getString(): String = ""

      override fun getObject(): Any? = value

      override fun showDialog() {
      }
    }
  }

}
