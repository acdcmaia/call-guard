package com.acdcmaia.callguard

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_MARK_SEEN = "com.acdcmaia.callguard.ACTION_MARK_SEEN"
    }


    private lateinit var vm: MainViewModel

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.checkRole() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm = ViewModelProvider(this, MainViewModel.factory(callGuardApp))[MainViewModel::class.java]
        vm.checkRole()
        handleIntent(intent)
        setContent {
            CallGuardTheme {
                val hasRole by vm.hasRole.collectAsStateWithLifecycle()
                if (hasRole) {
                    CallGuardNavigation()
                } else {
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
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.checkRole()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_MARK_SEEN) markNotificationSeen()
    }

    private fun markNotificationSeen() {
        lifecycleScope.launch {
            val total = callGuardApp.database.recentCallDao().countBlockedOnce()
            callGuardApp.settingsRepository.setSeenBlockedCount(total.toLong())
        }
    }

    private fun requestRole() {
        val rm = getSystemService(RoleManager::class.java)
        val intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        roleRequest.launch(intent)
    }
}
