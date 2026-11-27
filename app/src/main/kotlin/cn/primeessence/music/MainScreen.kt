// Main Screen

package cn.primeessence.music

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

enum class Tab {
    Home,
    Music
}

@Composable
fun MainScreen() {
    val homeStack: NavBackStack<NavKey> = rememberNavBackStack(HomeRoute)
    val musicStack: NavBackStack<NavKey> = rememberNavBackStack(MusicRoute)
    
    var selectedTab by remember { mutableStateOf(Tab.Home) }

    val currentBackStack = when(selectedTab){
        Tab.Home -> homeStack
        Tab.Music -> musicStack
    }

    BackHandler(enabled = currentBackStack.size > 1) {
        currentBackStack.removeLastOrNull()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == Tab.Home,
                    onClick = { selectedTab = Tab.Home },
                    label = { Text("Home") },
                    icon = {}
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.Music,
                    onClick = { selectedTab = Tab.Music },
                    label = { Text("Music") },
                    icon = {}
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (selectedTab) {
                Tab.Home -> AppNav(homeStack)
                Tab.Music -> AppNav(musicStack)
            }
        }
    }
}