# GdxPlugin


GdxPlugin is a Gradle plugin that adds three [LibGDX](https://libgdx.badlogicgames.com/) related tasks to:

* create Texture Packs (a.k.a. Texture Atlases) using [TexturePacker](https://github.com/libgdx/libgdx/wiki/Texture-packer) 
* create Bitmap Fonts using [Hiero](https://github.com/libgdx/libgdx/wiki/Hiero)
* create Distance Fields from single images using [DistanceFieldGenerator](https://github.com/libgdx/libgdx/wiki/Distance-field-fonts#using-distance-fields-for-arbitrary-images)

**This plugin requires Gradle 3.0 or higher**

# Table of Contents

- [Getting started](#getting-started)
  - [Add the plugin](#add-the-plugin)
  - [Packing textures](#packing-textures)
  - [Creating a Bitmap Font](#creating-a-bitmap-font)
  - [Creating distance fields](#creating-distance-fields)
- [PackTextures task](#packtextures-task)
  - [Settings](#settings)
  - [Generating multiple texture packs](#generating-multiple-texture-packs)
  - [Reusing settings](#reusing-settings)
  - [Multiple input directories, filtering and renaming](#multiple-input-directories-filtering-and-renaming)
  - [Using "pack.json"](#using-packjson)
  - [Custom tasks](#custom-tasks)
- [BitmapFont task](#bitmapfont-task)
  - [Input font and characters](#input-font-and-characters)
  - [Output font](#output-font)
  - [Settings](#settings-1)
  - [Effects](#effects)
- [DistanceField task](#distancefield-task)
  - [Arguments](#arguments)
  - [DistanceField and PackTextures](#distancefield-and-packtextures)
- [General](#general)
  - [LibGDX version](#libgdx-version)
- [Changelog](#changelog)
  - [1.1](#11)
  - [1.0.1](#101)


# Getting started
## Add the plugin
Add the plugin to your project:

```kotlin
plugins {
    id("com.github.blueboxware.gdx") version "1.1.1"
}
```

## Packing textures
Creating a packTextures task:

```kotlin
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import com.github.blueboxware.gdxplugin.dsl.*


val packTextures: PackTextures by tasks

packTextures.apply {

    // The directory which contains the images to pack
    from("textures/")
    
    // The target directory: 'pack.atlas' is placed in this directory
    into("assets/")
    
    settings {
        // Settings for TexturePacker
        filterMin = MipMapLinearNearest
        filterMag = MipMap
    }
    
}
```

Run the task (or just do a build):

```dos
gradlew.bat packTextures
```

## Creating a Bitmap Font
To create a bitmap font:

```kotlin
import com.github.blueboxware.gdxplugin.tasks.BitmapFont

val bitmapFonts: NamedDomainObjectContainer<BitmapFont> by extensions

bitmapFonts.invoke {

    // We name the font 'text': this creates a task called 'generateTextFont'
    "text" {
		
        inputFont = file("fonts/roboto.ttf")
        
        outputFile = file("assets/textFont.fnt")
        
        // Create the font in 2 sizes. The created fonts will be put in "textFont32px.fnt" and "textFont64px.fnt"
        sizes(32, 48)
        
        // The settings to use for the fonts
        settings {
        
            bold = true
            
            // The effects to apply
            effects = listOf(
                color {
                    color = color("#ff0000")
                },
                shadow {
                    opacity = 0.4f
                    xDistance = 4f
                    yDistance = 4f
                }
            )
            
        }
    
    }
    
}
```

Run the task (or just do a build):

```dos
gradlew.bat generateTextFont
```

## Creating distance fields
To create distance fields from single images:

```kotlin
import com.github.blueboxware.gdxplugin.tasks.DistanceField


val distanceFields: NamedDomainObjectContainer<DistanceField> by extensions

distanceFields.invoke {

    // Creates a task called generateLogoDistanceField
    "logo" {
    
        inputFile = file("textures/logo.png")
        downscale = 8
        spread = 32f
        outputFile = file("assets/logo-df.png")
        
    }
    
    // Creates a task called generateTitleDistanceField
    "title" {
    
        inputFile = file("textures/title.jpg")
        downscale = 4
        spread = 16f
        color = "ff0000"
        outputFile = file("assets/title-df.png")
    
    }

}

```

# PackTextures task

## Settings
Settings for Texture Packer are specified in a `settings { }` block. See the [LibGDX Wiki](https://github.com/libgdx/libgdx/wiki/Texture-packer#settings) 
for a list of available settings, their default values and descriptions. To get a quick overview of the available settings you can run the 
`texturePackerSettingsHelp` Gradle task.

For reference, these are the most important settings and their default values, as of LibGDX 1.9.8: 

```kotlin
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
    filterMin = Nearest
    filterMag = Nearest
    wrapX = ClampToEdge
    wrapY = ClampToEdge
    format = RGBA8888
    alias = true
    outputFormat = "png"
    jpegQuality = 0.9f
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
    scale = floatArrayOf(1f)
    scaleSuffix = arrayOf("")
    scaleResampling = arrayOf(Resampling.Bicubic)
    atlasExtension = ".atlas"

}
```

## Generating multiple texture packs
If you want to create multiple texture packs, you can use a `texturePacks { }` block.

The following example creates 3 tasks: pack**Game**Textures, pack**Menu**Textures and pack**GameOver**Textures:
```kotlin
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import com.github.blueboxware.gdxplugin.dsl.*


val texturePacks: NamedDomainObjectContainer<PackTextures> by extensions

texturePacks.invoke {

    // Creates "game.atlas"
    "game" {
        from("textures/game")
        into("assets")
    }

    // Creates "menu.atlas"
    "menu" {
        from("textures/menu")
        into("assets")
        
        settings {
            filterMin = MipMapLinearNearest
            filterMag = Nearest
        }
    }

    "gameOver" {
        from("textures/gameOver")
        into("assets")
        // Name the pack "end.atlas" instead of the default "gameOver.atlas"
        packFileName = "end.atlas"  
    }

}

```

## Reusing settings
To reuse settings for multiple texture packs, you can define settings objects with `packSettings { }`:

```kotlin
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import com.github.blueboxware.gdxplugin.dsl.*


// Create base settings
val baseSettings = packSettings {
    filterMin = MipMapLinearNearest
    filterMag = Nearest
    maxWidth = 2048
    maxHeight = 2048
}

// Create settings for scaled texture packs based on the base settings
val scaledPackSettings = packSettings(baseSettings) { 
    scale = floatArrayOf(1f, 2f)
    scaleSuffix = arrayOf("Normal", "Scaled")
    scaleResampling = arrayOf(Resampling.Bicubic, Resampling.Bicubic)
}

val texturePacks: NamedDomainObjectContainer<PackTextures> by extensions

texturePacks.invoke {

  "game" {
        from("textures/game")
        into("assets")
        settings = baseSettings
    }

    "menu" {
        from("textures/menu")
        into("assets")
        settings = scaledPackSettings
    }

    "gameOver" {
        from("textures/gameOver")
        into("assets")
        
        // Use baseSettings, but change outputFormat to jpg
        settings = packSettings(baseSettings) { 
            outputFormat = "jpg"
        }
    }

}

```

## Multiple input directories, filtering and renaming
Pack Textures tasks implement Gradle's [CopySpec](https://docs.gradle.org/current/javadoc/org/gradle/api/file/CopySpec.html), so you can specify
multiple input directories, and filter and rename files:

```kotlin
packTextures.apply {

    into("assets")
    
    from("textures/ui") {
      exclude("test*")
    } 
    
    from("textures/menu") {
      include("*.png")
      rename("menu_(.*)", """$1""")
    }

}

```

## Using "pack.json"
Normally any `pack.json` files in the input directories (and any subdirectories) are ignored. If you want to load the texture packer settings from a 
pack.json file instead of defining them in the build file, you can use the `settingsFile` argument:

```kotlin
packTextures.apply {

  from("textures/")
  into("assets/")
  
  settingsFile = file("textures/pack.json")

}
```

If you want TexturePacker to use pack.json files found in the input directories and any subdirectories, set `usePackJson` to true:

 
```kotlin
packTextures.apply {

  from("textures/")
  into("assets/")
  
  usePackJson = true

}
```

Note that if you specify multiple input directories (see [above](#multiple-input-directories-filtering-and-renaming)), and more than one of the top level directories contain
pack.json files, only one of these is used. Use the `settingsFile` parameter to specify which one.

## Custom tasks
The plugin provides the `PackTextures` task type which can be used to create custom tasks:

```kotlin
import com.github.blueboxware.gdxplugin.tasks.PackTextures
import com.github.blueboxware.gdxplugin.dsl.*


task<PackTextures>("myPackTask") {

  description = "Pack things"

  into("assets")
  from("textures")

  settings {
    atlasExtension = ".pack"
    filterMin = MipMapLinearLinear
  }

  doLast { 
    println("Done!")
  }

}.let {
  tasks.findByName("build")?.dependsOn(it)    
}

```

Note that we added `myPackTask` to the dependencies of the `build` task so that myPackTask is automatically run when building the project. This is not necessary for the plugins builtin tasks (like `packTextures`):
they are automatically added to the build. 

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
```kotlin
val bitmapFonts: NamedDomainObjectContainer<BitmapFont> by extensions

bitmapFonts.invoke {

    "text" {
		
        inputFont = file("fonts/roboto.ttf")
        
        // Default output name
        outputFile = "assets/textFont.fnt"
        
        // Create textFont16px.fnt and textFont24px.fnt
        sizes(16, 24)
        
        // Also create a 32 px font in assets/big.fnt
        size(32, "assets/big.fnt")
        
        // And a 64 px font in assets/toobig.fnt
        size(64, file("assets/toobig.fnt"))
    
    }
    
}
```

## Settings
The settings for the font are specified using a `settings { }` block. It supports the same settings as Hiero and has the same defaults, except that
the default `renderType` is _Java_ instead of _FreeType_. Here are the available settings with their defaults:

```kotlin
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
    renderType = Java
    
    // The effects to apply
    effects = listOf(
       color {
            color = color("#ffffff")
       }
    )
    
}
```

## Effects
The effects from Hiero are available, and a list of effects to apply can be used with the `effects` parameter of the settings to
specify which effects to use and their settings. The effects have the same parameters and defaults as in Hiero. Here are the
available effects and their defaults:

```kotlin
color {
    color = color("#ffffff")
}

gradient {
     topColor = color("#00ffff")
     bottomColor = color("#0000ff")
     offset = 0
     scale = 1f
     setCyclic(false)
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

# DistanceField task
## Arguments
The arguments for the distance field task:

* `inputFile`: The input file (type: File)
* `outputFile`: The output file (type: File, default: inputFileWithoutExtension + "-df." + outputFormat) 
* `color`: The color of the output image (type: String, default: "ffffff")
* `downscale`: The downscale factor (type: int, default: 1)
* `spread`: The edge scan distance (type: float, default: 1.0)
* `outputFormat`: The output format (type: String, default: The extension of `outputFile`. If `outputFile` is not specified: "png")

## DistanceField and PackTextures
If the distance fields you create should be packed by one or more pack tasks, you can add the relevant distance field tasks to the dependencies
of the pack tasks, to make sure the distance fields are available and up to date when the pack task runs. You can do this using Gradle's [dependsOn mechanism](https://docs.gradle.org/4.5.1/dsl/org.gradle.api.Task.html#N17778):

```kotlin
val distanceFields: NamedDomainObjectContainer<DistanceField> by extensions

distanceFields.invoke {

    "logo" {
    
        inputFile = file("textures/logo.png")
        outputFile = file("textures/logo-df.png")
        
    }
    
    "title" {
    
        inputFile = file("textures/title.png")
        outputFile = file("textures/title-df.png")
    
    }

}

val packTextures: PackTextures by tasks

packTextures.apply {

  from("textures/")
  into("assets/")
  
  dependsOn("generateLogoDistanceField", "generateTitleDistanceField")
}
```

# General
## LibGDX version
The plugin comes with a bundled version of LibGDX, which is used for packing etc. To see the LibGDX version used by the 
plugin (this is not the version used by your project itself), run the `gdxVersion` task:

```dos
> gradlew.bat -q gdxVersion
1.9.8
```

If you want the plugin to use a different version, you can force this in the `buildscript` block. For example, to use version 1.9.5:

```kotlin
buildscript {

    val gdxVersion = "1.9.5"

    repositories {
        mavenCentral()
        // ... other repositories
    }


    configurations.all {
        resolutionStrategy {
            force("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
        }
        resolutionStrategy {
            force("com.badlogicgames.gdx:gdx-backend-lwjgl:$gdxVersion")
        }
        resolutionStrategy {
            force("com.badlogicgames.gdx:gdx-platform:$gdxVersion")
        }
    }
}

```

Use the `gdxVersion` task again to check:
```dos
> gradlew.bat -q gdxVersion
1.9.5 (default: 1.9.8)
```
 
# Changelog

## 1.1
* Added task for creating Bitmap Fonts
* Added `createAllAssets` task which runs all pack, font and distance field tasks

## 1.0.1
* Added `createAllTexturePacks` task which runs all texture pack tasks
* Added `createAllDistanceFields` task which runs all distance fields tasks
* Use `packSettings()` instead of `PackTextures.createSettings()` to create texture packer settings objects
* Made the plugin more Gradle Kotlin DSL-friendly. 