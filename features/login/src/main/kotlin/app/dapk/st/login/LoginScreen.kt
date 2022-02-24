package app.dapk.st.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.dapk.st.core.StartObserving
import app.dapk.st.login.LoginEvent.LoginComplete
import app.dapk.st.login.LoginScreenState.*

@Composable
fun LoginScreen(loginViewModel: LoginViewModel, onLoggedIn: () -> Unit) {
    loginViewModel.ObserveEvents(onLoggedIn)
    LaunchedEffect(true) {
        loginViewModel.start()
    }

    var userName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    when (loginViewModel.state) {
        is Error -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Something went wrong")
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(onClick = {
                        loginViewModel.start()
                    }) {
                        Text("Retry".uppercase())
                    }
                }
            }
        }
        Loading -> {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        Idle ->
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
                        modifier = Modifier.fillMaxWidth(),
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

                    val canDoLoginAttempt = userName.isNotEmpty() && password.isNotEmpty()

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                        },
                        keyboardActions = KeyboardActions(onDone = { loginViewModel.login(userName, password) }),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            imeAction = ImeAction.Done.takeIf { canDoLoginAttempt } ?: ImeAction.None,
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

                    Spacer(Modifier.height(4.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { loginViewModel.login(userName, password) },
                        enabled = canDoLoginAttempt
                    ) {
                        Text("Sign in".uppercase(), fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.weight(0.1f))
            }
    }
}

@Composable
private fun LoginViewModel.ObserveEvents(onLoggedIn: () -> Unit) {
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
                LoginComplete -> onLoggedIn()
            }
        }
    }
}

