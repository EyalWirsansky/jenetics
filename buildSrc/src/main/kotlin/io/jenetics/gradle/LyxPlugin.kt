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

import Env
import Env.BUILD_DATE
import Jenetics
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include
import org.gradle.kotlin.dsl.filter
import org.gradle.kotlin.dsl.register
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Plugin which adds a build task for creating a PDF file from the lyx sources.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since 1.5
 * @version 6.1
 */
open class LyxPlugin : Plugin<Project> {

	override fun apply(project: Project) {
		val build: Task = if (project.tasks.findByPath(BUILD) != null) {
			project.tasks.getByPath(BUILD)
		} else {
			project.tasks.register(BUILD).get()
		}
		build.dependsOn(LYX)



		project.tasks.register<Copy>(PREPARE_PDF_GENERATION) {

			// copy the manual with text replacements:
			from("${project.projectDir}/src/main") {
				include("lyx/manual.lyx").
				filter(
					ReplaceTokens::class, "tokens" to mapOf(
					"__version__" to project.version,
					"__minor_version__" to Version.parse(project.version).minorVersionString(),
					"__identifier__" to "${Jenetics.VERSION}-$BUILD_DATE",
					"__year__" to Env.COPYRIGHT_YEAR
				)
				)
			}

			// copy the rest of the files without changes:
			from("${project.projectDir}/src/main") {
				exclude("lyx/manual.lyx")
			}

			into(build.temporaryDir)
		}

		project.tasks.register<Lyx2PDFTask>("lyx") {
			val file = project.layout.buildDirectory.file("tmp/build/lyx/manual.lyx")
			document.set(file)
			pdfDir.set(project.layout.buildDirectory.dir("lyx_output/pdf"))

			dependsOn(PREPARE_PDF_GENERATION)
			doLast {
				project.copy {
					from("${build.temporaryDir}/lyx/manual.pdf")
					into("${project.buildDir}/doc")
					rename { name ->
						name.replace("manual.pdf", "manual-${project.version}.pdf")
					}
				}
			}
		}

		if (project.tasks.findByPath(CLEAN) == null) {
			project.tasks.getByPath(CLEAN).doLast {
				project.buildDir.deleteRecursively()
			}
		}
	}

	companion object {
		private const val BUILD = "build"
		private const val CLEAN = "clean"
		private const val LYX = "lyx"
		private const val PREPARE_PDF_GENERATION = "preparePDFGeneration"

		private val BUILD_DATE = DateTimeFormatter
			.ofPattern("yyyy/MM/dd")
			.format(LocalDate.now());
	}
}
