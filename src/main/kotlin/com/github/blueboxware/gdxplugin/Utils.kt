package com.github.blueboxware.gdxplugin

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.TaskInputPropertyBuilder


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
internal fun <T: Any> closure(f: () -> T): Closure<T> =
  object: Closure<T>(null) {
    override fun call(): T = f()
  }

internal fun Task.input(name: String, f: () -> Any): TaskInputPropertyBuilder = inputs.property(name, closure(f))

internal fun prettyPrint(value: Any?): String {

  fun iterator(value: Any?): Iterator<*>? =
    when(value) {
      is Collection<*> -> value.iterator()
      is Array<*> -> value.iterator()
      is IntArray -> value.iterator()
      is FloatArray -> value.iterator()
      is BooleanArray -> value.iterator()
      is ByteArray -> value.iterator()
      is CharArray -> value.iterator()
      is ShortArray -> value.iterator()
      is LongArray -> value.iterator()
      is DoubleArray -> value.iterator()
      else -> null
    }

  return when (value) {
    null -> "null"
    is String -> "\"" + value + "\""
    is Enum<*> -> "\"" + value + "\""
    else -> iterator(value)?.asSequence()?.map { prettyPrint(it) }?.joinToString(prefix = "[", postfix = "]") ?: value.toString()
  }
}

