package com.tencent.bkrepo.analysis.executor.dependencycheck

import org.junit.jupiter.api.TestInstance
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Import(
    DependencyScanExecutor::class
)
@TestPropertySource(locations = ["classpath:bootstrap-ut.properties"])
@ComponentScan("com.tencent.bkrepo.scanner.executor")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DependencyScanExecutorTest(
    private val dependencyScanExecutor: DependencyScanExecutor
) {

//    @Test
//    fun latestDependencyCheckerDBTest() {
//        val (dbDir, dbName) = dependencyScanExecutor.latestDependencyCheckerDB(
//            "/Users/test/dependency-checker/data/7.0")
//        println("dbDir: $dbDir, dbName: $dbName")
//    }
}
