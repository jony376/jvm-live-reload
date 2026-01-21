package me.seroperson.reload.live.gradle

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test

@Timeout(value = 5, unit = TimeUnit.MINUTES)
class LiveReloadJavalinJteTest : LiveReloadTestBase() {
    @field:TempDir lateinit var projectDir: File

    private val appCode by lazy {
        val kotlinSources = projectDir.resolve("src/main/kotlin")
        kotlinSources.mkdirs()
        kotlinSources.resolve("App.kt")
    }
    private val templateFile by lazy {
        val jteDir = projectDir.resolve("src/main/resources")
        jteDir.mkdirs()
        jteDir.resolve("greet.jte")
    }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val jteClassesDir by lazy {
        val dir = projectDir.resolve("jte-classes")
        dir.mkdirs()
        dir
    }

    @Test
    fun `reload javalin jte template`() {
        settingsFile.writeText(SETTINGS_CONTENT)
        buildFile.writeText(BUILD_CONTENT)
        appCode.writeText(APP_CODE)
        templateFile.writeText(TEMPLATE_1)

        val runner =
            initGradleRunner(
                ":liveReloadRun",
                projectDir,
                mapOf("JTE_CLASSES_DIR" to jteClassesDir.absolutePath),
            )
        val isBuildRunning = AtomicBoolean(true)
        val runThread =
            Thread {
                try {
                    runner.build()
                    isBuildRunning.set(false)
                } catch (_: InterruptedException) {
                    println("Interrupted")
                } catch (ex: Exception) {
                    println("Got exception ${ex.message}")
                }
            }
        runThread.start()

        val greet = runUntil(isBuildRunning, "http://localhost:9000/greet", 200, EXPECTED_HTML_1)

        templateFile.writeText(TEMPLATE_2)

        val greetReloaded =
            runUntil(isBuildRunning, "http://localhost:9000/greet", 200, EXPECTED_HTML_2)

        runThread.interrupt()

        assertTrue(greet && greetReloaded)
    }

    companion object {
        const val SETTINGS_CONTENT = ""
        const val BUILD_CONTENT =
            """
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    application
    id("me.seroperson.reload.live.gradle")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.7.0")
    implementation("io.javalin:javalin-rendering:6.7.0")
    implementation("gg.jte:jte:3.2.2")
    implementation("gg.jte:jte-runtime:3.2.2")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}

liveReload { settings = mapOf("live.reload.http.port" to "8081") }

application { mainClass = "AppKt" }

"""
        const val APP_CODE =
            """
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.ResourceCodeResolver
import io.javalin.Javalin
import io.javalin.http.ContentType as JavalinContentType
import io.javalin.rendering.template.JavalinJte
import java.nio.file.Paths

fun main() {
    val classLoader = Thread.currentThread().contextClassLoader
    val codeResolver = ResourceCodeResolver("", classLoader)
    val templateEngine = TemplateEngine.create(codeResolver, Paths.get(System.getenv("JTE_CLASSES_DIR")), ContentType.Html, classLoader.getParent())
    
    val server = Javalin.create { config ->
            config.fileRenderer(JavalinJte(templateEngine))
        }
        .get("/greet") { ctx ->
            ctx.render("greet.jte", mapOf("name" to "World"))
            ctx.contentType(JavalinContentType.HTML)
        }
        .get("/health") {
            it.status(200)
        }
    try {
        server.start(8081)
        Thread.currentThread().join()
    } catch (ex: InterruptedException) {
        server.stop()
    }
}
"""
        const val TEMPLATE_1 =
            """@param String name
<html>
<body>
<h1>Hello ${"$"}{name}!</h1>
</body>
</html>"""
        const val TEMPLATE_2 =
            """@param String name
<html>
<body>
<h1>Goodbye ${"$"}{name}!</h1>
</body>
</html>"""
        const val EXPECTED_HTML_1 =
            """<html>
<body>
<h1>Hello World!</h1>
</body>
</html>"""
        const val EXPECTED_HTML_2 =
            """<html>
<body>
<h1>Goodbye World!</h1>
</body>
</html>"""
    }
}
