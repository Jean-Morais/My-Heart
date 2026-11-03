package com.example.myheart.ui.instructions

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myheart.R
import com.example.myheart.ui.components.AppCard
import com.example.myheart.ui.components.AppHeader
import com.example.myheart.ui.theme.AppColors
import com.example.myheart.ui.theme.MyHeartTheme

private data class InstructionStep(
    val number: Int,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
)

private val connectSteps = listOf(
    InstructionStep(1, R.string.step1_title, R.string.step1_body),
    InstructionStep(2, R.string.step2_title, R.string.step2_body),
    InstructionStep(3, R.string.step3_title, R.string.step3_body),
)

private val readingSteps = listOf(
    InstructionStep(4, R.string.step4_title, R.string.step4_body),
    InstructionStep(5, R.string.step5_title, R.string.step5_body),
    InstructionStep(6, R.string.step6_title, R.string.step6_body),
)

/** Conteúdo estático: LazyColumn com cabeçalho fixo e rolagem do restante. */
@Composable
fun InstructionsScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        AppHeader(title = stringResource(R.string.app_name))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.instructions_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Burgundy,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.instructions_subtitle),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = AppColors.TextMuted,
                    )
                }
            }

            item {
                SectionHeader(
                    title = stringResource(R.string.section_connect),
                    range = stringResource(R.string.section_connect_range),
                )
            }
            items(connectSteps, key = { it.number }) { StepCard(it) }

            item {
                SectionHeader(
                    title = stringResource(R.string.section_reading),
                    range = stringResource(R.string.section_reading_range),
                )
            }
            items(readingSteps, key = { it.number }) { StepCard(it) }

            item { HelpCard() }

            item {
                Text(
                    text = stringResource(R.string.instructions_disclaimer),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = AppColors.TextMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, range: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Burgundy,
        )
        Text(text = range, fontSize = 13.sp, color = AppColors.TextMuted)
    }
}

@Composable
private fun StepCard(step: InstructionStep) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.StepBadge),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = step.number.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Burgundy,
                )
            }
            Column {
                Text(
                    text = stringResource(step.title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Burgundy,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(step.body),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = AppColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun HelpCard() {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(shape)
            .background(AppColors.AlertBackground)
            .border(1.dp, AppColors.AlertBorder, shape)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.help_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.help_body),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = AppColors.TextMuted,
        )
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun InstructionsScreenPreview() {
    MyHeartTheme { InstructionsScreen() }
}
