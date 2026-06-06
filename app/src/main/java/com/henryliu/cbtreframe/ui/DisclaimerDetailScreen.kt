package com.henryliu.cbtreframe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerDetailScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("免责声明与服务协议", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "CBT 思维重构 免责声明与使用条款",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            DisclaimerTextSection(
                title = "一、非医疗服务声明",
                content = "1. 本软件是一款基于认知行为治疗（CBT）理论的心理自助练习工具，仅作为个人进行日常情绪管理、心理调试和自我思维重构的辅助工具。\n2. 本软件所提供的所有功能、AI 分析回应、练习建议等，均不构成且不可替代专业的心理咨询、精神医学诊断、临床治疗或任何其他医疗建议。\n3. 开发者不具备提供临床医疗或心理治疗服务的资质，本软件亦不能替代医生或心理咨询师等专业人士的线下诊疗。"
            )

            DisclaimerTextSection(
                title = "二、心理危机与紧急情况",
                content = "1. 本软件不具备实时心理危机干预、自杀预防或紧急求助监测功能。\n2. 如果您当前正处于严重的心理危机中，或有自残、自杀、伤害他人等极端想法或倾向，请立即停止使用本软件，并前往医院就诊或拨打专业心理援助热线：\n   • 全国心理援助热线：400-161-9995\n   • 紧急求助电话：110（报警）、120（急救）"
            )

            DisclaimerTextSection(
                title = "三、免责与责任限制",
                content = "1. 本软件以“原样（AS IS）”提供，开发者在法律允许的最大范围内，不对本软件的功能完整性、AI 分析的绝对准确性、科学性、以及对特定个人的心理改善效果做出任何明示或暗示的保证。\n2. 人身安全与极端事件免责：用户使用本软件过程中的所有行为决定及其引发的后果均由用户自行承担。开发者对用户因使用或无法使用本软件而导致的任何形式的财产损失、身体健康损害、人身意外、自残、自杀或任何其他第三方起诉及法律责任，均不承担任何直接、间接、附带或特殊的赔偿或法律责任。\n3. 如果您对本软件 of 分析或功能有任何疑虑或产生不适感，应立即停止使用。"
            )

            DisclaimerTextSection(
                title = "四、隐私与数据安全",
                content = "1. 本软件的所有数据（包括您的思维记录、情绪数据、API Key 等）均仅保存在您设备本地的 KeyStore 及 Room/SQLite 数据库中，不上传至任何第三方开发者服务器。\n2. 您需要妥善保管您的设备，以防数据泄露。因设备丢失或被他人获取导致的数据泄露风险由您自行承担。"
            )
        }
    }
}

@Composable
private fun DisclaimerTextSection(title: String, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}
