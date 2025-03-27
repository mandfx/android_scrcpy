package com.suda.androidscrcpy.home

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.suda.androidscrcpy.MainVM
import com.suda.androidscrcpy.ScrcpyActivity

@Composable
fun Home(mainVM: MainVM) {
    var showDialog by remember { mutableStateOf(false) }
    var ip by remember { mutableStateOf("192.168.5.68") }
    var port by remember { mutableStateOf("5555") }
    var code by remember { mutableStateOf("") }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
        ) {
            Card {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = ip,
                        singleLine = true,
                        onValueChange = { ip = it },
                        label = { Text("IP") },
                        placeholder = { Text(text = "必填") },
                    )
                    OutlinedTextField(
                        value = port,
                        singleLine = true,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        placeholder = { Text(text = "必填") },
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    OutlinedTextField(
                        value = code,
                        singleLine = true,
                        onValueChange = { code = it },
                        label = { Text("Pair Code") },
                        placeholder = { Text(text = "可空") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                    )

                    Button(onClick = {
                        mainVM.connect(ip, port, code)
                        showDialog = false
                    }) {
                        Text(text = "添加")
                    }
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {

        Box(modifier = Modifier.fillMaxSize()) {
            val adbs = mainVM.adbList.toList()
            if (adbs.isEmpty()) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "请通过USB插入设备，或者WIFI配对后连接"
                )
            } else {
                LazyColumn (
                    contentPadding = PaddingValues(start = 16.0.dp, end = 16.0.dp, top = 8.0.dp, bottom = 8.0.dp)
                ) {
                    items(adbs.size) {
                        Card (
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            //elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                        val ctx = LocalView.current.context
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(text = adbs[it], modifier = Modifier.align(Alignment.CenterStart))
                            Button(onClick = {
                                ctx.startActivity(Intent(ctx, ScrcpyActivity::class.java).apply {
                                    putExtra("device", adbs[it].split("\t")[0])
                                    putExtra("withNav", true)
                                })
                            }, modifier = Modifier.align(Alignment.CenterEnd)) {
                                Text(text = "连接")
                            }
                        }
                        }
                    }
                }
            }

            val navigationBarHeight = WindowInsets.navigationBars.getBottom(LocalDensity.current).dp
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navigationBarHeight / 2)
            ) {
                Button(onClick = {
                    showDialog = true
                }, modifier = Modifier.padding(top = 5.dp, bottom = 10.0.dp)) {
                    Text(text = "添加WIFI设备")
                }
                Text(text = "Copyright © Scrcpy", fontSize = 10.0.sp)
            }
        }
    }
}
