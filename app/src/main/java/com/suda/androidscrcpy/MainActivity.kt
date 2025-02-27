package com.suda.androidscrcpy

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.suda.androidscrcpy.home.Home
import com.suda.androidscrcpy.ui.component.NiaTopAppBar
import com.suda.androidscrcpy.ui.theme.NiaTheme


class MainActivity : ComponentActivity() {


    private val mainVM: MainVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // We keep this as a mutable state, so that we can track changes inside the composition.
        // This allows us to react to dark/light mode changes.
        var themeSettings by mutableStateOf(
            ThemeSettings(
                darkTheme = false,
                androidTheme = false,
                disableDynamicTheming = false,
            ),
        )

        setContent {
            NiaTheme (
                darkTheme = themeSettings.darkTheme,
                androidTheme = themeSettings.androidTheme,
                disableDynamicTheming = themeSettings.disableDynamicTheming,
            ) {
                MainContent(mainVM)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //杀死进程防止termux_api 起不来
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(mainVM: MainVM) {
    val ctx = LocalView.current.context
    Scaffold (
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column (
            Modifier.fillMaxSize().padding(padding)
                .consumeWindowInsets(padding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ){
            NiaTopAppBar(
                titleRes = R.string.app_name,
                navigationIcon = Icons.Rounded.ArrowBack,
                navigationIconContentDescription = stringResource(id = R.string.top_appbar_navigation_icon_description),
                actionIcon = Icons.Rounded.Menu,
                actionIconContentDescription = stringResource(id = R.string.top_appbar_action_icon_description),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                onActionClick = {
                    Toast.makeText(ctx, "Settings Icon Clicked", Toast.LENGTH_SHORT).show()
                },
                onNavigationClick = {
                    (ctx as? Activity)?.finish()
                },
            )
            Box(
                modifier = Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            ) {
                // A surface container using the "background" color from the theme
                // Surface {  }
                val adbInit by remember {
                    mainVM.adbInit
                }
                if (adbInit) {
                    Home(mainVM)
                    //Color(0xFFB8C3FF)
                    val lifecycleOwner = LocalLifecycleOwner.current
                    LaunchedEffect(lifecycleOwner) {
                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED){
                            mainVM.startAdbDevices()
                        }
                    }
                } else {
                    Greeting("初始化中")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = name,
            modifier = Modifier.align(alignment = Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NiaTheme {
        Greeting(name = "ADB")
    }
}

/**
 * Class for the system theme settings.
 * This wrapping class allows us to combine all the changes and prevent unnecessary recompositions.
 */
data class ThemeSettings(
    val darkTheme: Boolean,
    val androidTheme: Boolean,
    val disableDynamicTheming: Boolean,
)