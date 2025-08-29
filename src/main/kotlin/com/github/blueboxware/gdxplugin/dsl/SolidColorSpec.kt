package com.github.blueboxware.gdxplugin.dsl

import com.github.blueboxware.gdxplugin.RGBA_REGEX
import com.github.blueboxware.gdxplugin.RGB_REGEX
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import java.awt.Color
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable


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
class SolidColorSpec: Serializable {

  @Input
  var name: String? = null

  @Input
  var color: Color = Color.WHITE

  @Input
  var width: Int = 1

  @Input
  var height: Int = 1

  fun color(string: String): Color {

    if (!string.matches(RGB_REGEX) && !string.matches(RGBA_REGEX)) {
      throw GradleException("Invalid color specification: '$string' (should be 'rrggbb', 'rrggbbaa', '#rrggbb' or '#rrggbbaa')")
    }

   return string.removePrefix("#").let {str ->
      try {
        val r = Integer.valueOf(str.take(2), 16)
        val g = Integer.valueOf(str.substring(2, 4), 16)
        val b = Integer.valueOf(str.substring(4, 6), 16)
        val a = if (str.length == 8) Integer.valueOf(str.substring(6, 8), 16) else 255

        Color(r / 255f, g / 255f, b / 255f, a / 255f)
      } catch (e: NumberFormatException) {
        throw GradleException("Invalid color specification: '$string'")
      }
    }

  }

  private fun writeObject(out: ObjectOutputStream) {
    out.writeUTF(name)
    for (i in listOf(width, height, color.red, color.green, color.blue, color.alpha)) {
      out.writeInt(i)
    }
  }

  private fun readObject(oi: ObjectInputStream) {
    name = oi.readUTF()
    width = oi.readInt()
    height = oi.readInt()
    color = Color(oi.readInt(), oi.readInt(), oi.readInt(), oi.readInt())
  }

  override fun toString(): String = "name=$name, color=$color, width=$width, height=$height"

}
