package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- DATA CLASSES FOR MOSHI SERIALIZATION ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.2f, // low temp for high code precision
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 2048
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class MicrostockMetadata(
    @Json(name = "seo_title") val seoTitle: String,
    @Json(name = "keywords") val keywords: List<String>,
    @Json(name = "description_en") val descriptionEn: String,
    @Json(name = "categories") val categories: List<String>
)

// --- RETROFIT SERVICE INTERFACE ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateCode(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- RETROFIT CLIENT OBJECT ---

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

// --- CLIENT HELPER ENGINE ---

object GeminiGenerator {
    private const val TAG = "GeminiGenerator"
    private val moshi = com.squareup.moshi.Moshi.Builder().build()

    // Custom system instructions to enforce getting PURE, RUNNABLE JAVASCRIPT code
    private const val SYSTEM_INSTRUCTION_PROMPT = """
        Your job is to act as a world-class HTML5 Canvas creative VFX engineer.
        Generate ONLY valid, pure JavaScript code that draws a gorgeous, premium, 10-second looping video animation suitable for stock footage.
        The canvas element has already been declared as:
        <canvas id="canvas" width="1920" height="1080"></canvas>
        
        The animation MUST fit this element and use full resolution (1920x1080).
        Ensure everything loops seamlessly (starts and ends on similar phases, or resets over time smoothly with Math.sin, Math.cos, or a cyclic phase interval).
        
        CRITICAL RULES:
        1. DO NOT wrap the code in backticks (```) or code blocks (```javascript). No markdown! No explanatory conversational text before or after the code block.
        2. Give ONLY pure, executable Javascript. Start directly with retrieving the canvas:
           const canvas = document.getElementById('canvas');
           const ctx = canvas.getContext('2d');
        3. Do NOT include any HTML, css, or wrapper elements.
        4. Use 'requestAnimationFrame' for high-quality continuous rendering and implement a frame/tick-based time tracking.
        5. Deeply customize the visual elements, rendering algorithm, particle limits, speeds, and color dynamics based on the user's prompt (e.g., if they ask for dynamic waterfall, render rich vertical glowing streams with splashes; if cyber, render neon grids/nodes).
        6. Inject rich creative details:
           - Layering: Add background glow using radial gradients.
           - Tail Trails: Draw a semi-transparent fillStyle background rect (e.g. `rgba(10, 8, 25, 0.15)`) inside draw loop to create elegant glowing motion blur/particle trails.
           - Glow Effects: Utilise `ctx.shadowColor` and `ctx.shadowBlur` to make objects, lines, or particles look like radiant glowing neons.
           - Multilingual support: Interpret Indonesian or English prompts accurately and convert them into relevant high-end professional color palettes and physics simulators.
    """

    private fun getApiKey(): String {
        return ApiKeyRegistry.getGeminiKey()
    }

    suspend fun analyzeTemplateForMicrostock(templateTitle: String, templateDescription: String): MicrostockMetadata? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext null

        val systemPrompt = """
            Analyze the following video/media template's title and visual description, and generate highly professional, SEO-friendly metadata suitable for high-end microstock marketplace uploads (e.g., Shutterstock, Getty Images, Adobe Stock, Pond5).
            
            Return ONLY a valid JSON object matching the requested schema. No conversational filler or surrounding markdown fences (like ```json ... ```) - just raw JSON.
            
            Schema:
            {
              "seo_title": "A compelling, keyword-rich commercial title in English (maximum 70-80 characters)",
              "keywords": ["a list of 15 to 25 highly relevant, single-word or short-phrase search keywords in English ordered by importance, all lowercase"],
              "description_en": "A detailed English metadata description of the animation shot (1-2 sentences)",
              "categories": ["Two primary industry sectors, e.g. Technology, Abstract, Backgrounds, Science, Business"]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Template Title: $templateTitle\nVisual Description: $templateDescription")))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.5f, maxOutputTokens = 1024)
        )

        try {
            val response = RetrofitClient.service.generateCode(apiKey, request)
            var rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d(TAG, "Raw microstock metadata response: ${"$"}{rawText}")
            
            var cleanJson = rawText.trim()
            if (cleanJson.startsWith("```")) {
                val start = cleanJson.indexOf("{")
                val end = cleanJson.lastIndexOf("}")
                if (start >= 0 && end > start) {
                    cleanJson = cleanJson.substring(start, end + 1)
                }
            }
            
            val adapter = moshi.adapter(MicrostockMetadata::class.java)
            adapter.fromJson(cleanJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API for microstock metadata.", e)
            null
        }
    }

    suspend fun generatePromptFromImage(base64Image: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        
        if (apiKey.isEmpty()) {
            return@withContext "API Key Gemini belum diatur."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)),
                        Part(text = "Buatkan deskripsi visual yang detail dan imajinatif (dalam 1-2 kalimat) untuk elemen pada gambar ini agar bisa di-generate menjadi animasi. Fokus pada warna, pencahayaan, suasana, dan gerakan jika ada. Jangan deskripsikan gambar sebagai 'foto ruang kerja' atau manusia, tapi konversikan menjasi tema prompt yang artistik dan dinamis. Tulis dalam bahasa Indonesia.")
                    )
                )
            ),
            generationConfig = GenerationConfig()
        )

        try {
            val response = RetrofitClient.service.generateCode(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            return@withContext generatedText?.trim() ?: "Gagal membuat deskripsi."
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API for image.", e)
            return@withContext "Error: ${e.message}"
        }
    }

    suspend fun generateTemplateData(userDescription: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext ""

        val systemPrompt = """
            Your task is to take a user's creative description and generate a structured JSON representing a high-end HTML5 Canvas video template.
            
            The output MUST be a valid JSON object with the following fields:
            - title: A short, catchy title for the template.
            - description: A poetic 1-sentence description of the visual effect.
            - prompt: A refined AI prompt that describes this visual.
            - code: Pure JavaScript code that handles the rendering on a 1920x1080 canvas with id 'canvas'.
            
            CRITICAL CODE RULES:
            - Provide ONLY pure JS code in the 'code' field.
            - Ensure it loops perfectly and looks "high-end" (use gradients, blurs, and physics).
            - Use requestAnimationFrame.
            
            Example Format:
            {
              "title": "Galaxy Swirl",
              "description": "Deep space nebula with golden spiraling stars.",
              "prompt": "Create a golden spiral galaxy with glowing nebulae background.",
              "code": "const canvas = document.getElementById('canvas'); ..."
            }
            
            Return ONLY the raw JSON string. No markdown fences.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Descriptive Prompt: $userDescription")))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.4f)
        )

        try {
            val response = RetrofitClient.service.generateCode(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Clean up JSON from markdown if exists
            var cleanJson = rawText.trim()
            if (cleanJson.startsWith("```")) {
                val start = cleanJson.indexOf("{")
                val end = cleanJson.lastIndexOf("}")
                if (start >= 0 && end > start) {
                    cleanJson = cleanJson.substring(start, end + 1)
                }
            }
            return@withContext cleanJson
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API for template data.", e)
            return@withContext ""
        }
    }

    suspend fun generateCanvasCode(userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        
        if (apiKey.isEmpty()) {
            Log.w(TAG, "Gemini API key is empty. Triggering fallback.")
            return@withContext getLocalFallbackCode(userPrompt)
        }

        val isAssistant = userPrompt.startsWith("AS_ASSISTANT:")
        val systemPrompt = if (isAssistant) {
            "You are a helpful AI Assistant for MeydiAI App. Answer the user question accurately about the app features."
        } else {
            SYSTEM_INSTRUCTION_PROMPT
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = if (isAssistant) userPrompt.removePrefix("AS_ASSISTANT:") else "Create a stunning loop animation based on: $userPrompt")
                    )
                )
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = systemPrompt))
            ),
            generationConfig = GenerationConfig(
                temperature = if (isAssistant) 0.7f else 0.2f
            )
        )

        try {
            val response = RetrofitClient.service.generateCode(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            
            if (generatedText != null) {
                var cleanText = generatedText.trim()
                
                // Robust extraction of code within markdown code block fences (``` or ```javascript)
                val fenceStart = cleanText.indexOf("```")
                if (fenceStart >= 0) {
                    val nextNewLine = cleanText.indexOf("\n", fenceStart)
                    val contentStart = if (nextNewLine > fenceStart) nextNewLine + 1 else fenceStart + 3
                    
                    val fenceEnd = cleanText.lastIndexOf("```")
                    if (fenceEnd > contentStart) {
                        cleanText = cleanText.substring(contentStart, fenceEnd).trim()
                    } else {
                        cleanText = cleanText.substring(contentStart).trim()
                    }
                } else {
                    // Try to clean common fences just in case
                    if (cleanText.startsWith("```javascript")) {
                        cleanText = cleanText.removePrefix("```javascript")
                    } else if (cleanText.startsWith("```js")) {
                        cleanText = cleanText.removePrefix("```js")
                    } else if (cleanText.startsWith("```")) {
                        cleanText = cleanText.removePrefix("```")
                    }
                    if (cleanText.endsWith("```")) {
                        cleanText = cleanText.removeSuffix("```")
                    }
                }
                return@withContext cleanText.trim()
            } else {
                throw Exception("Received empty response from Gemini API.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API. Triggering fallback.", e)
            return@withContext getLocalFallbackCode(userPrompt)
        }
    }

    /**
     * Local intelligent code generator that returns magnificent animated loops
     * to serve as a high-fidelity offline fallback when the API key is not yet configured.
     */
    private fun getLocalFallbackCode(prompt: String): String {
        val lowerPrompt = prompt.lowercase()
        return when {
            // 1. Gradient Fluid Wave
            lowerPrompt.contains("wave") || lowerPrompt.contains("gelombang") || 
            lowerPrompt.contains("gradient") || lowerPrompt.contains("fluid") -> {
                """
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                let time = 0;

                function draw() {
                    ctx.clearRect(0, 0, canvas.width, canvas.height);
                    
                    // Base background gradient
                    const baseGrad = ctx.createLinearGradient(0, 0, 0, canvas.height);
                    baseGrad.addColorStop(0, '#02000a');
                    baseGrad.addColorStop(1, '#0c0721');
                    ctx.fillStyle = baseGrad;
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    // Multi-layered glowing neon waves
                    for (let i = 0; i < 5; i++) {
                        ctx.beginPath();
                        const opacity = 0.08 + i * 0.03;
                        const grad = ctx.createLinearGradient(0, 0, canvas.width, 0);
                        grad.addColorStop(0, `rgba(0, 255, 204, ${'$'}{opacity})`);
                        grad.addColorStop(0.5, `rgba(127, 0, 255, ${'$'}{opacity * 1.5})`);
                        grad.addColorStop(1, `rgba(255, 0, 95, ${'$'}{opacity})`);
                        ctx.fillStyle = grad;

                        for (let x = 0; x <= canvas.width; x += 15) {
                            const y = canvas.height * 0.5 + 
                                      Math.sin(x * 0.004 + time + i * 0.6) * 120 + 
                                      Math.cos(x * 0.002 - time + i * 0.3) * 60;
                            if (x === 0) ctx.moveTo(x, y);
                            else ctx.lineTo(x, y);
                        }
                        ctx.lineTo(canvas.width, canvas.height);
                        ctx.lineTo(0, canvas.height);
                        ctx.closePath();
                        ctx.fill();
                    }

                    time += 0.008;
                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
            // 2. Cyber Space Particle Flow
            lowerPrompt.contains("particle") || lowerPrompt.contains("partikel") || 
            lowerPrompt.contains("star") || lowerPrompt.contains("bintang") || 
            lowerPrompt.contains("space") || lowerPrompt.contains("nebula") ||
            lowerPrompt.contains("holografik") || lowerPrompt.contains("pusaran") ||
            lowerPrompt.contains("badai") -> {
                """
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                const particles = [];

                // Initialize particles
                for (let i = 0; i < 120; i++) {
                    particles.push({
                        angle: Math.random() * Math.PI * 2,
                        distance: Math.random() * 500 + 50,
                        speed: 0.003 + Math.random() * 0.006,
                        radius: Math.random() * 4 + 1.5,
                        hue: Math.random() > 0.5 ? 320 : 270 // magenta / violet
                    });
                }

                function draw() {
                    // Background overlay to produce tail trail effect
                    ctx.fillStyle = 'rgba(6, 4, 16, 0.12)';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    const cx = canvas.width / 2;
                    const cy = canvas.height / 2;

                    particles.forEach(p => {
                        p.angle += p.speed;
                        // Shrink or ripple distance
                        const radDist = p.distance + Math.sin(p.angle * 2) * 20;
                        const x = cx + Math.cos(p.angle) * radDist;
                        const y = cy + Math.sin(p.angle) * radDist;

                        ctx.beginPath();
                        ctx.arc(x, y, p.radius, 0, Math.PI * 2);
                        ctx.fillStyle = `hsla(${'$'}{p.hue}, 100%, 65%, 0.95)`;
                        ctx.shadowBlur = p.radius * 4;
                        ctx.shadowColor = `hsla(${'$'}{p.hue}, 100%, 60%, 1)`;
                        ctx.fill();
                        ctx.shadowBlur = 0;
                    });

                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
            // 3. Neon Circuit Mesh Grid
            lowerPrompt.contains("circuit") || lowerPrompt.contains("sirkuit") || 
            lowerPrompt.contains("mesh") || lowerPrompt.contains("papan") || 
            lowerPrompt.contains("chip") || lowerPrompt.contains("micro") || 
            lowerPrompt.contains("siber") -> {
                """
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                let pulse = 0;

                function draw() {
                    ctx.fillStyle = '#04020b';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    const gridSize = 60;
                    const opacity = 0.2 + Math.abs(Math.sin(pulse)) * 0.4;
                    
                    // Tech wires
                    ctx.strokeStyle = `rgba(0, 255, 204, ${'$'}{opacity})`;
                    ctx.lineWidth = 1.5;

                    for (let x = 0; x < canvas.width; x += gridSize) {
                        for (let y = 0; y < canvas.height; y += gridSize) {
                            ctx.strokeRect(x, y, gridSize, gridSize);
                            
                            // Diagonal tech lines
                            if ((x + y) % 120 === 0) {
                                ctx.beginPath();
                                ctx.moveTo(x, y);
                                ctx.lineTo(x + gridSize, y + gridSize);
                                ctx.strokeStyle = `rgba(255, 0, 95, ${'$'}{opacity * 1.2})`;
                                ctx.stroke();
                            }
                        }
                    }

                    // Cyber micro chips
                    ctx.fillStyle = '#00ffcc';
                    for (let i = 0; i < 15; i++) {
                        const cx = (i * 127) % canvas.width;
                        const cy = (i * 199) % canvas.height;
                        ctx.fillRect(cx - 5, cy - 5, 10, 10);
                        
                        ctx.beginPath();
                        ctx.arc(cx, cy, 18 + Math.sin(pulse + i) * 6, 0, Math.PI * 2);
                        ctx.strokeStyle = 'rgba(0, 255, 204, 0.4)';
                        ctx.lineWidth = 1;
                        ctx.stroke();
                    }

                    pulse += 0.03;
                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
            // 4. 3D Retro Synthwave Grid
            lowerPrompt.contains("synthwave") || lowerPrompt.contains("3d") || 
            lowerPrompt.contains("retro") || lowerPrompt.contains("sunset") || 
            lowerPrompt.contains("horizon") || lowerPrompt.contains("ufuk") || 
            lowerPrompt.contains("matahari") || lowerPrompt.contains("wireframe") -> {
                """
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                let offset = 0;

                function draw() {
                    ctx.fillStyle = '#020108';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    const cx = canvas.width / 2;
                    const cy = canvas.height / 2 - 120;

                    // Glowing neon sun
                    const grad = ctx.createLinearGradient(0, cy - 220, 0, cy + 220);
                    grad.addColorStop(0, '#ff0050');
                    grad.addColorStop(0.5, '#ff5500');
                    grad.addColorStop(1, '#ffaa00');
                    ctx.fillStyle = grad;
                    ctx.beginPath();
                    ctx.arc(cx, cy, 220, 0, Math.PI * 2);
                    ctx.fill();

                    // Synthwave sunset horizon stripes lines
                    ctx.fillStyle = '#020108';
                    for (let h = cy - 220; h < cy + 220; h += 25) {
                        const height = 4 + (h - (cy - 220)) * 0.06;
                        ctx.fillRect(0, h, canvas.width, height);
                    }

                    // 3D wireframe grid floor
                    const horizon = canvas.height * 0.52;
                    ctx.lineWidth = 2;

                    // Perspective grid longitude lines
                    for (let x = -800; x <= canvas.width + 800; x += 120) {
                        ctx.beginPath();
                        ctx.moveTo(cx + (x - cx) * 0.02, horizon);
                        ctx.lineTo(x, canvas.height);
                        ctx.strokeStyle = 'rgba(127, 0, 255, 0.5)';
                        ctx.stroke();
                    }

                    // Moving horizon horizontal latitude lines
                    for (let i = 0; i <= 25; i++) {
                        const ratio = i / 25;
                        const depth = (ratio + offset) % 1.0;
                        const y = horizon + Math.pow(depth, 1.8) * (canvas.height - horizon);
                        
                        ctx.beginPath();
                        ctx.moveTo(0, y);
                        ctx.lineTo(canvas.width, y);
                        ctx.strokeStyle = `rgba(0, 255, 204, ${'$'}{depth * 0.75})`;
                        ctx.stroke();
                    }

                    offset = (offset + 0.005) % 1.0;
                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
            // 5. Cosmic Matrix Code Stream
            lowerPrompt.contains("matrix") || lowerPrompt.contains("stream") || 
            lowerPrompt.contains("hujan") || lowerPrompt.contains("code") || 
            lowerPrompt.contains("kode") || lowerPrompt.contains("biner") || 
            lowerPrompt.contains("binary") || lowerPrompt.contains("falling") -> {
                """
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                
                const fontSize = 24;
                const columns = Math.floor(canvas.width / fontSize);
                const drops = Array(columns).fill(0);
                
                // Random character set
                const chars = "10ABCDEF@#${'$'}%^&*+-/=";

                function draw() {
                    ctx.fillStyle = 'rgba(2, 3, 10, 0.1)';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    ctx.fillStyle = '#00ff66';
                    ctx.font = 'bold ' + fontSize + 'px monospace';

                    for (let i = 0; i < drops.length; i++) {
                        const text = chars[Math.floor(Math.random() * chars.length)];
                        const x = i * fontSize;
                        const y = drops[i] * fontSize;

                        // Glowing key code
                        if (Math.random() > 0.98) {
                            ctx.fillStyle = '#ffffff';
                        } else {
                            ctx.fillStyle = '#00ff66';
                        }
                        
                        ctx.shadowBlur = 8;
                        ctx.shadowColor = '#00ff66';
                        ctx.fillText(text, x, y);
                        ctx.shadowBlur = 0;

                        if (y > canvas.height && Math.random() > 0.975) {
                            drops[i] = 0;
                        }
                        drops[i]++;
                    }
                    setTimeout(() => requestAnimationFrame(draw), 30); // 30fps lock for matrix speed
                }
                draw();
                """.trimIndent()
            }
            // 6. Glitch Abstract Waveform
            lowerPrompt.contains("glitch") || lowerPrompt.contains("waveform") || 
            lowerPrompt.contains("sinus") || lowerPrompt.contains("audio") || 
            lowerPrompt.contains("getar") || lowerPrompt.contains("pink-cyan") ||
            lowerPrompt.contains("abstrak") -> {
                """
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                let offset = 0;

                function draw() {
                    ctx.fillStyle = 'rgba(10, 5, 15, 0.2)';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    ctx.lineWidth = 5;
                    const horizon = canvas.height / 2;

                    // Magenta glitch wave
                    ctx.strokeStyle = '#ff007f';
                    ctx.beginPath();
                    for (let x = 0; x <= canvas.width; x += 10) {
                        const glitch = Math.random() > 0.96 ? (Math.random() - 0.5) * 60 : 0;
                        const y = horizon + Math.sin(x * 0.005 + offset) * 140 + glitch;
                        if (x === 0) ctx.moveTo(x, y);
                        else ctx.lineTo(x, y);
                    }
                    ctx.stroke();

                    // Cyan glitch wave offset
                    ctx.strokeStyle = '#00f6ff';
                    ctx.beginPath();
                    for (let x = 0; x <= canvas.width; x += 10) {
                        const glitch = Math.random() > 0.94 ? (Math.random() - 0.5) * 80 : 0;
                        const y = horizon + Math.sin(x * 0.004 - offset + 1) * 120 + glitch;
                        if (x === 0) ctx.moveTo(x, y);
                        else ctx.lineTo(x, y);
                    }
                    ctx.stroke();

                    offset += 0.05;
                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
            // Default Fallback: Digital Matrix Cyber Mesh
            else -> {
                """
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                let time = 0;
                const nodes = [];

                for (let i = 0; i < 40; i++) {
                    nodes.push({
                        x: Math.random() * canvas.width,
                        y: Math.random() * canvas.height,
                        vx: (Math.random() - 0.5) * 1.5,
                        vy: (Math.random() - 0.5) * 1.5,
                        radius: 3 + Math.random() * 4
                    });
                }

                function draw() {
                    ctx.fillStyle = '#060a16';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    // Ambient circle in center
                    ctx.beginPath();
                    ctx.arc(canvas.width/2, canvas.height/2, 200 + Math.sin(time)*40, 0, Math.PI*2);
                    ctx.fillStyle = 'rgba(0, 255, 204, 0.03)';
                    ctx.fill();

                    // Connection lines
                    for (let i = 0; i < nodes.length; i++) {
                        for (let j = i + 1; j < nodes.length; j++) {
                            const d = Math.hypot(nodes[i].x - nodes[j].x, nodes[i].y - nodes[j].y);
                            if (d < 300) {
                                ctx.beginPath();
                                ctx.moveTo(nodes[i].x, nodes[i].y);
                                ctx.lineTo(nodes[j].x, nodes[j].y);
                                ctx.strokeStyle = `rgba(0, 255, 204, ${'$'}{1 - d / 300 * 0.95})`;
                                ctx.lineWidth = 1;
                                ctx.stroke();
                            }
                        }
                    }

                    // Render nodes
                    nodes.forEach(n => {
                        ctx.beginPath();
                        ctx.arc(n.x, n.y, n.radius, 0, Math.PI * 2);
                        ctx.fillStyle = '#ff66b2';
                        ctx.shadowBlur = 10;
                        ctx.shadowColor = '#ff66b2';
                        ctx.fill();
                        ctx.shadowBlur = 0;

                        n.x += n.vx;
                        n.y += n.vy;

                        if (n.x < 0 || n.x > canvas.width) n.vx *= -1;
                        if (n.y < 0 || n.y > canvas.height) n.vy *= -1;
                    });

                    time += 0.01;
                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
        }
    }
}
