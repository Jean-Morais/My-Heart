package com.example.myheart.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myheart.R
import com.example.myheart.data.sensor.ble.BluetoothPermissions
import com.example.myheart.ui.bluetooth.openAppSettings
import com.example.myheart.ui.components.AppBottomBar
import com.example.myheart.ui.components.BottomBarItem
import com.example.myheart.ui.history.HistoryScreen
import com.example.myheart.ui.history.HistoryViewModel
import com.example.myheart.ui.home.HomeScreen
import com.example.myheart.ui.instructions.InstructionsScreen
import com.example.myheart.ui.navigation.HistoryRoute
import com.example.myheart.ui.navigation.HomeRoute
import com.example.myheart.ui.navigation.InstructionsRoute
import com.example.myheart.ui.navigation.ProfileRoute
import com.example.myheart.ui.navigation.ReadingRoute
import com.example.myheart.ui.profile.ProfileScreen
import com.example.myheart.ui.profile.ProfileViewModel
import com.example.myheart.ui.reading.ReadingScreen
import com.example.myheart.ui.theme.AppColors
import kotlin.reflect.KClass

private data class TopLevelDestination(
    val route: Any,
    /** Rotas que deixam este item "ativo" (a Leitura pertence ao fluxo do Início). */
    val activeRouteClasses: List<KClass<*>>,
    val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        HomeRoute,
        listOf(HomeRoute::class, ReadingRoute::class, HistoryRoute::class),
        R.string.nav_home,
        Icons.Outlined.Home,
    ),
    TopLevelDestination(
        InstructionsRoute, listOf(InstructionsRoute::class), R.string.nav_instructions, Icons.AutoMirrored.Outlined.MenuBook,
    ),
    TopLevelDestination(
        ProfileRoute, listOf(ProfileRoute::class), R.string.nav_profile, Icons.Outlined.Person,
    ),
)

@Composable
fun MyHeartApp(
    viewModel: MyHeartViewModel,
    historyViewModel: HistoryViewModel,
    profileViewModel: ProfileViewModel,
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Conectar exige as permissões de Bluetooth (que mudam com a versão do Android); elas são
    // pedidas no momento do toque, e só depois a conexão começa.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) viewModel.onConnectClicked() else viewModel.onBluetoothPermissionDenied()
    }
    val onConnectClick = {
        if (BluetoothPermissions.granted(context)) {
            viewModel.onConnectClicked()
        } else {
            permissionLauncher.launch(BluetoothPermissions.required())
        }
    }

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    // Sair da tela de Leitura (voltar, trocar de aba...) cancela uma leitura em andamento.
    val onReadingScreen = currentDestination?.hasRoute<ReadingRoute>() == true
    var wasOnReadingScreen by remember { mutableStateOf(false) }
    LaunchedEffect(onReadingScreen) {
        if (wasOnReadingScreen && !onReadingScreen) viewModel.onCancelReading()
        wasOnReadingScreen = onReadingScreen
    }

    val items = topLevelDestinations.map { destination ->
        BottomBarItem(
            label = stringResource(destination.labelRes),
            icon = destination.icon,
            selected = currentDestination?.hierarchy?.any { node ->
                destination.activeRouteClasses.any { node.hasRoute(it) }
            } == true,
            onClick = {
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }

    Scaffold(
        containerColor = AppColors.Background,
        // Cada tela trata as próprias áreas de sistema (o cabeçalho desenha sob a barra de status).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { AppBottomBar(items) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    uiState = uiState,
                    onConnectClick = onConnectClick,
                    onDisconnectClick = viewModel::onDisconnectClicked,
                    onReadClick = {
                        viewModel.onReadClicked()
                        navController.navigate(ReadingRoute)
                    },
                    onHistoryClick = { navController.navigate(HistoryRoute) },
                    onRetryLoadClick = viewModel::onRetryLoad,
                    onOpenAppSettingsClick = { openAppSettings(context) },
                )
            }
            composable<ReadingRoute> {
                ReadingScreen(
                    uiState = uiState,
                    onBackClick = { navController.popBackStack() },
                    onCancelClick = {
                        viewModel.onCancelReading()
                        navController.popBackStack()
                    },
                    onNewReadingClick = viewModel::onReadClicked,
                    onRetrySaveClick = viewModel::onRetrySave,
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBackClick = { navController.popBackStack() },
                )
            }
            composable<InstructionsRoute> { InstructionsScreen() }
            composable<ProfileRoute> { ProfileScreen(viewModel = profileViewModel) }
        }
    }
}
