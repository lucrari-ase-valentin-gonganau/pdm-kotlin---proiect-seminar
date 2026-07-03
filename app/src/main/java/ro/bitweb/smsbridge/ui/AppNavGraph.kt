package ro.bitweb.smsbridge.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ro.bitweb.smsbridge.ui.components.AppTopBar
import ro.bitweb.smsbridge.ui.screens.ListaMesajeScreen
import ro.bitweb.smsbridge.ui.screens.SetariScreen
import ro.bitweb.smsbridge.ui.screens.StatusSmsScreen
import ro.bitweb.smsbridge.ui.screens.TrimiteSmsScreen


@Composable
fun AppNavGraph() {
    val backStack = rememberNavBackStack(StatusSms)
    val ecranCurent = backStack.lastOrNull()

    Scaffold(
        topBar = {
            AppTopBar(
                ecranCurent = ecranCurent,
                onBackClick = {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    }
                },
                onMeniuClick = { pagina ->
                    backStack.add(pagina)
                }
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        val topPadding = padding.calculateTopPadding()

        NavDisplay(
            modifier = Modifier.padding(
                top = topPadding,
                bottom = padding.calculateBottomPadding()
            ),
            backStack = backStack,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { -it / 2 },
                    animationSpec = tween(300)
                )
            },
            popTransitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { -it / 2 },
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            entryProvider = entryProvider {
                entry<StatusSms> {
                    StatusSmsScreen(
                        onVeziMesajeClick = { tip ->
                            backStack.add(ListaMesaje(tip = tip))
                        }
                    )
                }
                entry<ListaMesaje> { key ->
                    ListaMesajeScreen(tip = key.tip)
                }
                entry<TrimiteSms> {
                    TrimiteSmsScreen()
                }
                entry<Setari> {
                    SetariScreen()
                }
            }
        )
    }
}
