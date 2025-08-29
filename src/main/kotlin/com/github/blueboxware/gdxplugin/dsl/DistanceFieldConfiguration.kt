package com.github.blueboxware.gdxplugin.dsl

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class DistanceFieldConfiguration @Inject constructor(@Input var name: String, objectFactory: ObjectFactory) {

  @get:Input
  val color: Property<String> = objectFactory.property<String>().convention("ffffff")

  @get:Input
  @get:Optional
  abstract val outputFormat: Property<String>

  @get:Input
  @get:Optional
  val downscale: Property<Int> = objectFactory.property<Int>().convention(1)

  @get:Input
  @get:Optional
  val spread: Property<Float> = objectFactory.property<Float>().convention(1f)

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputFile: RegularFileProperty

  @get:Internal
  abstract val outputFile: RegularFileProperty

}
