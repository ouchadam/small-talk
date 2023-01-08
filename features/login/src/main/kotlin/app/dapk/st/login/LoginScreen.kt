package app.dapk.st.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.design.components.GenericError
import app.dapk.st.login.state.LoginAction
import app.dapk.st.login.state.LoginEvent.LoginComplete
import app.dapk.st.login.state.LoginEvent.WellKnownMissing
import app.dapk.st.login.state.LoginScreenState
import app.dapk.st.login.state.LoginState

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(loginState: LoginState, onLoggedIn: () -> Unit) {
    loginState.ObserveEvents(onLoggedIn)
    LaunchedEffect(true) {
        loginState.dispatch(LoginAction.ComponentLifecycle.Visible)
    }

    var userName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    when (val content = loginState.current.content) {
        is LoginScreenState.Content.Error -> GenericError(cause = content.cause, action = { loginState.dispatch(LoginAction.ComponentLifecycle.Visible) })
        LoginScreenState.Content.Loading -> CenteredLoading()

        is LoginScreenState.Content.Idle -> {
            val showServerUrl = loginState.current.showServerUrl
            Row {
                Spacer(modifier = Modifier.weight(0.1f))
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                ) {
                    Spacer(Modifier.fillMaxHeight(0.2f))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "SmallTalk", fontSize = 34.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    var passwordVisibility by rememberSaveable { mutableStateOf(false) }

                    val focusManager = LocalFocusManager.current
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .autofill(listOf(AutofillType.Username), onFill = { userName = it }),
                        value = userName,
                        onValueChange = { userName = it },
                        singleLine = true,
                        leadingIcon = {
                            Text(text = "@", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 3.dp))
                        },
                        label = { Text("Username") },
                        placeholder = { Text("hello:server.com") },
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        keyboardOptions = KeyboardOptions(autoCorrect = false, keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )

                    val canDoLoginAttempt = if (showServerUrl) {
                        userName.isNotEmpty() && password.isNotEmpty() && serverUrl.isNotEmpty()
                    } else {
                        userName.isNotEmpty() && password.isNotEmpty()
                    }

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .autofill(listOf(AutofillType.Password), onFill = { password = it }),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                        },
                        keyboardActions = KeyboardActions(
                            onDone = { loginState.dispatch(LoginAction.Login(userName, password, serverUrl)) },
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        ),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            imeAction = ImeAction.Done.takeIf { canDoLoginAttempt } ?: ImeAction.Next.takeIf { showServerUrl } ?: ImeAction.None,
                            keyboardType = KeyboardType.Password
                        ),
                        visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                Icon(imageVector = image, "")
                            }
                        }
                    )

                    if (showServerUrl) {
                        TextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Web, contentDescription = null)
                            },
                            keyboardActions = KeyboardActions(onDone = { loginState.dispatch(LoginAction.Login(userName, password, serverUrl)) }),
                            keyboardOptions = KeyboardOptions(
                                autoCorrect = false,
                                imeAction = ImeAction.Done.takeIf { canDoLoginAttempt } ?: ImeAction.None,
                                keyboardType = KeyboardType.Uri
                            ),
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            keyboardController?.hide()
                            loginState.dispatch(LoginAction.Login(userName, password, serverUrl))
                        },
                        enabled = canDoLoginAttempt
                    ) {
                        Text("Sign in".uppercase(), fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.weight(0.1f))
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this
        .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
}

@Composable
private fun LoginState.ObserveEvents(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
                LoginComplete -> onLoggedIn()
                WellKnownMissing -> {
                    Toast.makeText(context, "Couldn't find the homeserver, please enter the server URL", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

