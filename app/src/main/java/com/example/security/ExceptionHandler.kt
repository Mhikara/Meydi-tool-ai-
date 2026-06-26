package com.example.security

import java.io.IOException

open class CryptoSecurityException(message: String, cause: Throwable? = null) : IOException(message, cause)

class DecryptionException(message: String, cause: Throwable? = null) : CryptoSecurityException(message, cause)

class KeyStoreException(message: String, cause: Throwable? = null) : CryptoSecurityException(message, cause)

class NetworkInsecurityException(message: String, cause: Throwable? = null) : CryptoSecurityException(message, cause)
