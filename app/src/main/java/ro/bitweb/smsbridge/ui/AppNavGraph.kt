package ro.bitweb.smsbridge.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import ro.bitweb.smsbridge.ui.components.AppTopBar
import ro.bitweb.smsbridge.ui.screens.DespreScreen
import ro.bitweb.smsbridge.ui.screens.DetaliiTraseuScreen
import ro.bitweb.smsbridge.ui.screens.ListaMesajeScreen
import ro.bitweb.smsbridge.ui.screens.SetariScreen
import ro.bitweb.smsbridge.ui.screens.StatusSmsScreen
import ro.bitweb.smsbridge.ui.screens.TraseeleMeleScreen
import ro.bitweb.smsbridge.ui.screens.TraseuNouScreen
import ro.bitweb.smsbridge.ui.screens.TrimiteSmsScreen


@Composable
fun AppNavGraph() {
    val backStack = rememberNavBackStack(TraseeleMele)
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
        floatingActionButton = {
            if (ecranCurent is TraseeleMele) {
                FloatingActionButton(
                    onClick = {
                        backStack.add(TraseulNou)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adauga traseu nou"
                    )
                }
            }
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
                        onTrimiteSmsClick = {
                            backStack.add(TrimiteSms)
                        },
                        onSetariClick = {
                            backStack.add(Setari)
                        },
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
                entry<Despre> {
                    DespreScreen()
                }
                entry<DetaliiTraseu> { key ->
                    DetaliiTraseuScreen(numeTraseu = key.numeTraseu)
                }
                entry<Setari> {
                    SetariScreen()
                }
                entry<TraseulNou> {
                    TraseuNouScreen()
                }
                entry<TraseeleMele> {
                    TraseeleMeleScreen(
                        onItemClick = { meniu ->
                            backStack.add(DetaliiTraseu(numeTraseu = meniu.nume))
                        },
                        onStatusSmsClick = {
                            backStack.add(StatusSms)
                        }
                    )
                }
            }
        )
    }
}
