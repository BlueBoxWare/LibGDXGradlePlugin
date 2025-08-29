package com.github.blueboxware.gdxplugin.dsl

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class NinePatchConfiguration @Inject constructor(@Input var name: String, objectFactory: ObjectFactory) {

  @get:Input
  @get:Optional
  abstract val left: Property<Int>

  @get:Input
  @get:Optional
  abstract val right: Property<Int>

  @get:Input
  @get:Optional
  abstract val top: Property<Int>

  @get:Input
  @get:Optional
  abstract val bottom: Property<Int>

  @get:Input
  @get:Optional
  abstract val paddingLeft: Property<Int>

  @get:Input
  @get:Optional
  abstract val paddingRight: Property<Int>

  @get:Input
  @get:Optional
  abstract val paddingTop: Property<Int>

  @get:Input
  @get:Optional
  abstract val paddingBottom: Property<Int>

  @get:Input
  @get:Optional
  val auto: Property<Boolean> = objectFactory.property<Boolean>().convention(false)

  @get:Input
  @get:Optional
  val edgeDetect: Property<Boolean> = objectFactory.property<Boolean>().convention(false)

  @get:Input
  @get:Optional
  val fuzziness: Property<Float> = objectFactory.property<Float>().convention(0f)

  @get:Input
  @get:Optional
  abstract val centerX: Property<Int>

  @get:Input
  @get:Optional
  abstract val centerY: Property<Int>

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val image: RegularFileProperty

  @get:Internal
  abstract val output: RegularFileProperty

}