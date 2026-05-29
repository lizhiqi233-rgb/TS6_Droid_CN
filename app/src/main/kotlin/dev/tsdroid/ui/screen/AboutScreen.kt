package dev.tsdroid.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tsdroid.han.R

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repoUrl = "https://github.com/YUAXI/TS6_Droid_CN"
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Dark mode backdrop
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // --- 1. TOP NAVIGATION BAR ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.about_back),
                color = Color(0xFF64B5F6),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onBack() }.padding(8.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = stringResource(R.string.about_software),
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // --- 2. ORIGINAL DEVELOPER CREDITS ---
        Text(text = stringResource(R.string.about_credits_title), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_credits_desc),
            color = Color(0xB3FFFFFF), fontSize = 14.sp, lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 3. SECONDARY DEVELOPER BRANDING & ENHANCEMENTS ---
        Text(text = stringResource(R.string.about_enhancement_title), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_enhancement_desc),
            color = Color(0xB3FFFFFF), fontSize = 14.sp, lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. OPEN SOURCE LICENSE (GPLv3) SECTION ---
        Text(text = stringResource(R.string.about_license_title), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_license_desc),
            color = Color(0xB3FFFFFF), fontSize = 14.sp, lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 5. REPOSITORY & UPDATE CHANNELS (HYPERLINK) ---
        Text(text = stringResource(R.string.about_repo_title), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.about_repo_desc),
            color = Color(0xB3FFFFFF), fontSize = 14.sp, lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.about_repo_link),
            color = Color(0xFF64B5F6),
            fontSize = 14.sp,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to avoid handler crashes if no browser is active
                }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
        Spacer(modifier = Modifier.height(28.dp))

        // --- 6. STRICTOR ANTI-PIRACY / FREE SOFTWARE WARNING CONTAINER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0x26FF5252)), // Semi-transparent warning red tint
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.about_warning_title),
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.about_warning_desc),
                    color = Color(0xFFFF8A80),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
