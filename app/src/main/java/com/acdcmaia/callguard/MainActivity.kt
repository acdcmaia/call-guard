package com.acdcmaia.callguard

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.acdcmaia.callguard.ui.CallGuardNavigation
import com.acdcmaia.callguard.ui.theme.CallGuardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var vm: MainViewModel
    private var navigateTo by androidx.compose.runtime.mutableStateOf<String?>(null)

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.checkRole() }

    private val batteryRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.checkBatteryOptimization() }

    private val permissionsRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.checkPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm = ViewModelProvider(this, MainViewModel.factory(callGuardApp))[MainViewModel::class.java]
        navigateTo = intent?.getStringExtra(EXTRA_NAVIGATE_TO)
        vm.checkRole()
        vm.checkBatteryOptimization()
        vm.checkPermissions()
        setContent {
            CallGuardTheme {
                val hasRole by vm.hasRole.collectAsStateWithLifecycle()
                val isBatteryUnrestricted by vm.isBatteryUnrestricted.collectAsStateWithLifecycle()
                val hasPermissions by vm.hasPermissions.collectAsStateWithLifecycle()

                when {
                    !hasRole -> SetupStep(
                        message = "Call Guard precisa ser definido como serviço de triagem de chamadas.",
                        buttonLabel = "Conceder permissão",
                        onAction = { requestRole() }
                    )
                    !hasPermissions -> SetupStep(
                        message = "O Call Guard precisa de acesso aos seus contatos, histórico de chamadas e permissão para exibir notificações.",
                        buttonLabel = "Conceder permissões",
                        onAction = { permissionsRequest.launch(MainViewModel.requiredPermissions()) }
                    )
                    !isBatteryUnrestricted -> SetupStep(
                        message = "Para funcionar de forma confiável, o Call Guard precisa ser isento de restrições de bateria.",
                        buttonLabel = "Configurar bateria",
                        onAction = { requestBatteryOptimization() },
                        secondaryButtonLabel = "Já configurei",
                        onSecondaryAction = { vm.checkBatteryOptimization() }
                    )
                    else -> CallGuardNavigation(
                        navigateTo = navigateTo,
                        onNavigateConsumed = { navigateTo = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.`package` == null || intent.`package` == packageName) {
            navigateTo = intent.getStringExtra(EXTRA_NAVIGATE_TO)
        }
    }

    override fun onResume() {
        super.onResume()
        vm.checkRole()
        vm.checkBatteryOptimization()
        vm.checkPermissions()
        lifecycleScope.launch(Dispatchers.IO) {
            val total = callGuardApp.callRepository.countBlockedOnce()
            callGuardApp.settingsRepository.setSeenBlockedCount(total)
        }
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }

    private fun requestRole() {
        val rm = getSystemService(RoleManager::class.java)
        roleRequest.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    private fun requestBatteryOptimization() {
        batteryRequest.launch(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }
}

@Composable
private fun SetupStep(
    message: String,
    buttonLabel: String,
    onAction: () -> Unit,
    secondaryButtonLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(buttonLabel)
            }
            if (secondaryButtonLabel != null && onSecondaryAction != null) {
                TextButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(secondaryButtonLabel)
                }
            }
        }
    }
}
