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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private var navigateTo by mutableStateOf<String?>(null)

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.checkRole() }

    private val batteryRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.checkBatteryOptimization() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm = ViewModelProvider(this, MainViewModel.factory(callGuardApp))[MainViewModel::class.java]
        navigateTo = intent?.getStringExtra(EXTRA_NAVIGATE_TO)
        vm.checkRole()
        vm.checkBatteryOptimization()
        setContent {
            CallGuardTheme {
                val hasRole by vm.hasRole.collectAsStateWithLifecycle()
                val isBatteryUnrestricted by vm.isBatteryUnrestricted.collectAsStateWithLifecycle()
                if (!hasRole) {
                    Scaffold { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Call Guard precisa ser definido como serviço de triagem de chamadas.")
                            Button(
                                onClick = { requestRole() },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Conceder permissão")
                            }
                        }
                    }
                } else if (!isBatteryUnrestricted) {
                    Scaffold { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Para funcionar de forma confiável, o Call Guard precisa ser isento de restrições de bateria.")
                            Button(
                                onClick = { requestBatteryOptimization() },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Configurar bateria")
                            }
                            TextButton(
                                onClick = { vm.checkBatteryOptimization() },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Já configurei")
                            }
                        }
                    }
                } else {
                    CallGuardNavigation(
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
        val intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        roleRequest.launch(intent)
    }

    private fun requestBatteryOptimization() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        batteryRequest.launch(intent)
    }
}
