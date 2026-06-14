package com.example.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.regex.Pattern

enum class Platform {
    YOUTUBE, TIKTOK, INSTAGRAM, X, UNKNOWN
}

sealed class DownloadResult {
    data class Success(val fileUrl: String, val platform: Platform) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
    data class Progress(val percentage: Int) : DownloadResult()
}

object MediaDownloaderRouter {

    // Regex untuk mendeteksi URL masing-masing platform secara akurat (dengan atau tanpa https/www)
    private val YOUTUBE_REGEX = Pattern.compile("^(https?\\:\\/\\/)?(www\\.)?(youtube\\.com|youtu\\.?be)\\/.+$")
    private val TIKTOK_REGEX = Pattern.compile("^(https?\\:\\/\\/)?(www\\.)?(tiktok\\.com|vt\\.tiktok\\.com)\\/.+$")
    private val INSTAGRAM_REGEX = Pattern.compile("^(https?\\:\\/\\/)?(www\\.)?(instagram\\.com)\\/.+$")
    private val X_REGEX = Pattern.compile("^(https?\\:\\/\\/)?(www\\.)?(twitter\\.com|x\\.com)\\/.+$")

    /**
     * Memeriksa dan mengembalikan enum Platform berdasarkan URL yang diberikan
     */
    fun identifyPlatform(url: String): Platform {
        return when {
            YOUTUBE_REGEX.matcher(url).matches() -> Platform.YOUTUBE
            TIKTOK_REGEX.matcher(url).matches() -> Platform.TIKTOK
            INSTAGRAM_REGEX.matcher(url).matches() -> Platform.INSTAGRAM
            X_REGEX.matcher(url).matches() -> Platform.X
            else -> Platform.UNKNOWN
        }
    }

    /**
     * Fungsi utama untuk routing dan memproses unduhan (menghasilkan asynchronous flow status)
     */
    fun processDownload(url: String): Flow<DownloadResult> = flow {
        if (url.isBlank()) {
            emit(DownloadResult.Error("Tautan tidak boleh kosong."))
            return@flow
        }

        val platform = identifyPlatform(url)
        
        when (platform) {
            Platform.YOUTUBE -> {
                emit(DownloadResult.Progress(10))
                emit(downloadYouTube(url))
            }
            Platform.TIKTOK -> {
                emit(DownloadResult.Progress(10))
                emit(downloadTikTok(url))
            }
            Platform.INSTAGRAM -> {
                emit(DownloadResult.Progress(10))
                emit(downloadInstagram(url))
            }
            Platform.X -> {
                emit(DownloadResult.Progress(10))
                emit(downloadX(url))
            }
            Platform.UNKNOWN -> {
                emit(DownloadResult.Error("Platform tidak dikenali atau tautan tidak valid."))
            }
        }
    }

    // --- HANDLER SPESIFIK PLATFORM ---
    // Di bawah ini adalah kerangka handler, yang sebaiknya memanggil API Backend Anda.

    private suspend fun downloadYouTube(url: String): DownloadResult {
        // TODO: Panggil API Backend (yang nge-wrap yt-dlp)
        return DownloadResult.Error("Integrasi YouTube membutuhkan backend khusus untuk yt-dlp.")
    }

    private suspend fun downloadTikTok(url: String): DownloadResult {
        // TODO: Panggil API Backend (Scraper unofficial API TikWM / sejenisnya)
        return DownloadResult.Error("Integrasi TikTok belum diaktifkan.")
    }

    private suspend fun downloadInstagram(url: String): DownloadResult {
        // TODO: Panggil API Backend Instagram Downloader
        return DownloadResult.Error("Instagram sangat ketat, membutuhkan sesi backend terpisah.")
    }

    private suspend fun downloadX(url: String): DownloadResult {
        // TODO: Panggil API Backend (X support juga bisa via yt-dlp)
        return DownloadResult.Error("Handler X belum tersedia.")
    }
}
