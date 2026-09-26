package org.fptn.vpn.ui.login

import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elvishew.xlog.XLog
import kotlinx.coroutines.launch
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.HtmlLinkText
import org.fptn.vpn.ui.common.LegacyPillButton
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.Black
import org.fptn.vpn.ui.theme.Hint
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.Yellow


private const val TAG = "LoginScreen"
private val TOKEN_CLIPBOARD_PREFIXES = listOf("fptn:", "fptnb:")

private enum class LoginMode { EMAIL_SIGN_IN, EMAIL_SIGN_UP, TOKEN }

/**
 * Login screen. Email/password (Firebase account, `gms` only) is the primary way in; pasting a
 * token stays available as a fallback for when Firebase is unreachable or not built in (`foss`).
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginActivityViewModel = viewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val errorText by viewModel.errorTextLiveData.observeAsState("")
    val accountLoginAvailable = remember { viewModel.isAccountLoginAvailable }
    var mode by rememberSaveable {
        mutableStateOf(if (accountLoginAvailable) LoginMode.EMAIL_SIGN_IN else LoginMode.TOKEN)
    }
    var tokenText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.errorTextLiveData.postValue(message)
    }

    fun switchMode(newMode: LoginMode) {
        mode = newMode
        viewModel.errorTextLiveData.postValue("")
    }

    /** Runs [action] with the buttons disabled; failures are logged and shown to the user. */
    fun runAction(logLabel: String, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        coroutineScope.launch {
            try {
                action()
            } catch (e: Exception) {
                XLog.tag(TAG).e("%s failed: %s", logLabel, e.message)
                showError(loginErrorMessage(context, e))
            } finally {
                busy = false
            }
        }
    }

    fun onTokenLogin() = runAction("Token login") {
        viewModel.parseAndSaveToken(tokenText.text)
        onLoginSuccess()
    }

    fun onEmailSignIn() {
        if (email.isBlank() || password.isEmpty()) {
            showError(context.getString(R.string.email_and_password_required))
            return
        }
        runAction("Email login") {
            viewModel.signInWithEmail(email, password)
            onLoginSuccess()
        }
    }

    fun onSignUp() {
        if (email.isBlank() || password.isEmpty()) {
            showError(context.getString(R.string.email_and_password_required))
            return
        }
        if (password != passwordConfirm) {
            showError(context.getString(R.string.passwords_do_not_match))
            return
        }
        runAction("Sign up") {
            viewModel.signUp(email, password)
            passwordConfirm = ""
            mode = LoginMode.EMAIL_SIGN_IN
            viewModel.errorTextLiveData.postValue(context.getString(R.string.sign_up_success))
        }
    }

    fun onForgotPassword() {
        if (email.isBlank()) {
            showError(context.getString(R.string.email_required))
            return
        }
        runAction("Password reset") {
            viewModel.sendPasswordReset(email)
            val message = context.getString(R.string.password_reset_sent)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.errorTextLiveData.postValue(message)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legacyDrawableBackground(R.drawable.application_background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.icon_account_circle_100),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
        )

        when (mode) {
            LoginMode.EMAIL_SIGN_IN, LoginMode.EMAIL_SIGN_UP -> {
                val signUp = mode == LoginMode.EMAIL_SIGN_UP
                LoginTextField(
                    value = email,
                    onValueChange = { email = it },
                    hint = stringResource(R.string.email_hint),
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.padding(top = 32.dp),
                )
                LoginTextField(
                    value = password,
                    onValueChange = { password = it },
                    hint = stringResource(R.string.password_hint),
                    keyboardType = KeyboardType.Password,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (signUp) {
                    LoginTextField(
                        value = passwordConfirm,
                        onValueChange = { passwordConfirm = it },
                        hint = stringResource(R.string.password_confirm_hint),
                        keyboardType = KeyboardType.Password,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                LegacyPillButton(
                    text = stringResource(if (signUp) R.string.sign_up_button else R.string.login_button),
                    backgroundDrawable = R.drawable.round_back_secondary_100,
                    textColor = Primary,
                    onClick = { if (signUp) onSignUp() else onEmailSignIn() },
                    enabled = !busy,
                    modifier = Modifier.padding(top = 32.dp),
                )

                if (signUp) {
                    LoginLink(stringResource(R.string.have_account_sign_in), enabled = !busy) {
                        switchMode(LoginMode.EMAIL_SIGN_IN)
                    }
                } else {
                    LoginLink(stringResource(R.string.forgot_password), enabled = !busy, onClick = ::onForgotPassword)
                    LoginLink(stringResource(R.string.create_account), enabled = !busy) {
                        switchMode(LoginMode.EMAIL_SIGN_UP)
                    }
                }
                LoginLink(stringResource(R.string.login_with_token), enabled = !busy) {
                    switchMode(LoginMode.TOKEN)
                }
            }

            LoginMode.TOKEN -> {
                HtmlLinkText(
                    html = stringResource(R.string.telegram_bot_html),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    color = White,
                    fontSize = 16.sp,
                    padding = PaddingValues(8.dp),
                )

                TokenField(
                    value = tokenText,
                    onValueChange = { tokenText = it },
                    onPaste = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val text = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)?.text?.toString()?.trim()
                        if (text != null && TOKEN_CLIPBOARD_PREFIXES.any { text.startsWith(it) }) {
                            tokenText = TextFieldValue(text, selection = TextRange(text.length))
                        }
                    },
                )

                LegacyPillButton(
                    text = stringResource(R.string.login_button),
                    backgroundDrawable = R.drawable.round_back_secondary_100,
                    textColor = Primary,
                    onClick = ::onTokenLogin,
                    enabled = !busy,
                    modifier = Modifier.padding(top = 32.dp),
                )

                if (accountLoginAvailable) {
                    LoginLink(stringResource(R.string.login_with_email), enabled = !busy) {
                        switchMode(LoginMode.EMAIL_SIGN_IN)
                    }
                }
            }
        }

        Text(
            text = errorText,
            color = Yellow,
            modifier = Modifier.padding(start = 5.dp, top = 20.dp, end = 20.dp, bottom = 20.dp),
        )
    }
}

@Composable
private fun LoginTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    val isPassword = keyboardType == KeyboardType.Password
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .legacyDrawableBackground(R.drawable.round_back_white10_20)
            .padding(10.dp),
        textStyle = TextStyle(color = Black, fontSize = 16.sp),
        singleLine = true,
        cursorBrush = SolidColor(Black),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(text = hint, color = Hint, fontSize = 16.sp)
                innerTextField()
            }
        },
    )
}

@Composable
private fun LoginLink(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = White,
        fontSize = 15.sp,
        modifier = Modifier
            .padding(top = 12.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp),
    )
}

@Composable
private fun TokenField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onPaste: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // The token field never shows the soft keyboard (it's meant to be pasted into), so hide it
    // the moment focus lands here, matching `setShowSoftInputOnFocus(false)`.
    LaunchedEffect(isFocused) {
        if (isFocused) keyboardController?.hide()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp, max = 140.dp)
            .padding(top = 16.dp)
            .legacyDrawableBackground(R.drawable.round_back_white10_20),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 76.dp),
            textStyle = TextStyle(color = Black, fontSize = 16.sp),
            singleLine = false,
            maxLines = 5,
            cursorBrush = SolidColor(Black),
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.TopStart) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.paste_your_token),
                            color = Hint,
                            fontSize = 16.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_content_paste_24),
                contentDescription = stringResource(R.string.paste_button),
                tint = Primary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onPaste),
            )
            Icon(
                painter = painterResource(R.drawable.ic_baseline_close_24),
                contentDescription = stringResource(R.string.clear_button),
                tint = Primary,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(24.dp)
                    .clickable { onValueChange(TextFieldValue("")) },
            )
        }
    }
}
