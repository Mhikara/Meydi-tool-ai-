package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BrandNeonTeal = Color(0xFF00FFCC)
private val BrandNeonPurple = Color(0xFF7F00FF)
private val DarkBg = Color(0xFF0A0A10)
private val InputBg = Color(0xFF151522)
private val TextMuted = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernLoginScreen(onLoginSuccess: (String?) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo / Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(BrandNeonPurple, BrandNeonTeal)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isRegistering) "CREAT ACCOUNT" else "WELCOME BACK",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White,
                letterSpacing = 2.sp
            )
            
            Text(
                text = if (isRegistering) "Join Meydi OS Workspace" else "Login to continue",
                color = TextMuted,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Inputs
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandNeonTeal) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandNeonTeal,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = BrandNeonTeal) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle password visibility",
                            tint = TextMuted
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandNeonTeal,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!isRegistering) {
                Text(
                    text = "Forgot Password?",
                    color = BrandNeonTeal,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { /* TODO */ }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        scope.launch {
                            delay(1500)
                            isLoading = false
                            onLoginSuccess(email)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandNeonPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (isRegistering) "SIGN UP" else "LOGIN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Or Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(color = Color.DarkGray, modifier = Modifier.weight(1f))
                Text(" OR ", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Divider(color = Color.DarkGray, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Guest Login
            OutlinedButton(
                onClick = {
                    isLoading = true
                    scope.launch {
                        delay(800)
                        isLoading = false
                        onLoginSuccess("guest")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = borderStroke(1.dp, Color.DarkGray),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue as Guest", color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRegistering) "Already have an account? " else "Don't have an account? ",
                    color = TextMuted,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isRegistering) "Login" else "Sign Up",
                    color = BrandNeonTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { isRegistering = !isRegistering }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "DEVELOPER BY MEYDI",
                color = BrandNeonTeal.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
