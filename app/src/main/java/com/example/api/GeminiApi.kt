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

    // Custom system instructions to enforce getting PURE, RUNNABLE JAVASCRIPT code
    private const val SYSTEM_INSTRUCTION_PROMPT = """
        Your job is to act as a world-class HTML5 Canvas creative engineer.
        Generate ONLY valid, pure JavaScript code that draws a gorgeous, modern, 10-second looping video animation.
        The canvas element has already been declared as:
        <canvas id="canvas" width="1920" height="1080"></canvas>
        
        The animation MUST fit this element and use full resolution (1920x1080).
        Ensure everything loops seamlessly (starts and ends on similar phases).
        
        CRITICAL RULES:
        1. DO NOT wrap the code in backticks (```) or code blocks (```javascript). No markdown!
        2. Give ONLY pure, executable Javascript. Start directly with retrieving the canvas:
           const canvas = document.getElementById('canvas');
           const ctx = canvas.getContext('2d');
        3. Do NOT include any HTML, css, or wrapper elements.
        4. Use 'requestAnimationFrame' for high-quality continuous rendering.
        5. Use colors and animations that fit professional microstock themes: neon curves, glow systems, abstract gravity flow, glowing cyber meshes, sci-fi nodes, synthwave grids, or fluid gradients.
    """

    suspend fun generatePromptFromImage(base64Image: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
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

    suspend fun generateCanvasCode(userPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is empty or uses default placeholder. Triggering fallback.")
            return@withContext getLocalFallbackCode(userPrompt)
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "Create a stunning loop animation based on: $userPrompt")
                    )
                )
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = SYSTEM_INSTRUCTION_PROMPT))
            ),
            generationConfig = GenerationConfig()
        )

        try {
            val response = RetrofitClient.service.generateCode(apiKey, request)
            val generatedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            
            if (generatedText != null) {
                // Clean up any stray markdown code fences if the model forgot to follow the instruction
                var cleanText = generatedText.trim()
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
            lowerPrompt.contains("wave") || lowerPrompt.contains("gelombang") || lowerPrompt.contains("gradient") -> {
                """
                // Neon Wave Flow (Offline Premium Fallback)
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                let time = 0;

                function draw() {
                    ctx.fillStyle = '#05030f';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    // Background ambient light
                    const radial = ctx.createRadialGradient(canvas.width/2, canvas.height/2, 100, canvas.width/2, canvas.height/2, 800);
                    radial.addColorStop(0, 'rgba(30, 15, 60, 0.4)');
                    radial.addColorStop(1, '#05030f');
                    ctx.fillStyle = radial;
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    // Wave rendering
                    for (let j = 0; j < 6; j++) {
                        ctx.beginPath();
                        const hue = (200 + j * 25) % 360;
                        ctx.strokeStyle = `hsla(${'$'}{hue}, 100%, 60%, ${'$'}{0.15 + j * 0.05})`;
                        ctx.lineWidth = 4;
                        
                        for (let x = 0; x <= canvas.width; x += 15) {
                            const y = canvas.height / 2 + 
                                Math.sin(x * 0.003 - time + j * 0.4) * 150 +
                                Math.cos(x * 0.001 + time + j * 0.2) * 80;
                            if (x === 0) ctx.moveTo(x, y);
                            else ctx.lineTo(x, y);
                        }
                        ctx.stroke();
                    }

                    time += 0.015;
                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
            lowerPrompt.contains("particle") || lowerPrompt.contains("partikel") || lowerPrompt.contains("star") -> {
                """
                // Cosmonaut Particle Vortex (Offline Premium Fallback)
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                const pts = [];

                for (let i = 0; i < 150; i++) {
                    pts.push({
                        angle: Math.random() * Math.PI * 2,
                        distance: Math.random() * 600 + 50,
                        speed: 0.002 + Math.random() * 0.005,
                        size: Math.random() * 4 + 1.5,
                        hue: Math.random() * 60 + 280 // magenta-purple neon
                    });
                }

                function draw() {
                    ctx.fillStyle = 'rgba(6, 4, 15, 0.15)';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    const cx = canvas.width / 2;
                    const cy = canvas.height / 2;

                    pts.forEach(p => {
                        p.angle += p.speed;
                        const x = cx + Math.cos(p.angle) * p.distance;
                        const y = cy + Math.sin(p.angle) * p.distance;

                        ctx.beginPath();
                        ctx.arc(x, y, p.size, 0, Math.PI * 2);
                        
                        // Glowing shadow
                        ctx.shadowBlur = p.size * 3;
                        ctx.shadowColor = `hsla(${'$'}{p.hue}, 100%, 65%, 1)`;
                        ctx.fillStyle = `hsla(${'$'}{p.hue}, 100%, 70%, 0.9)`;
                        ctx.fill();

                        ctx.shadowBlur = 0; // reset
                    });

                    // Outer orbit decorative lines
                    ctx.beginPath();
                    ctx.arc(cx, cy, 350, 0, Math.PI * 2);
                    ctx.strokeStyle = 'rgba(255, 0, 127, 0.05)';
                    ctx.lineWidth = 1;
                    ctx.stroke();

                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
            lowerPrompt.contains("neon") || lowerPrompt.contains("tech") || lowerPrompt.contains("grid") || lowerPrompt.contains("sirkuit") -> {
                """
                // Neon Grid Synthwave Landscape (Offline Premium Fallback)
                const canvas = document.getElementById('canvas');
                const ctx = canvas.getContext('2d');
                let speed = 0;

                function draw() {
                    ctx.fillStyle = '#04020a';
                    ctx.fillRect(0, 0, canvas.width, canvas.height);

                    // Sunset neon sun
                    const cx = canvas.width / 2;
                    const cy = canvas.height / 2 - 100;
                    const grad = ctx.createLinearGradient(0, cy - 200, 0, cy + 200);
                    grad.addColorStop(0, '#ff0055');
                    grad.addColorStop(1, '#ffaa00');
                    ctx.fillStyle = grad;
                    ctx.beginPath();
                    ctx.arc(cx, cy, 220, 0, Math.PI * 2);
                    ctx.fill();

                    // Synthwave sunset bars cut (horizontal lines)
                    ctx.fillStyle = '#04020a';
                    for (let h = cy - 200; h < cy + 220; h += 30) {
                        const lineThickness = 3 + (h - (cy - 200)) * 0.07;
                        ctx.fillRect(cx - 250, h, 500, lineThickness);
                    }

                    // Bottom horizontal wireframe grids
                    const horizon = canvas.height * 0.55;
                    ctx.strokeStyle = '#00f6ff';
                    ctx.lineWidth = 2;

                    for (let i = 0; i <= 20; i++) {
                        const depth = (i / 20);
                        const y = horizon + depth * (canvas.height - horizon);
                        ctx.beginPath();
                        ctx.moveTo(0, y);
                        ctx.lineTo(canvas.width, y);
                        ctx.strokeStyle = `rgba(0, 246, 255, ${'$'}{depth})`;
                        ctx.stroke();
                    }

                    // Perspective column lines
                    for (let x = -400; x <= canvas.width + 400; x += 100) {
                        ctx.beginPath();
                        ctx.moveTo(canvas.width / 2 + (x - canvas.width / 2) * 0.1, horizon);
                        ctx.lineTo(x, canvas.height);
                        ctx.strokeStyle = 'rgba(0, 246, 255, 0.4)';
                        ctx.stroke();
                    }

                    speed += 0.5;
                    requestAnimationFrame(draw);
                }
                draw();
                """.trimIndent()
            }
            else -> {
                // Default abstract sci-fi node loop
                """
                // Digital Matrix Cyber Mesh (Offline Premium Fallback)
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
