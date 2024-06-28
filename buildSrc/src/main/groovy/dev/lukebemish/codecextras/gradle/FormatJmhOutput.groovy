package dev.lukebemish.codecextras.gradle

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class FormatJmhOutput extends DefaultTask {
	@InputFile
	abstract RegularFileProperty getJmhResults()

	@OutputFile
	abstract RegularFileProperty getFormattedResults()

	@TaskAction
	void format() {
		// Load JMH JSON results
		List json = new JsonSlurper().parse(jmhResults.get().getAsFile())
		List lines = [
		    "| Benchmark | Mode | Count | Score | | Error | Units |",
			"| --- | --- | --- | ---: | :---: | :--- | --- |"
		]
		for (def result in json) {
			String benchmark = result.benchmark
			benchmark = benchmark.split(/\./).dropWhile { it.capitalize() != it}.join('.')
			String mode = result.mode
			int count = result.forks * result.measurementIterations
			double score = result.primaryMetric.score
			double error = result.primaryMetric.scoreError
			String units = result.primaryMetric.scoreUnit
			lines << ("| $benchmark | $mode | $count | ${String.format('%.3f', score)} | ± | ${String.format('%.3f', error)} | $units |" as String)
		}
		formattedResults.get().asFile.write(lines.join('\n') + '\n')
	}
}
