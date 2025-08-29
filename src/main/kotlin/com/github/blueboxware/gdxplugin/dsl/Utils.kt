@file:JvmName("Utils")
@file:Suppress("unused")

package com.github.blueboxware.gdxplugin.dsl

import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.github.blueboxware.gdxplugin.configure
import groovy.lang.Closure

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
@JvmOverloads
fun packSettings(baseSettings: TexturePacker.Settings? = null, closure: Closure<in TexturePacker.Settings>): TexturePacker.Settings {
  val settings = TexturePacker.Settings()
  baseSettings?.let { settings.set(it) }
  settings.configure(closure)
  return settings
}

@JvmOverloads
fun packSettings(baseSettings: TexturePacker.Settings? = null, closure: TexturePacker.Settings.() -> Unit): TexturePacker.Settings {
  val settings = TexturePacker.Settings()
  baseSettings?.let { settings.set(it) }
  settings.apply(closure)
  return settings
}
