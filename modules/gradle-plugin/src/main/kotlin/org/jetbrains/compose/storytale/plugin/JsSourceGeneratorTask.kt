package org.jetbrains.compose.storytale.plugin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.incremental.createDirectory

@CacheableTask
open class JsSourceGeneratorTask : DefaultTask() {

    @Input
    lateinit var title: String

    @OutputDirectory
    lateinit var outputResourcesDir: File

    @OutputDirectory
    lateinit var outputSourcesDir: File

    @TaskAction
    fun generate() {
        cleanup(outputSourcesDir)
        cleanup(outputResourcesDir)

        generateSources()
        generateResources()
    }

    private fun generateSources() {
        val optInExperimentalComposeUi = AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
            "androidx.compose.ui.ExperimentalComposeUiApi::class",
        ).build()

        val file = FileSpec.builder(StorytaleGradlePlugin.STORYTALE_PACKAGE, "Main").apply {
            addImport("androidx.compose.ui.window", "ComposeViewport")
            addImport("kotlinx.browser", "document")
            addImport("org.jetbrains.skiko.wasm", "onWasmReady")
            addImport("org.jetbrains.compose.storytale.gallery", "Gallery")

            function("MainViewController") {
                addAnnotation(optInExperimentalComposeUi)
                addStatement(
                    """
          |onWasmReady {
          |   ComposeViewport(document.body!!) {
          |      Gallery()    
          |   }
          |}
                    """.trimMargin(),
                )
            }

            function("main") {
                addStatement("MainViewController()")
            }
        }.build()

        file.writeTo(outputSourcesDir)
    }

    private fun generateResources() {
        if (!outputResourcesDir.exists()) {
            outputResourcesDir.createDirectory()
        }

        val index = File(outputResourcesDir, "index.html")
        index.writeText(
            """
      |<!DOCTYPE html>
      |<html lang="en">
      |  <head>
      |    <meta charset="UTF-8">
      |    <title>Gallery</title>
      |    <script type="application/javascript" src="skiko.js"></script>
      |    <script type="application/javascript" src="$SCRIPT_FILE_NAME"></script>
      |  </head>
      |  <body style="height: 100vh; width: 100vw;">
      |  </body>
      |</html>   
      | 
            """.trimMargin(),
        )
    }

    companion object {
        internal const val SCRIPT_FILE_NAME = "composeApp.js"
    }
}
