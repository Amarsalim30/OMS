package com.zeynbakers.order_management_system.core.licensing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.zeynbakers.order_management_system.R
import com.zeynbakers.order_management_system.core.ui.theme.Order_management_systemTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
internal fun AuthGate(
    authorizedContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val repository = remember {
        LicensingRepository(
            firestore = FirebaseFirestore.getInstance(),
            localStore = LicensingLocalStore(context.applicationContext)
        )
    }
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context.applicationContext) }

    var gateState by remember { mutableStateOf<GateUiState>(GateUiState.Validating) }
    var authInFlight by remember { mutableStateOf(false) }
    var authErrorResId by remember { mutableIntStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    suspend fun validateCurrentUser() {
        val user = auth.currentUser
        if (user == null) {
            gateState = GateUiState.SignedOut
            authInFlight = false
            return
        }
        gateState = GateUiState.Validating
        val validation = repository.validateSignedInUser(user.uid)
        gateState =
            when (validation) {
                LicensingValidationResult.Allowed -> GateUiState.Authorized
                is LicensingValidationResult.Blocked -> GateUiState.Blocked(validation.reason)
            }
        authInFlight = false
    }

    LaunchedEffect(Unit) {
        validateCurrentUser()
    }

    Order_management_systemTheme {
        when (val state = gateState) {
            GateUiState.Validating -> {
                FullScreenLoading(message = stringResource(R.string.licensing_validating))
            }

            GateUiState.SignedOut -> {
                LoginScreen(
                    email = email,
                    password = password,
                    inFlight = authInFlight,
                    errorMessage = authErrorResId.takeIf { it != 0 }?.let { stringResource(it) },
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onSignInWithGoogle = {
                        scope.launch {
                            authInFlight = true
                            authErrorResId = 0
                            val hostActivity = context.findActivity()
                            if (hostActivity == null) {
                                authInFlight = false
                                authErrorResId = R.string.auth_error_google_request_failed
                                return@launch
                            }
                            try {
                                val idToken =
                                    requestGoogleIdToken(
                                        activity = hostActivity,
                                        credentialManager = credentialManager
                                    )
                                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                                auth.signInWithCredential(firebaseCredential).await()
                                validateCurrentUser()
                            } catch (error: NoCredentialException) {
                                authInFlight = false
                                authErrorResId = R.string.auth_error_google_no_account
                            } catch (error: GetCredentialCancellationException) {
                                authInFlight = false
                                authErrorResId = R.string.auth_error_google_cancelled
                            } catch (error: GetCredentialException) {
                                authInFlight = false
                                authErrorResId = R.string.auth_error_google_request_failed
                            } catch (error: GoogleIdTokenParsingException) {
                                authInFlight = false
                                authErrorResId = R.string.auth_error_google_invalid_credential
                            } catch (error: Exception) {
                                authInFlight = false
                                authErrorResId = R.string.auth_error_google_firebase_failed
                            }
                        }
                    },
                    onSignInWithEmail = {
                        scope.launch {
                            authInFlight = true
                            authErrorResId = 0
                            try {
                                auth.signInWithEmailAndPassword(email.trim(), password).await()
                                validateCurrentUser()
                            } catch (error: Exception) {
                                authInFlight = false
                                authErrorResId = emailAuthErrorResId(error)
                            }
                        }
                    },
                    onCreateAccountWithEmail = {
                        scope.launch {
                            authInFlight = true
                            authErrorResId = 0
                            try {
                                auth.createUserWithEmailAndPassword(email.trim(), password).await()
                                validateCurrentUser()
                            } catch (error: Exception) {
                                authInFlight = false
                                authErrorResId = emailAuthErrorResId(error)
                            }
                        }
                    }
                )
            }

            GateUiState.Authorized -> {
                authorizedContent()
            }

            is GateUiState.Blocked -> {
                AccessBlockedScreen(
                    reasonRes = blockedReasonMessageRes(state.reason),
                    inFlight = authInFlight,
                    onRetry = {
                        scope.launch {
                            authInFlight = true
                            validateCurrentUser()
                        }
                    },
                    onSignOut = {
                        auth.signOut()
                        authInFlight = false
                        authErrorResId = 0
                        gateState = GateUiState.SignedOut
                    }
                )
            }
        }
    }
}

private sealed interface GateUiState {
    data object Validating : GateUiState
    data object SignedOut : GateUiState
    data object Authorized : GateUiState
    data class Blocked(val reason: LicensingBlockReason) : GateUiState
}

@Composable
private fun LoginScreen(
    email: String,
    password: String,
    inFlight: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onSignInWithEmail: () -> Unit,
    onCreateAccountWithEmail: () -> Unit
) {
    val emailActionsEnabled = !inFlight && email.trim().isNotBlank() && password.isNotBlank()
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.auth_sign_in_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.auth_sign_in_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
            Button(
                onClick = onSignInWithGoogle,
                enabled = !inFlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Text(text = stringResource(R.string.auth_sign_in_google_button))
            }
            Text(
                text = stringResource(R.string.auth_sign_in_method_or),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 14.dp)
            )
            androidx.compose.material3.OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(R.string.auth_email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            androidx.compose.material3.OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.auth_password_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            Button(
                onClick = onSignInWithEmail,
                enabled = emailActionsEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(text = stringResource(R.string.auth_sign_in_email_button))
            }
            TextButton(
                onClick = onCreateAccountWithEmail,
                enabled = emailActionsEnabled,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(text = stringResource(R.string.auth_create_account_button))
            }
            if (inFlight) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun AccessBlockedScreen(
    @StringRes reasonRes: Int,
    inFlight: Boolean,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.auth_access_blocked_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(reasonRes),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
            Button(
                onClick = onRetry,
                enabled = !inFlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            ) {
                Text(text = stringResource(R.string.auth_retry_validation))
            }
            TextButton(
                onClick = onSignOut,
                enabled = !inFlight,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(text = stringResource(R.string.auth_sign_out_button))
            }
            if (inFlight) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun FullScreenLoading(message: String) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@StringRes
private fun blockedReasonMessageRes(reason: LicensingBlockReason): Int {
    return when (reason) {
        LicensingBlockReason.EntitlementMissing -> R.string.auth_blocked_entitlement_missing
        LicensingBlockReason.AccessDenied -> R.string.auth_blocked_not_allowed
        LicensingBlockReason.DeviceLimitReached -> R.string.auth_blocked_device_limit
        LicensingBlockReason.DeviceRevoked -> R.string.auth_blocked_device_revoked
        LicensingBlockReason.EntitlementExpired -> R.string.auth_blocked_expired
        LicensingBlockReason.OfflineGraceExpired -> R.string.auth_blocked_offline_grace
        LicensingBlockReason.ValidationFailed -> R.string.auth_blocked_validation_failed
    }
}

@StringRes
private fun emailAuthErrorResId(error: Exception): Int {
    return when (error) {
        is FirebaseAuthInvalidUserException -> R.string.auth_error_email_account_not_found
        is FirebaseAuthInvalidCredentialsException -> R.string.auth_error_email_invalid_credentials
        is FirebaseAuthUserCollisionException -> R.string.auth_error_email_account_exists
        else -> R.string.auth_error_email_generic
    }
}

private suspend fun requestGoogleIdToken(
    activity: Activity,
    credentialManager: CredentialManager
): String {
    val authorizedAccountToken =
        try {
            tryRequestGoogleIdToken(
                activity = activity,
                credentialManager = credentialManager,
                filterByAuthorizedAccounts = true
            )
        } catch (_: NoCredentialException) {
            null
        }
    if (!authorizedAccountToken.isNullOrBlank()) {
        return authorizedAccountToken
    }
    return tryRequestGoogleIdToken(
        activity = activity,
        credentialManager = credentialManager,
        filterByAuthorizedAccounts = false
    ) ?: throw NoCredentialException("No Google account credential available")
}

private suspend fun tryRequestGoogleIdToken(
    activity: Activity,
    credentialManager: CredentialManager,
    filterByAuthorizedAccounts: Boolean
): String? {
    val googleIdOption =
        GetGoogleIdOption.Builder()
            .setServerClientId(activity.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .build()
    val request =
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    val response = credentialManager.getCredential(
        context = activity,
        request = request
    )
    return credentialToGoogleIdToken(response.credential)
}

private fun credentialToGoogleIdToken(credential: Credential): String? {
    if (credential !is CustomCredential) {
        return null
    }
    if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        return null
    }
    return GoogleIdTokenCredential.createFrom(credential.data).idToken
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
