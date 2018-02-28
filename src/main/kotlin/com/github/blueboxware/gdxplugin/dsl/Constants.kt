@file:Suppress("unused")

package com.github.blueboxware.gdxplugin.dsl

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.tools.texturepacker.TexturePacker

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
val Nearest = Texture.TextureFilter.Nearest
val Linear = Texture.TextureFilter.Linear
val MipMap = Texture.TextureFilter.MipMap
val MipMapNearestNearest = Texture.TextureFilter.MipMapNearestNearest
val MipMapLinearNearest = Texture.TextureFilter.MipMapLinearNearest
val MipMapNearestLinear = Texture.TextureFilter.MipMapNearestLinear
val MipMapLinearLinear = Texture.TextureFilter.MipMapLinearLinear

val MirroredRepeat = Texture.TextureWrap.MirroredRepeat
val ClampToEdge = Texture.TextureWrap.ClampToEdge
val Repeat = Texture.TextureWrap.Repeat

val Alpha = Pixmap.Format.Alpha
val Intensity = Pixmap.Format.Intensity
val LuminanceAlpha = Pixmap.Format.LuminanceAlpha
val RGB565 = Pixmap.Format.RGB565
val RGBA4444 = Pixmap.Format.RGBA4444
val RGB888 = Pixmap.Format.RGB888
val RGBA8888 = Pixmap.Format.RGBA8888

object Resampling {
  val Nearest = TexturePacker.Resampling.nearest
  val Bilinear = TexturePacker.Resampling.bilinear
  val Bicubic = TexturePacker.Resampling.bicubic
}