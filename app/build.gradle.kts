plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.meydiai.vgxtf"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("checkBraces") {
    doLast {
        val file = file("src/main/java/com/example/ui/MeydiAiApp.kt")
        if (!file.exists()) {
            println("File does not exist")
            return@doLast
        }
        val lines = file.readLines()
        var inComment = false
        var inString = false
        var inMultilineString = false
        
        data class BraceInfo(val lineNum: Int, val charIndex: Int, val lineText: String)
        val stack = mutableListOf<BraceInfo>()
        
        for (i in lines.indices) {
            val lineNum = i + 1
            val rawLine = lines[i]
            // We want to scan character by character but handle escaped chars, string literals, and comments
            var q = 0
            while (q < rawLine.length) {
                val c = rawLine[q]
                
                // Handle triple quotes multiline strings
                if (!inComment && !inString) {
                    if (rawLine.startsWith("\"\"\"", q)) {
                        inMultilineString = !inMultilineString
                        q += 3
                        continue
                    }
                }
                
                if (inMultilineString) {
                    if (rawLine.startsWith("\"\"\"", q)) {
                        inMultilineString = false
                        q += 3
                        continue
                    }
                    q++
                    continue
                }
                
                if (inComment) {
                    if (rawLine.startsWith("*/", q)) {
                        inComment = false
                        q += 2
                        continue
                    }
                    q++
                    continue
                }
                
                if (inString) {
                    if (c == '\\') {
                        q += 2 // skip escape character
                        continue
                    }
                    if (c == '"') {
                        inString = false
                    }
                    q++
                    continue
                }
                
                // Check for single line comments
                if (rawLine.startsWith("//", q)) {
                    break // Ignore of of line
                }
                
                // Check for start of block comment
                if (rawLine.startsWith("/*", q)) {
                    inComment = true
                    q += 2
                    continue
                }
                
                if (c == '"') {
                    inString = true
                    q++
                    continue
                }
                
                if (c == '{') {
                    stack.add(BraceInfo(lineNum, q + 1, rawLine.trim()))
                } else if (c == '}') {
                    if (stack.isNotEmpty()) {
                        stack.removeAt(stack.size - 1)
                    } else {
                        println("Line ${lineNum}: Unmatched closing brace '}' found at char ${q+1}")
                    }
                }
                q++
            }
        }
        
        if (stack.isNotEmpty()) {
            println("Unmatched open braces:")
            stack.forEach { brace ->
                println("Line ${brace.lineNum}: '${brace.lineText}'")
            }
        } else {
            println("All braces are perfectly balanced!")
        }
    }
}
