package com.simpletools.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.xml.parsers.DocumentBuilderFactory

/** Generates Android resources derived from `simpletools.properties` (currently the brand name). */
@CacheableTask
abstract class GenerateAppConfigResourcesTask : DefaultTask() {

    @get:Input
    abstract val appName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val valuesDir = outputDirectory.get().dir("values").asFile
        valuesDir.deleteRecursively()
        valuesDir.mkdirs()
        valuesDir.resolve("simpletools_config.xml").writeText(
            """
            |<?xml version="1.0" encoding="utf-8"?>
            |<!-- Generated from simpletools.properties. Do not edit. -->
            |<resources>
            |    <string name="app_name" translatable="false">${escapeAndroidString(appName.get())}</string>
            |</resources>
            |""".trimMargin(),
        )
    }

    private fun escapeAndroidString(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
}

/** Fails the build when the merged manifest requests permissions not declared in `simpletools.properties`. */
@CacheableTask
abstract class VerifyPermissionsTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val declaredPermissions: ListProperty<String>

    @get:Input
    abstract val applicationId: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(mergedManifest.get().asFile)
        val requested = listOf("uses-permission", "uses-permission-sdk-23").flatMap { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).mapNotNull { index ->
                nodes.item(index).attributes
                    ?.getNamedItemNS("http://schemas.android.com/apk/res/android", "name")
                    ?.nodeValue
            }
        }
        val result = PermissionAudit.audit(requested, declaredPermissions.get(), applicationId.get())
        report.get().asFile.writeText(
            "requested=${requested.sorted().joinToString()}\nundeclared=${result.undeclared}\nunused=${result.unused}\n",
        )
        if (!result.isClean) throw GradleException(PermissionAudit.describe(result))
    }
}
