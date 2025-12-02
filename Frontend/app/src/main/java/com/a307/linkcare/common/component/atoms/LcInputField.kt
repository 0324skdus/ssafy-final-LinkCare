@file:Suppress("FunctionName")

package com.a307.linkcare.common.component.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.a307.linkcare.common.theme.*

@Composable
fun LcInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    backgroundColor: Color = white,
    inactiveBorderColor: Color = unActiveBtn,
    activeBorderColor: Color = main,
) {
    val shape = RoundedCornerShape(22.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val borderColor = if (focused) activeBorderColor else inactiveBorderColor
    val visualTransformation =
        if (isPassword) PasswordVisualTransformation() else VisualTransformation.None

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        enabled = enabled,
        readOnly = readOnly,
        visualTransformation = visualTransformation,
        keyboardOptions = if (isPassword)
            KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        else
            KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(),
        interactionSource = interaction,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 16.sp,
            color = Color(0xFF111111)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 20.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth().height(46.dp - 1.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = unActiveField
                    )
                }
                innerTextField()
            }
        }
    )
}
