package com.example.smsfirewall.ui.inbox

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smsfirewall.R
import kotlinx.coroutines.launch

private const val TAG = "NewMessageScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewMessageScreen(
    onBack: () -> Unit,
    onSendMessage: (destinationAddress: String, messageBody: String) -> Unit
) {
    var destinationAddress by remember { mutableStateOf("") }
    var draftMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }

        val contactUri = result.data?.data
        if (contactUri == null) {
            Toast.makeText(context, context.getString(R.string.contact_pick_failed), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val selectedNumber = readPhoneNumberFromPickerUri(
                context = context,
                contactUri = contactUri
            )
            if (selectedNumber.isNullOrBlank()) {
                Toast.makeText(context, context.getString(R.string.number_not_found), Toast.LENGTH_SHORT).show()
            } else {
                destinationAddress = selectedNumber
            }
        }
    }

    fun submitMessage() {
        val recipient = destinationAddress.trim()
        val messageBody = draftMessage.trim()

        if (recipient.isBlank()) {
            Toast.makeText(context, context.getString(R.string.enter_number), Toast.LENGTH_SHORT).show()
            return
        }
        if (messageBody.isBlank()) {
            Toast.makeText(context, context.getString(R.string.message_empty), Toast.LENGTH_SHORT).show()
            return
        }

        onSendMessage(recipient, messageBody)
    }

    val handleBack = {
        focusManager.clearFocus()
        keyboardController?.hide()
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .swipeBackGesture(onBack = handleBack)
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
            .imePadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.new_message),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = handleBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = destinationAddress,
                onValueChange = { destinationAddress = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                label = { Text(text = stringResource(R.string.phone_number)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )
            FloatingActionButton(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    val pickContactIntent = Intent(
                        Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    )
                    runCatching {
                        contactPickerLauncher.launch(pickContactIntent)
                    }.onFailure { throwable ->
                        Log.e(TAG, "Failed to open contact picker", throwable)
                        Toast.makeText(context, context.getString(R.string.contacts_open_failed), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = stringResource(R.string.cd_pick_contact)
                )
            }
        }

        OutlinedTextField(
            value = draftMessage,
            onValueChange = { draftMessage = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            label = { Text(text = stringResource(R.string.message_label)) },
            placeholder = { Text(text = stringResource(R.string.message_placeholder)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    submitMessage()
                    keyboardController?.hide()
                }
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledIconButton(
                onClick = {
                    submitMessage()
                    keyboardController?.hide()
                },
                enabled = destinationAddress.isNotBlank() && draftMessage.isNotBlank(),
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_send)
                )
            }
        }
    }
}
