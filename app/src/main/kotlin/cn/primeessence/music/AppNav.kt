// App Nav

package cn.primeessence.music

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.entryProvider

@Composable
fun AppNav(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeScreen()
            }
            
            entry<MusicRoute> {
                MusicScreen()
            }
       }
    )
}