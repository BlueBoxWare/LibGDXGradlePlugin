package com.github.blueboxware.gdxplugin

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
internal fun <T: Any> closure(f: () -> T): Closure<T> =
  object: Closure<T>(null) {
    override fun call(): T = f()
  }

internal fun prettyPrint(value: Any?): String =
        when (value) {
          null -> "null"
          is String, is Enum<*> -> "\"" + value + "\""
          else -> collectionToList(value)?.joinToString(prefix = "[", postfix = "]") { prettyPrint(it) }
                  ?: value.toString()
        }

internal fun collectionToList(value: Any): List<*>? =
        when(value) {
          is Collection<*> -> value.toList()
          is Array<*> -> value.toList()
          is IntArray -> value.toList()
          is FloatArray -> value.toList()
          is BooleanArray -> value.toList()
          is ByteArray -> value.toList()
          is CharArray -> value.toList()
          is ShortArray -> value.toList()
          is LongArray -> value.toList()
          is DoubleArray -> value.toList()
          else -> null
        }
