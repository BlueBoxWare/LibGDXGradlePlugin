package com.github.blueboxware.gdxplugin

import groovy.lang.Closure
import org.gradle.api.tasks.WorkResult
import org.gradle.util.internal.ConfigureUtil
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO


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
internal val RGB_REGEX = Regex("""#?[\da-fA-F]{6}""")
internal val RGBA_REGEX = Regex("""#?[\da-fA-F]{8}""")

// Don't use Action(s), doesn't work.
internal fun <T : Any> T.configure(closure: Closure<in T>): T {
    ConfigureUtil.configure(closure, this)
    return this
}

internal fun <T : Any> closure(f: () -> T): Closure<T> = object : Closure<T>(null) {
    override fun call(): T = f()
}

internal inline fun <T : Any, reified P : Any> closure(crossinline f: (P) -> T): Closure<T> =
    object : Closure<T>(null) {
        override fun call(vararg args: Any?): T = f(args.firstOrNull() as P)
    }

internal fun prettyPrint(value: Any?): String = when (value) {
    null -> "null"
    is String, is Enum<*> -> "\"" + value + "\""
    else -> collectionToList(value)?.joinToString(prefix = "[", postfix = "]") { prettyPrint(it) } ?: value.toString()
}

internal fun collectionToList(value: Any): List<*>? = when (value) {
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

internal fun createSolidColorImage(outputFile: File, color: Color, width: Int, height: Int) {

    val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = bufferedImage.createGraphics()
    graphics.paint = color
    graphics.fillRect(0, 0, width, height)
    ImageIO.write(bufferedImage, "png", outputFile)

}

internal fun String.capitalize(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

internal fun Char.titlecase(locale: Locale): String {
    val localizedUppercase = uppercase(locale)
    if (localizedUppercase.length > 1) {
        return if (this == '\u0149') localizedUppercase else localizedUppercase[0] + localizedUppercase.substring(1)
            .lowercase()
    }
    if (localizedUppercase != uppercase()) {
        return localizedUppercase
    }
    return titlecaseChar().toString()
}

internal fun Char.uppercase(locale: Locale): String = toString().uppercase(locale)

internal val DID_WORK = WorkResult { true }

internal val DID_NO_WORK = WorkResult { false }
