package com.onemb.mbsync

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onemb.mbsync.ui.theme.MBSyncTheme
import com.onemb.mbsync.viewmodels.SynViewModel
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.onemb.mbsync.screens.AppScaffold
import com.onemb.mbsync.screens.NavigationAfterLogin


class MainActivity : ComponentActivity() {
    private var locationPermissionGranted = false
    private var storagePermissionGranted = false
    private var havePermissions by mutableStateOf(false)
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            havePermissions = Environment.isExternalStorageManager()
        }
    }


    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                havePermissions = true
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + applicationContext.packageName)
                launcher.launch(intent)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: SynViewModel by viewModels()
        val splash = installSplashScreen()


        checkIfLocationPermissionNeeded(this) {
            locationPermissionGranted = it
            Log.d("PERMISSION_AMIT", it.toString())
        }

        checkIfStoragePermissionNeeded() {
            storagePermissionGranted = it
            Log.d("PERMISSION_AMIT", it.toString())
        }

        if(storagePermissionGranted && locationPermissionGranted) {
            viewModel.updateState(false)
            havePermissions = true
        } else {
//            viewModel.updateState(false)
        }

        setContent {
            MBSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val progress = remember { mutableStateOf(0.5f) }
                    val appNotReady by viewModel.state.collectAsState()
                    splash.setKeepOnScreenCondition{ appNotReady }
                    if(!havePermissions) {
                        NavigationBeforeLogin(progress, ::checkStoragePermission)
                    } else {
                        NavigationAfterLogin(progress)
                    }
                }
            }
        }
    }
}

fun checkIfLocationPermissionNeeded(context: Context, setLocationPermissionGranted: (Boolean) -> Unit) {
    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
        setLocationPermissionGranted(true)
    } else {
        setLocationPermissionGranted(false)
    }
}

fun checkIfStoragePermissionNeeded(setStoragePermissionGranted: (Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val isGranted = Environment.isExternalStorageManager()
        setStoragePermissionGranted(isGranted)
    } else {
        setStoragePermissionGranted(false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun NavigationBeforeLogin(progress: MutableState<Float>, checkStoragePermission: () -> Unit) {
    AppScaffold(
        {
            TopAppBar(
                title = {
                    Text(text = "")
                }
            )
        },
        {
            BottomAppBar(
                content = {
                    LinearProgressIndicator(
                        progress = progress.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }
            )
        }
    ) {paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,

        ) {
            val navController = rememberNavController()

            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    progress.value = 1f
                    navController.navigate("storagePermission")
                } else {
                    // Handle location permission denied
                }
            }

            NavHost(navController = navController, startDestination = "locationPermission") {
                composable("locationPermission") {
                    Column (
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ){
                        Text(
                            modifier = Modifier.padding(10.dp),
                            text = "Location permission is needed for this app to search local devices on wifi network"
                        )
                        Spacer(modifier = Modifier.padding(10.dp),)
                        ElevatedButton(onClick = {
                            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        }) {
                            Text(text = "Give Permission")
                        }
                    }
                }
                composable("storagePermission") {
                    Column (
                        modifier = Modifier
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ){
                        Text(
                            modifier = Modifier.padding(10.dp),
                            text = "Storage permission is needed for this app to sync all file to network location."
                        )
                        Spacer(modifier = Modifier.padding(10.dp),)
                        ElevatedButton(onClick = {
                            checkStoragePermission()
                        }) {
                            Text(text = "Give Permission")
                        }
                    }
                }
                // Add more destinations similarly.
            }
        }

    }
}
