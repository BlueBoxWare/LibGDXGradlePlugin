
import com.badlogic.gdx.tools.texturepacker.ImageProcessor
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import java.awt.image.BufferedImage
import java.io.File
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

internal val CURRENT_VERSION_REGEX = Regex("""pluginVersion\s*=\s*'([^']+)'""")
internal val RELEASED_VERSION_REGEX = Regex("""releasedPluginVersion\s*=\s*'([^']+)'""")

internal fun getCurrentVersion() =
        CURRENT_VERSION_REGEX.find(File("versions.gradle").readText())?.groupValues?.getOrNull(1) ?: throw AssertionError()

internal fun getReleasedVersion() =
        RELEASED_VERSION_REGEX.find(File("versions.gradle").readText())?.groupValues?.getOrNull(1) ?: throw AssertionError()


internal operator fun File.get(child: String): File {
  assert(isDirectory)
  return File(this, child)
}

internal operator fun TemporaryFolder.get(child: String): File = root[child]

internal fun String.prefixIfNecessary(prefix: String): String =
        if (startsWith(prefix)) this else prefix + this

internal fun BufferedImage.getRect(): TexturePacker.Rect =
        ImageProcessor(TexturePacker.Settings()).addImage(this, "foo.9")

internal fun getRect(file: File): TexturePacker.Rect = ImageIO.read(file).getRect()