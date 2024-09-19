import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(kotlinx.plugins.multiplatform)
    alias(androidx.plugins.library)
    alias(libs.plugins.moko)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.moko.resources)
                api(libs.moko.resources.compose)
            }
        }
        androidMain {
        }
        iosMain {
        }
    }
}

android {
    namespace = "Komari.i18n"
}

multiplatformResources {
    resourcesPackage.set("Komari.i18n")
}

tasks {
   val localesConfigTask = registerLocalesConfigTask(project)
   preBuild {
       dependsOn(localesConfigTask)
   }

    withType<KotlinCompile> {
        compilerOptions.freeCompilerArgs.addAll(
            "-Xexpect-actual-classes",
        )
    }
}
