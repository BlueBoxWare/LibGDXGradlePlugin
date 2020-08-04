# GdxPlugin

> **If you are using the Gradle Kotlin DSL, see [README-kotlin](README-kotlin.md)**

GdxPlugin is a Gradle plugin that adds a few [LibGDX](https://libgdx.badlogicgames.com/) related tasks to:

* create Texture Packs (a.k.a. Texture Atlases) using [TexturePacker](https://github.com/libgdx/libgdx/wiki/Texture-packer)
* create Bitmap Fonts using [Hiero](https://github.com/libgdx/libgdx/wiki/Hiero)
* create [Nine Patches](https://github.com/libgdx/libgdx/wiki/Ninepatches)
* create Distance Fields from single images using [DistanceFieldGenerator](https://github.com/libgdx/libgdx/wiki/Distance-field-fonts#using-distance-fields-for-arbitrary-images)

**This plugin requires Gradle 3.5 or higher**

# Table of Contents

<!-- toc -->
- __[Getting started](#getting-started)__
  - __[Add the plugin](#add-the-plugin)__
  - __[Packing textures](#packing-textures)__
  - __[Creating a Bitmap Font](#creating-a-bitmap-font)__
  - __[Creating Nine Patches](#creating-nine-patches)__
  - __[Creating Distance Fields](#creating-distance-fields)__
- __[PackTextures task](#packtextures-task)__
  - __[Settings](#settings)__
  - __[Generating multiple texture packs](#generating-multiple-texture-packs)__
  - __[Adding solid color textures](#adding-solid-color-textures)__
  - __[Dependencies on BitmapFont or DistanceField task](#dependencies-on-bitmapfont-or-distancefield-task)__
  - __[Reusing settings](#reusing-settings)__
  - __[Multiple input directories, filtering and renaming](#multiple-input-directories-filtering-and-renaming)__
  - __[Using "pack.json"](#using-packjson)__
  - __[Custom tasks](#custom-tasks)__
- __[BitmapFont task](#bitmapfont-task)__
  - __[Input font and characters](#input-font-and-characters)__
  - __[Output font](#output-font)__
  - __[Settings](#settings-1)__
  - __[Effects](#effects)__
- __[NinePatch task](#ninepatch-task)__
  - __[Arguments](#arguments)__
  - __[Automatic inset generation](#automatic-inset-generation)__
- __[DistanceField task](#distancefield-task)__
  - __[Arguments](#arguments-1)__
- __[General](#general)__
  - __[LibGDX version](#libgdx-version)__
- __[Changelog](#changelog)__
  - __[1.3](#13)__
  - __[1.2.2](#122)__
  - __[1.2.1](#121)__
  - __[1.2](#12)__
  - __[1.1.2](#112)__
  - __[1.1.1](#111)__
  - __[1.1](#11)__
  - __[1.0.1](#101)__
<!-- /toc -->

# Getting started
## Add the plugin
Add the plugin to your project:

```groovy
plugins {
  id "com.github.blueboxware.gdx" version "1.3"
}
```

## Packing textures
Creating a packTextures task:

```groovy
packTextures {

  // The directory which contains the images to pack
  from 'textures/'

  // The target directory: 'pack.atlas' is placed in this directory
  into 'assets/'

  settings {
    // Settings for TexturePacker
    filterMin = "MipMapLinearNearest"
    filterMag = "MipMap"
  }

}
```

Run the task (or just do a build):

```dos
gradlew.bat packTextures
```

## Creating a Bitmap Font
To create a Bitmap Font:

```groovy
bitmapFonts {

    // We name the font 'text': this creates a task called 'generateTextFont'
    text {

        inputFont = file("fonts/roboto.ttf")

        outputFile = file("assets/textFont.fnt")

        // Create the font in 2 sizes. The created fonts will be put in "textFont32px.fnt" and "textFont64px.fnt"
        sizes = [32, 48]

        // The settings to use for the fonts
        settings {

            bold = true

            // The effects to apply
            effects = [
                color {
                    color = color("#ff0000")
                },
                shadow {
                    opacity = 0.4
                    xDistance = 4
                    yDistance = 4
                }
            ]

        }

    }

}
```

Run the task (or just do a build):

```dos
gradlew.bat generateTextFont
```

## Creating Nine Patches
To create Nine Patches:

```groovy
ninePatch {

    // Creates a task called generateRectangleNinePatch
    rectangle {

        image = file('textures/rect.png')
        output = file('assets/rect.9.png')

        // The insets, as number of pixels from the left/right/top/bottom of the image
        left = 2
        right = 2
        top = 4
        bottom = 4

    }

    // Creates a task called generateBorderNinePatch
    border {

        image = file('textures/border.png')
        output = file('assets/border.9.png')

        left = 2
        right = 2
        top = 4
        bottom = 4

        // The paddings
        // If you don't specify any padding, no padding is included in the ninepatch
        paddingLeft = 4
        paddingRight = 4
        paddingTop = 4
        paddingBottom = 4

    }

}
```

To create all the ninepatches, run the `createAllNinePatches` task.

## Creating Distance Fields
To create Distance Fields from single images:

```groovy
distanceFields {

    // Creates a task called generateLogoDistanceField
    logo {

        inputFile = file('textures/logo.png')
        downscale = 8
        spread = 32
        outputFile = file('assets/logo-df.png')

    }

    // Creates a task called generateTitleDistanceField
    title {

        inputFile = file('textures/title.jpg')
        downscale = 4
        spread = 16
        color = 'ff0000'
        outputFile = file('assets/title-df.png')

    }

}
```

# PackTextures task

## Settings
Settings for Texture Packer are specified in a `settings { }` block. See the [LibGDX Wiki](https://github.com/libgdx/libgdx/wiki/Texture-packer#settings)
for a list of available settings, their default values and descriptions. To get a quick overview of the available settings you can run the
`texturePackerSettingsHelp` Gradle task.

For reference, these are the most important settings and their default values, as of LibGDX 1.9.8:

```groovy
settings {

    paddingX = 2
    paddingY = 2
    edgePadding = true
    duplicatePadding = false
    rotation = false
    minWidth = 16
    minHeight = 16
    maxWidth = 1024
    maxHeight = 1024
    square = false
    stripWhitespaceX = false
    stripWhitespaceY = false
    alphaThreshold = 0
    filterMin = "Nearest"
    filterMag = "Nearest"
    wrapX = "ClampToEdge"
    wrapY = "ClampToEdge"
    format = "RGBA8888"
    alias = true
    outputFormat = "png"
    jpegQuality = 0.9
    ignoreBlankImages = true
    fast = false
    debug = false
    combineSubdirectories = false
    flattenPaths = false
    premultiplyAlpha = false
    useIndexes = true
    bleed = true
    bleedIterations = 2
    limitMemory = true
    grid = false
    scale = [1]
    scaleSuffix = [""]
    scaleResampling = ["bicubic"]
    atlasExtension = ".atlas"

}
```

## Generating multiple texture packs
If you want to create multiple texture packs, you can use a `texturePacks { }` block.

The following example creates 3 tasks: pack**Game**Textures, pack**Menu**Textures and pack**GameOver**Textures:
```groovy
texturePacks {

    // Creates "game.atlas"
    game {
        from 'textures/game'
        into 'assets'
    }

    // Creates "menu.atlas"
    menu {
        from 'textures/menu'
        into 'assets'

        settings {
            filterMin = 'MipMapLinearNearest'
            filterMag = 'Nearest'
        }
    }

    gameOver {
        from 'textures/gameOver'
        into 'assets'
        // Name the pack "end.atlas" instead of the default "gameOver.atlas"
        packFileName = 'end.atlas'
    }

}
```

## Adding solid color textures
Sometimes you want a few simple solid color rectangular textures in your atlas, like a single white pixel. Instead of creating these yourself, you can have
the plugin generate them for you with the ```solid { }``` directive:

```groovy
packTextures {

  into 'assets/'
  from 'textures/'

  // Adds a single white pixel named "white"
  solid {
    name = "white"
  }

  // Adds a 3x4 red texture named "red"
  solid {
    name = "red"
    color = color("#ff0000")    // default: #ffffffff
    width = 3                   // default: 1
    height = 4                  // default: 1
  }

}
```

## Dependencies on BitmapFont or DistanceField task
If some of the textures which have to be packed are generated by a bitmap font task or distance field task, you have make sure these tasks are run
first, so that the input for the texture pack is available and up to date. You can do this by making the texture pack task depended on those other
tasks using Gradle's [dependsOn mechanism](https://docs.gradle.org/4.5.1/dsl/org.gradle.api.Task.html#N17778):

```groovy
bitmapFonts {

    roboto {
        // ...
    }

}

distanceFields {

    logo {
        // ...
    }

}

packTextures {

    from 'textures/'
    into 'assets/'

    dependsOn(generateRobotoFont, generateLogoDistanceField)

}
```

## Reusing settings
To reuse settings for multiple texture packs, you can define settings objects with `packSettings { }` (`packSettings` has to be
imported from `dsl.Utils`):

```groovy
import static com.github.blueboxware.gdxplugin.dsl.Utils.*

// Create base settings
def baseSettings = packSettings {
    filterMin = 'MipMapLinearNearest'
    filterMag = 'Nearest'
    maxWidth = 2048
    maxHeight = 2048
}

// Create settings for scaled texture packs, based on the base settings
def scaledPackSettings = packSettings(baseSettings) {
    scale = [1, 2]
    scaleSuffix = ["Normal", "Scaled"]
    scaleResampling = ["bicubic", "bicubic"]
}

texturePacks {

    game {
        from 'textures/game'
        into 'assets'
        settings = baseSettings
    }

    menu {
        from 'textures/menu'
        into 'assets'
        settings = scaledPackSettings
    }

    gameOver {
        from 'textures/gameOver'
        into 'assets'

        // Use packSettings, but change outputFormat to jpg
        settings = packSettings(baseSettings) {
            outputFormat = "jpg"
        }
    }
}
```

## Multiple input directories, filtering and renaming
Pack Textures tasks implement Gradle's [CopySpec](https://docs.gradle.org/current/javadoc/org/gradle/api/file/CopySpec.html), so you can specify
multiple input directories, and filter and rename files:

```groovy
packTextures {

    into 'assets'

    from('textures/ui') {
      exclude 'test*'
    }

    from('textures/menu') {
      include '*.png'
      rename('menu_(.*)', '$1')
    }

}
```

## Using "pack.json"
Normally any `pack.json` files in the input directories (and any subdirectories) are ignored. If you want to load the texture packer settings from a
pack.json file instead of defining them in the build file, you can use the `settingsFile` argument:

```groovy
packTextures {

  from 'textures/'
  into 'assets/'

  settingsFile = file('textures/pack.json')

}
```

If you want TexturePacker to use pack.json files found in the input directories and any subdirectories, set `usePackJson` to true:

```groovy
packTextures {

  from 'textures/'
  into 'assets/'

  usePackJson = true

}
```

Note that if you specify multiple input directories (see [above](#multiple-input-directories-filtering-and-renaming)), and more than one of the top level directories contain
pack.json files, only one of these is used. Use the `settingsFile` parameter to specify which one.

## Custom tasks
The plugin provides the `PackTextures` task type which can be used to create custom tasks:

```groovy
import com.github.blueboxware.gdxplugin.tasks.PackTextures

task('myPackTask', type: PackTextures) {

  description = 'Pack things'

  into 'assets'
  from 'textures'

  settings {
      atlasExtension = ".pack"
      filterMin = "MipMapLinearLinear"
  }

  doLast {
      println 'Done!'
  }

}
```

# BitmapFont task
## Input font and characters
The input font is specified by the `inputFont` parameter. It can be set to a File, which should be a TTF-file. It can also be set to a String, in which
case it should be the name of a system font which will be used to generate the bitmap font. If you don't specify an input font, the system font "Arial"
is used (assuming a font with that name is available).

The `characters` parameter is used to specify a string which contains all the characters to include in the font. The following predefined constants are available:

* `NEHE` (**DEFAULT**): "ABCDEFGHIJKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz1234567890"!`?'.,;:()[]{}<>|/@\^$-%+=#_&~*\u007f", the same as Hiero's NEHE character set
* `COMMON`: same as `NEHE`
* `ASCII`: all ASCII characters from ord(33) to ord(255) inclusive, the same as Hiero's ASCII character set
* `EXTENDED`: a larger collection of characters, the same as Hiero's Extended character set

The space and null characters are always added.

## Output font
Use the `outputFile` argument to specify the filename for the bitmap font. If you specify multiple sizes, the sizes will be appended to the specified
name (for example: `arial16px.fnt`, `arial32px.fnt`, etc.). If you don't specify an output file, the task name will be used and the font will be written
to the top level directory of the project.

You can also specify custom names for specific sizes:
```groovy
bitmapFonts {

    text {

        inputFont = file("fonts/roboto.ttf")

        // Default output name
        outputFile = "assets/textFont.fnt"

        // Create textFont16px.fnt and textFont24px.fnt
        sizes = [16, 24]

        // Also create a 32 px font in assets/big.fnt
        size 32, "assets/big.fnt"

        // And a 64 px font in assets/toobig.fnt
        size 64, file("assets/toobig.fnt")

    }

}
```

## Settings
The settings for the font are specified using a `settings { }` block. It supports the same settings as Hiero and has the same defaults, except that
the default `renderType` is _Java_ instead of _FreeType_. Here are the available settings with their defaults:

```groovy
settings {

    bold = false
    italic = false
    mono = false

    gamma = 1.8f

    paddingTop = 1
    paddingLeft = 1
    paddingBottom = 1
    paddingRight = 1

    paddingAdvanceX = -2
    paddingAdvanceY = -2

    glyphPageWidth = 512
    glyphPageHeight = 512

    // "Java", "FreeType" or "Native"
    renderType = "Java"

    // The effects to apply
    effects = [
       color {
            color = color("#ffffff")
       }
    ]

}
```

## Effects
The effects from Hiero are available, and a list of effects to apply can be used with the `effects` parameter of the settings to
specify which effects to use and their settings. The effects have the same parameters and defaults as in Hiero. Here are the
available effects and their defaults:

```groovy
color {
    color = color("#ffffff")
}

gradient {
    topColor = color("#00ffff")
    bottomColor = color("#0000ff")
    offset = 0
    scale = 1f
    cyclic = false
}

shadow {
    color = color("#000000")
    opacity = 0.6f
    xDistance = 2f
    yDistance = 2f
    blurKernelSize = 0
    blurPasses = 1
}

outline {
    width = 2f
    color = color("#000000")
    join = JoinBevel // JoinBevel, JoinMiter or JoinRound
    stroke = null // See java.awt.Stroke
}

wobble {
    width = 2f
    color = color("#000000")
    detail = 1f
    amplitude = 1f
}

zigzag {
    width = 2f
    color = color("#000000")
    amplitude = 1f
    wavelength = 3f
    join = JoinBevel // JoinBevel, JoinMiter or JoinRound
}

distanceField {
    color = color("#ffffff")
    scale = 1
    spread = 1f
}
```

# NinePatch task
## Arguments
The arguments for NinePatch tasks with their defaults:

```groovy
ninePatch {

    taskName {

        // Input image
        image = file('textures/rect.png')

        // By default the output image is put in the same directory as the input image
        // and has the same name but with the extension ".9.png"
        output = file('textures/rect.9.png')

        // Insets
        // These are the number of pixels from the left/right/top/bottom of the image where the center stretch region starts
        left = 0
        right = 0
        top = 0
        bottom = 0

        // Padding
        // Padding is only generated if you specify at least one of the below
        paddingLeft = same as left inset
        paddingRight = same as right inset
        paddingTop = same as top inset
        paddingBottom = same as bottom inset

        // When true, tries to determine the insets automatically. See next section.
        auto = false
        fuzziness = 0f
        centerX = image.width / 2
        centerY = image.height / 2

    }

}
```

## Automatic inset generation
If your image is fairly simple, you can have the plugin try to determine the insets automatically. Whether this will
actually work depends on the image.

To do this, specify `auto = true` and all insets you don't specify yourself will be automatically set.

When determining the insets, the plugin scans outward from the center of the image. If the center of the image is not
actually part of the center stretchable region, the results will be wrong. You can specify an alternative starting
location with the `centerX` and/or `centerY` arguments (the origin is top left).

The `fuzziness` parameter determines how strict or loose the rows and columns of the image are compared. It should be
a float between 0 (inclusive) and 100 (inclusive). 0 means: the center region should be completely even - any difference
in row or column will be taken as the border of the center region. 100 means: all differences will be ignored and the
center region will contain the entire image. The default is 0. A value between 50 and 70 often seems to work when the
center region has a color gradient.

# DistanceField task
## Arguments
The arguments for the distance field task:

* `inputFile`: The input file (type: File)
* `outputFile`: The output file (type: File, default: inputFileWithoutExtension + "-df." + outputFormat)
* `color`: The color of the output image (type: String, default: "ffffff")
* `downscale`: The downscale factor (type: int, default: 1)
* `spread`: The edge scan distance (type: float, default: 1.0)
* `outputFormat`: The output format (type: String, default: The extension of `outputFile`. If `outputFile` is not specified: "png")

# General
## LibGDX version
The plugin comes with a bundled version of LibGDX, which is used for packing etc. To see the LibGDX version used by the
plugin (this is not the version used by your project itself), run the `gdxVersion` task:

```dos
> gradlew.bat -q gdxVersion
1.9.11
```

If you want the plugin to use a different version, you can force this in the `buildscript` block. For example, to use version 1.9.5:

```groovy
buildscript {

    ext {
        gdxVersion = "1.9.5"
    }

    repositories {
        mavenCentral()
        // ... other repositories
    }

    dependencies {
        // ... other dependencies
        classpath("com.badlogicgames.gdx:gdx-tools:$gdxVersion") {
            force = true
        }
        classpath("com.badlogicgames.gdx:gdx-backend-lwjgl:$gdxVersion") {
            force = true
        }
        classpath("com.badlogicgames.gdx:gdx-platform:$gdxVersion") {
            force = true
        }
    }

}
```

Use the `gdxVersion` task again to check:
```dos
> gradlew.bat -q gdxVersion
1.9.5 (default: 1.9.11)
```

# Changelog

## 1.3
* Update to LibGDX 1.9.11
* Display informative error when trying to create a jpeg with alpha when using OpenJDK

## 1.2.2
* Fix BitmapFont task

## 1.2.1
* Fix backward compatibility, down to Gradle 3.5

## 1.2
* Added NinePatch task
* It's no longer necessary to add custom tasks as dependencies to the build task

## 1.1.2
* Added `solid` directive to texture pack tasks to add simple solid colored textures

## 1.1.1
* Fix BitmapFont task

## 1.1
* Added task for creating Bitmap Fonts
* Added `createAllAssets` task which runs all the tasks of the plugin

## 1.0.1
* Added `createAllTexturePacks` task which runs all texture pack tasks
* Added `createAllDistanceFields` task which runs all distance fields tasks
* Use `packSettings()` instead of `PackTextures.createSettings()` to create texture packer settings objects
* Made the plugin more Gradle Kotlin DSL-friendly. See [README-kotlin.md](README-kotlin.md)
