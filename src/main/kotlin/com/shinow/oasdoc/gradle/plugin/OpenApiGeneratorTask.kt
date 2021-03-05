package com.shinow.oasdoc.gradle.plugin

import khttp.responses.Response
import org.awaitility.Durations
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.ConnectException
import java.time.Duration
import java.time.temporal.ChronoUnit.SECONDS


open class OpenApiGeneratorTask : DefaultTask() {
    @get:Input
    val apiDocsUrl: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val outputFileName: Property<String> = project.objects.property(String::class.java)

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    private val waitTimeInSeconds: Property<Int> = project.objects.property(Int::class.java)

    private val yapiOrigin: Property<String> = project.objects.property(String::class.java)

    private val yapiProjectToken: Property<String> = project.objects.property(String::class.java)

    init {
        description = OPEN_API_TASK_DESCRIPTION
        group = GROUP_NAME
        // load my extensions
        val extension: OpenApiExtension = project.extensions.run {
            getByName(EXTENSION_NAME) as OpenApiExtension
        }

        // set a default value if not provided
        val defaultOutputDir = project.objects.directoryProperty()
        defaultOutputDir.set(project.buildDir)

        apiDocsUrl.set(extension.apiDocsUrl.getOrElse(DEFAULT_API_DOCS_URL))
        outputFileName.set(extension.outputFileName.getOrElse(DEFAULT_OPEN_API_FILE_NAME))
        outputDir.set(extension.outputDir.getOrElse(defaultOutputDir.get()))
        waitTimeInSeconds.set(extension.waitTimeInSeconds.getOrElse(DEFAULT_WAIT_TIME_IN_SECONDS))

        yapiOrigin.set(
            if (extension.yapiOrigin.get().endsWith("/"))
                extension.yapiOrigin.get().substring(0, extension.yapiOrigin.get().length - 1)
            else
                extension.yapiOrigin.get()
        )
        yapiProjectToken.set(extension.yapiProjectToken.get())
        if ((yapiOrigin.isPresent && !yapiProjectToken.isPresent) || (!yapiOrigin.isPresent && yapiProjectToken.isPresent)) {
            throw GradleException("Uploading OpenApi document requires both \"yapiOrigin\" and \"yapiProjectoToken\" parameters.")
        }
    }

    @TaskAction
    fun execute() {
        val apiDocs: String
        try {
            await ignoreException ConnectException::class withPollInterval Durations.ONE_SECOND atMost Duration.of(
                waitTimeInSeconds.get().toLong(),
                SECONDS
            ) until {
                val statusCode = khttp.get(apiDocsUrl.get()).statusCode
                logger.trace("apiDocsUrl = {} status code = {}", apiDocsUrl.get(), statusCode)
                statusCode < 299
            }
            logger.info("Generating OpenApi Docs..")
            val response: Response = khttp.get(apiDocsUrl.get())

            val isYaml = apiDocsUrl.get().toLowerCase().contains(".yaml")
            apiDocs = if (isYaml) response.text else response.jsonObject.toString()

            val outputFile = outputDir.file(outputFileName.get()).get().asFile
            outputFile.writeText(apiDocs)
        } catch (e: ConditionTimeoutException) {
            this.logger.error(
                "Unable to connect to ${apiDocsUrl.get()} waited for ${waitTimeInSeconds.get()} seconds",
                e
            )
            throw GradleException("Unable to connect to ${apiDocsUrl.get()} waited for ${waitTimeInSeconds.get()} seconds")
        }

        if (yapiOrigin.isPresent && yapiProjectToken.isPresent && apiDocs.isNotEmpty()) {
            uploadToYapi(apiDocs)
        }
    }

    private fun uploadToYapi(apiDocs: String) {
        val response = khttp.post(
            url = "$yapiOrigin/api/open/import_data",
            data = mapOf(
                "type" to "swagger",
                "merge" to "merge",
                "token" to yapiProjectToken,
                "json" to apiDocs
            )
        )
        val responseJson = response.jsonObject
        if (responseJson["errcode"] == 0) {
            logger.info(responseJson["errmsg"].toString())
        } else {
            throw GradleException(responseJson.toString())
        }
    }
}
