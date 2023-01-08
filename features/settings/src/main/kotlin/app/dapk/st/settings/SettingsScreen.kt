package app.dapk.st.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.dapk.st.core.Lce
import app.dapk.st.core.StartObserving
import app.dapk.st.core.components.CenteredLoading
import app.dapk.st.core.components.Header
import app.dapk.st.core.extensions.takeAs
import app.dapk.st.core.getActivity
import app.dapk.st.design.components.*
import app.dapk.st.engine.ImportResult
import app.dapk.st.navigator.Navigator
import app.dapk.st.settings.SettingsEvent.*
import app.dapk.st.settings.eventlogger.EventLogActivity
import app.dapk.st.settings.state.ComponentLifecycle
import app.dapk.st.settings.state.RootActions
import app.dapk.st.settings.state.ScreenAction
import app.dapk.st.settings.state.SettingsState
import app.dapk.state.SpiderPage
import app.dapk.state.page.PageAction

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(settingsState: SettingsState, onSignOut: () -> Unit, navigator: Navigator) {
    settingsState.ObserveEvents(onSignOut)
    LaunchedEffect(true) {
        settingsState.dispatch(ComponentLifecycle.Visible)
    }

    val onNavigate: (SpiderPage<out Page>?) -> Unit = {
        when (it) {
            null -> navigator.navigate.upToHome()
            else -> settingsState.dispatch(PageAction.GoTo(it))
        }
    }
    Spider(currentPage = settingsState.current.state1.page, onNavigate = onNavigate, toolbar = { navigate, title -> Toolbar(navigate, title) }) {
        item(Page.Routes.root) {
            RootSettings(
                it,
                onClick = { settingsState.dispatch(ScreenAction.OnClick(it)) },
                onRetry = { settingsState.dispatch(ComponentLifecycle.Visible) }
            )
        }
        item(Page.Routes.encryption) {
            Encryption(settingsState, it)
        }
        item(Page.Routes.pushProviders) {
            PushProviders(settingsState, it)
        }
        item(Page.Routes.importRoomKeys) {
            when (val result = it.importProgress) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(0.8f),
                        ) {
                            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                                it?.let {
                                    settingsState.dispatch(RootActions.SelectKeysFile(it))
                                }
                            }
                            val keyboardController = LocalSoftwareKeyboardController.current
                            Button(modifier = Modifier.fillMaxWidth(), onClick = { launcher.launch("text/*") }) {
                                Text(text = "SELECT FILE".uppercase())
                            }

                            if (it.selectedFile != null) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("Importing file: ${it.selectedFile.name}", overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(24.dp))

                                var passphrase by rememberSaveable { mutableStateOf("") }
                                var passwordVisibility by rememberSaveable { mutableStateOf(false) }
                                val startImportAction = {
                                    keyboardController?.hide()
                                    settingsState.dispatch(RootActions.ImportKeysFromFile(it.selectedFile.uri, passphrase))
                                }

                                TextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = passphrase,
                                    onValueChange = { passphrase = it },
                                    label = { Text("Passphrase") },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
                                    },
                                    keyboardActions = KeyboardActions(onDone = { startImportAction() }),
                                    keyboardOptions = KeyboardOptions(autoCorrect = false, imeAction = ImeAction.Done, keyboardType = KeyboardType.Password),
                                    visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        val image = if (passwordVisibility) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                        IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                                            Icon(imageVector = image, contentDescription = null)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = startImportAction,
                                    enabled = passphrase.isNotEmpty()
                                ) {
                                    Text(text = "Import".uppercase())
                                }
                            }
                        }
                    }
                }

                is ImportResult.Success -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Successfully imported ${result.totalImportedKeysCount} keys")
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { navigator.navigate.upToHome() }) {
                                Text(text = "Close".uppercase())
                            }
                        }
                    }
                }

                is ImportResult.Error -> {
                    val message = when (result.cause) {
                        ImportResult.Error.Type.NoKeysFound -> "No keys found in the file"
                        ImportResult.Error.Type.UnexpectedDecryptionOutput -> "Unable to decrypt file, double check your passphrase"
                        is ImportResult.Error.Type.Unknown -> "Unknown error"
                        ImportResult.Error.Type.UnableToOpenFile -> "Unable to open file"
                        ImportResult.Error.Type.InvalidFile -> "Unable to process file"
                    }
                    GenericError(
                        message = message,
                        label = "Close",
                        cause = result.cause.takeAs<ImportResult.Error.Type.Unknown>()?.cause
                    ) {
                        navigator.navigate.upToHome()
                    }
                }

                is ImportResult.Update -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Importing ${result.importedKeysCount} keys...")
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(Modifier.wrapContentSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RootSettings(page: Page.Root, onClick: (SettingItem) -> Unit, onRetry: () -> Unit) {
    when (val content = page.content) {
        is Lce.Content -> {
            LazyColumn(
                Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth()
            ) {
                items(content.value) { item ->
                    when (item) {
                        is SettingItem.Text -> {
                            val itemOnClick = onClick.takeIf {
                                item.id != SettingItem.Id.Ignored && item.enabled
                            }?.let { { it.invoke(item) } }

                            SettingsTextRow(item.content, item.subtitle, itemOnClick, enabled = item.enabled)
                        }

                        is SettingItem.AccessToken -> {
                            Row(
                                Modifier
                                    .padding(start = 24.dp, end = 24.dp)
                                    .clickable { onClick(item) }) {
                                Column {
                                    Spacer(Modifier.height(24.dp))
                                    Text(text = item.content, fontSize = 24.sp)
                                    Spacer(Modifier.height(24.dp))
                                    Divider(
                                        color = Color.Gray, modifier = Modifier
                                            .height(1.dp)
                                            .alpha(0.2f)
                                    )
                                }
                            }
                        }

                        is SettingItem.Header -> Header(item.label)
                        is SettingItem.Toggle -> SettingsToggleRow(item.content, item.subtitle, item.state, onToggle = {
                            onClick(item)
                        })
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }

        is Lce.Error -> GenericError(cause = content.cause, action = onRetry)

        is Lce.Loading -> {
            // Should be quick enough to avoid needing a loading state
        }
    }
}

@Composable
private fun Encryption(state: SettingsState, page: Page.Security) {
    Column {
        TextRow("Import room keys", includeDivider = false, onClick = { state.dispatch(ScreenAction.OpenImportRoom) })
    }
}


@Composable
private fun PushProviders(state: SettingsState, page: Page.PushProviders) {
    LaunchedEffect(true) {
        state.dispatch(RootActions.FetchProviders)
    }

    when (val lce = page.options) {
        null -> {}
        is Lce.Loading -> CenteredLoading()
        is Lce.Content -> {
            LazyColumn {
                items(lce.value) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = it == page.selection, onClick = { state.dispatch(RootActions.SelectPushProvider(it)) })
                        Text(it.id)
                    }
                }
            }
        }

        is Lce.Error -> GenericError(cause = lce.cause) { state.dispatch(RootActions.FetchProviders) }
    }
}


@Composable
private fun SettingsState.ObserveEvents(onSignOut: () -> Unit) {
    val context = LocalContext.current
    StartObserving {
        this@ObserveEvents.events.launch {
            when (it) {
                SignedOut -> onSignOut()
                is CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("dapk token", it.content))
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                }

                is SettingsEvent.Toast -> Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                OpenEventLog -> {
                    context.startActivity(Intent(context, EventLogActivity::class.java))
                }

                is OpenUrl -> {
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = it.url.toUri() })
                }

                RecreateActivity -> {
                    context.getActivity()?.recreate()
                }
            }
        }
    }
}
