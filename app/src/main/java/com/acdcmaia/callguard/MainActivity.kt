package com.acdcmaia.callguard

import android.app.role.RoleManager
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
import com.acdcmaia.callguard.ui.CallGuardNavigation
import com.acdcmaia.callguard.ui.theme.CallGuardTheme

class MainActivity : ComponentActivity() {

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkRole() }

    private var hasRole by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkRole()
        setContent {
            CallGuardTheme {
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
        checkRole()
    }

    private fun checkRole() {
        val rm = getSystemService(RoleManager::class.java)
        hasRole = rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    private fun requestRole() {
        val rm = getSystemService(RoleManager::class.java)
        val intent = rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
        roleRequest.launch(intent)
    }
}
