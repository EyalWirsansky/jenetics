/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package io.jenetics.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

/**
 * This tasks converts a lyx document into a PDF.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since 1.4
 * @version 6.1
 */
abstract class Lyx2PDFTask : DefaultTask() {

	@get:InputFile
	abstract val document: RegularFileProperty

	@get:OutputDirectory
	abstract val pdfDir: DirectoryProperty

	@TaskAction
	fun lyx2PDF() {
		if (lyxExists()) {
			convert()
		} else {
			logger.lifecycle("Binary '{}' not found.", BINARY)
			logger.lifecycle("Manual PDF has not been created.")
			createEmptyFile()
		}
	}

	private fun createEmptyFile() {
		pdfDir.get().file("empty.pdf").asFile.writeText("foo")
	}

	private fun convert() {
		val documentFile = document.get().asFile
		val workingDir = documentFile.parentFile
		val documentName = documentFile.name

		val pdfDir = pdfDir.get().getAsFile().absolutePath
		val pdfDocName = "${documentFile.nameWithoutExtension}.pdf"

		val builder = ProcessBuilder(
			//BINARY, "-e", "pdf2", documentName
			BINARY, "${workingDir}/${documentName}", "${pdfDir}/${pdfDocName}"
		)
		builder.directory(workingDir)
		builder.redirectErrorStream(true)
		logger.debug("${workingDir}/${documentName}")

		try {
			this.pdfDir.get().getAsFile().mkdir() // create the PDF directory

			val process = builder.start()
			output(process.inputStream)
			val exitValue = process.waitFor()

			if (exitValue != 0) {
				logger.lifecycle("Error while generating PDF.")
				logger.lifecycle("Manual PDF has not been created.")
			}
		} catch (e: Exception) {
			throw TaskExecutionException(this, e)
		}
	}

	private fun output(input: InputStream) {
		val d = BufferedReader(InputStreamReader(input))
		var line: String? = d.readLine()
		while (line != null) {
			logger.info(line)
			line = d.readLine()
		}
	}


	companion object {
		private const val BINARY = "cp" //"lyx"

		private fun lyxExists(): Boolean {
			//val builder = ProcessBuilder(BINARY, "-version")
			val builder = ProcessBuilder(BINARY, "--help")

			return try {
				val process = builder.start();
				process.waitFor() == 0;
			} catch (e: IOException) {
				false
			} catch (e: InterruptedException) {
				Thread.currentThread().interrupt()
				return false
			}
		}
	}

}
