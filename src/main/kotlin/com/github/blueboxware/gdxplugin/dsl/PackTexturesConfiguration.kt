package com.github.blueboxware.gdxplugin.dsl

import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.github.blueboxware.gdxplugin.configure
import com.github.blueboxware.gdxplugin.getDestinationDirectory
import groovy.lang.Closure
import org.gradle.api.file.*
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.CopySpecResolver
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.util.GradleVersion
import java.io.File
import javax.inject.Inject

open class PackTexturesConfiguration @Inject constructor(
  @Input var name: String,
  @Internal val copySpec: DefaultCopySpec,
  private val providerFactory: ProviderFactory,
  private val projectLayout: ProjectLayout,
  objectFactory: ObjectFactory
) : CopySpecInternal by copySpec {

  @get:Input
  val packFileName: Property<String> = objectFactory.property<String>().convention(name)

  @get:Input
  val usePackJson: Property<Boolean> = objectFactory.property<Boolean>().convention(false)

  @get:InputFile
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  val settingsFile: RegularFileProperty = objectFactory.fileProperty()

  @get:Nested
  val settings: Property<TexturePacker.Settings> = objectFactory.property<TexturePacker.Settings>().convention(
    TexturePacker.Settings()
  )

  @get:Nested
  val solidSpecs: ListProperty<SolidColorSpec> =
    objectFactory.listProperty<SolidColorSpec>().convention(listOf())

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @IgnoreEmptyDirectories
  val inputFiles: ConfigurableFileCollection = objectFactory.fileCollection()

  @get:Input
  @Optional
  val properties: MapProperty<String, Provider<Any>> = objectFactory.mapProperty<String, Provider<Any>>()

  @get:Input
  val hasCustomActions: Provider<Boolean> = providerFactory.provider(::hasCustomActions)

  fun settings(closure: Closure<in TexturePacker.Settings>) {
    settings.set(TexturePacker.Settings().configure(closure))
  }

  fun settings(closure: TexturePacker.Settings.() -> Unit) {
    settings.set(TexturePacker.Settings().apply(closure))
  }

  @Suppress("unused")
  fun solid(closure: Closure<in SolidColorSpec>) {
    solidSpecs.add(SolidColorSpec().configure(closure))
  }

  @Suppress("unused")
  fun solid(closure: SolidColorSpec.() -> Unit) {
    solidSpecs.add(SolidColorSpec().apply(closure))
  }

  init {

    addChildSpecListener { path, spec ->
      val propertyNameBuilder = StringBuilder("rootSpec")
      val parentResolver = path.unroll(propertyNameBuilder)
      val resolver = spec.buildResolverRelativeToParent(parentResolver)
      val specPropertyName = propertyNameBuilder.toString()

      addInputs(specPropertyName, spec, resolver)
    }

    addInputs("", copySpec, copySpec.buildRootResolver())
  }

  private fun addInputs(path: String, copySpec: CopySpec, resolver: CopySpecResolver) {
    inputFiles.from(resolver::getSource)
    properties.put("$path.destPath", providerFactory.provider { resolver.destPath.pathString })
    properties.put("$path.caseSensitive", providerFactory.provider(copySpec::isCaseSensitive))
    properties.put("$path.includeEmptyDirs", providerFactory.provider(copySpec::getIncludeEmptyDirs))
    properties.put("$path.duplicatesStrategy", providerFactory.provider(copySpec::getDuplicatesStrategy))
    properties.put("$path.dirPermissions", copySpec.dirPermissions.map(FilePermissions::toUnixNumeric))
    properties.put("$path.filePermissions", copySpec.filePermissions.map(FilePermissions::toUnixNumeric))
    properties.put("$path.filteringCharset", providerFactory.provider(copySpec::getFilteringCharset))
  }

  @Internal
  override fun isCaseSensitive(): Boolean = copySpec.isCaseSensitive

  @Internal
  override fun getDirMode(): Int? =
    if (GradleVersion.current() < GradleVersion.version("9.0")) copySpec.dirMode else null

  @Internal
  override fun getFilePermissions(): Property<ConfigurableFilePermissions?> = copySpec.filePermissions

  @Internal
  override fun getDirPermissions(): Property<ConfigurableFilePermissions?> = copySpec.dirPermissions

  @Internal
  override fun getDuplicatesStrategy(): DuplicatesStrategy = copySpec.duplicatesStrategy

  @Input
  @Optional
  override fun getExcludes(): Set<String?> = copySpec.excludes

  @Input
  @Optional
  override fun getIncludes(): Set<String?> = copySpec.includes

  @Internal
  override fun getFileMode(): Int? =
    if (GradleVersion.current() < GradleVersion.version("9.0")) copySpec.fileMode else null

  @Internal
  override fun getFilteringCharset(): String = copySpec.filteringCharset

  @Internal
  override fun getIncludeEmptyDirs(): Boolean = copySpec.includeEmptyDirs

  @Internal
  override fun getChildren(): Iterable<CopySpecInternal?> = copySpec.children

  @Internal
  override fun getDestinationDir(): File? {
    val file = copySpec.getDestinationDirectory()
    return if (file?.isAbsolute == true) {
      file
    } else {
      File(projectLayout.projectDirectory.asFile.absolutePath, file?.path ?: "")
    }
  }
}