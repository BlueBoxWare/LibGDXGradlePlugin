package com.github.blueboxware.gdxplugin.utils

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration.disableAudio
import com.badlogic.gdx.tools.hiero.BMFontUtil
import com.badlogic.gdx.tools.hiero.HieroSettings
import com.badlogic.gdx.tools.hiero.unicodefont.UnicodeFont
import com.badlogic.gdx.tools.hiero.unicodefont.effects.ConfigurableEffect
import java.awt.Font
import java.io.File
import java.lang.Thread.sleep
import kotlin.system.exitProcess

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
internal class FontGenerator(
        private val settingsFile: String,
        private val outputSpecs: List<Pair<Int, String>>
): ApplicationAdapter() {

  var done = false
  var disposed = false

  override fun create() {
    Gdx.graphics.isContinuousRendering = false
  }

  override fun render() {

    if (done) return

    try {

      for ((size, outputFileName) in outputSpecs) {

        val hieroSettings = HieroSettings(settingsFile)
        val bold = hieroSettings.isBold
        val italic = hieroSettings.isItalic

        val unicodeFont = if (hieroSettings.isFont2Active) {
          val fontFile = hieroSettings.font2File
          UnicodeFont(fontFile, size, bold, italic)
        } else {
          val fontName = hieroSettings.fontName
          val font = Font.decode(fontName)
          UnicodeFont(font, size, bold, italic)
        }

        unicodeFont.mono = hieroSettings.isMono
        unicodeFont.gamma = hieroSettings.gamma
        unicodeFont.paddingTop = hieroSettings.paddingTop
        unicodeFont.paddingBottom = hieroSettings.paddingBottom
        unicodeFont.paddingLeft = hieroSettings.paddingLeft
        unicodeFont.paddingRight = hieroSettings.paddingRight
        unicodeFont.paddingAdvanceX = hieroSettings.paddingAdvanceX
        unicodeFont.paddingAdvanceY = hieroSettings.paddingAdvanceY
        unicodeFont.glyphPageWidth = hieroSettings.glyphPageWidth
        unicodeFont.glyphPageHeight = hieroSettings.glyphPageHeight
        unicodeFont.renderType = UnicodeFont.RenderType.values()[hieroSettings.renderType]

        for (effect in hieroSettings.effects) {
          val configurableEffect = effect as ConfigurableEffect
          unicodeFont.effects.add(configurableEffect)
        }

        unicodeFont.addGlyphs(hieroSettings.glyphText)

        val bmFont = BMFontUtil(unicodeFont)
        bmFont.save(File(outputFileName))

      }
    } catch (e: Exception) {
      println(e.message)
      System.err.println("Could not create bitmap font: " + e.message)
      e.printStackTrace()
    } finally {
      done = true
    }

  }

  override fun dispose() {
    disposed = true
  }

  companion object {

    @JvmStatic
    fun main(args : Array<String>) {
      val settingsFile = args[0]
      val outputSpecs = args.drop(1).map {
        val splitted = it.split(':', limit = 2)
        Pair(splitted[0].toInt(), splitted[1])
      }

      val fontCreator = FontGenerator(settingsFile, outputSpecs)
      val config = LwjglApplicationConfiguration().apply {
        disableAudio = true
        allowSoftwareMode = true
        forceExit = false
        width = 1
        height = 1
      }
      val app = LwjglApplication(fontCreator, config)
      Gdx.graphics.requestRendering()
      while (!fontCreator.done) {
        sleep(500)
      }
      app.exit()
      while (!fontCreator.disposed) {
        sleep(500)
      }
      exitProcess(0)
    }

  }

}