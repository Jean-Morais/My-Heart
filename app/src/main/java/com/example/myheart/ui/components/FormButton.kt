package com.example.myheart.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myheart.ui.theme.AppColors

/** Botão de formulário: preenchido (vinho) ou com contorno ([borderColor] ajusta a cor da borda). */
@Composable
fun FormButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    borderColor: Color = AppColors.Burgundy,
) {
    val buttonModifier = modifier.fillMaxWidth().height(52.dp)
    val shape = RoundedCornerShape(8.dp)

    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            border = BorderStroke(1.dp, borderColor),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AppColors.Surface,
                contentColor = AppColors.Burgundy,
            ),
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Burgundy,
                contentColor = Color.White,
            ),
        ) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
