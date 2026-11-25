package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ai.CurriculumAiRepository
import com.example.data.backup.LocalBackupStore
import com.example.data.repository.CurriculumRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOut
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BackupAndProfileScreen
import com.example.ui.screens.CollectionScreen
import com.example.ui.screens.GeneratorScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.theme.AjarMerdekaTheme
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AjarMerdekaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navTabs = listOf(
    NavTabItem("beranda", "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    NavTabItem("generator", "Generator", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavTabItem("koleksi", "Koleksi", Icons.Filled.FolderCopy, Icons.Outlined.FolderCopy),
    NavTabItem("backup", "Backup & Profil", Icons.Filled.Backup, Icons.Outlined.Backup)
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = auth.currentUser
    )
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedAppShell(
                currentUserId = user.uid,
                userEmail = user.email,
                userName = user.displayName
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    currentUserId: String,
    userEmail: String?,
    userName: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = CurriculumRepository(db),
                    aiRepository = CurriculumAiRepository(),
                    localBackupStore = LocalBackupStore(app.filesDir),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val docsState by viewModel.docsState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val backupLogsState by viewModel.backupLogsState.collectAsStateWithLifecycle()
    val cloudBackupsState by viewModel.cloudBackupsState.collectAsStateWithLifecycle()
    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterDocType by viewModel.filterDocType.collectAsStateWithLifecycle()
    val filterStarredOnly by viewModel.filterStarredOnly.collectAsStateWithLifecycle()
    val pendingRestoreBundle by viewModel.pendingRestoreBundle.collectAsStateWithLifecycle()
    val isBusyBackup by viewModel.isBusyBackup.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "beranda"

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "beranda",
                    modifier = Modifier.weight(1f)
                ) {
                    composable("beranda") {
                        HomeDashboardScreen(
                            docsState = docsState,
                            profileState = profileState,
                            cloudBackupsState = cloudBackupsState,
                            localBackups = localBackups,
                            userEmail = userEmail,
                            userName = userName,
                            onSelectMenuForGeneration = { docType ->
                                viewModel.selectDocTypeForGeneration(docType)
                                navigateToTab("generator")
                            },
                            onNavigateToGenerator = { navigateToTab("generator") },
                            onNavigateToCollection = { navigateToTab("koleksi") },
                            onNavigateToBackup = { navigateToTab("backup") }
                        )
                    }

                    composable("generator") {
                        GeneratorScreen(
                            formState = formState,
                            aiState = aiState,
                            profileState = profileState,
                            onUpdateForm = viewModel::updateForm,
                            onTogglePrinciple = viewModel::togglePrinciple,
                            onToggleDimension = viewModel::toggleDimension,
                            onGenerate = viewModel::generateDocument,
                            onCancel = viewModel::cancelGeneration,
                            onSaveDoc = { viewModel.saveGeneratedDocument() }
                        )
                    }

                    composable("koleksi") {
                        CollectionScreen(
                            docsState = docsState,
                            searchQuery = searchQuery,
                            filterDocType = filterDocType,
                            filterStarredOnly = filterStarredOnly,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onFilterDocTypeChange = viewModel::updateFilterDocType,
                            onToggleStarredFilter = viewModel::toggleFilterStarredOnly,
                            onToggleStarred = viewModel::toggleStarred,
                            onUpdateDoc = viewModel::updateDocument,
                            onDeleteDoc = viewModel::deleteDocument,
                            onNavigateToGenerator = { navigateToTab("generator") }
                        )
                    }

                    composable("backup") {
                        BackupAndProfileScreen(
                            userEmail = userEmail,
                            userName = userName,
                            profileState = profileState,
                            docsState = docsState,
                            backupLogsState = backupLogsState,
                            cloudBackupsState = cloudBackupsState,
                            localBackups = localBackups,
                            pendingRestoreBundle = pendingRestoreBundle,
                            isBusyBackup = isBusyBackup,
                            onSaveProfile = viewModel::saveTeacherProfile,
                            onBuildBackupJson = viewModel::buildBackupJson,
                            onLogExportSuccess = viewModel::logExportSuccess,
                            onCreateLocalBackup = viewModel::createLocalBackup,
                            onPreviewLocalRestore = viewModel::previewLocalBackupRestore,
                            onDeleteLocalBackup = viewModel::deleteLocalBackup,
                            onCreateCloudBackup = viewModel::createCloudBackup,
                            onPreviewCloudRestore = viewModel::previewCloudBackupRestore,
                            onDeleteCloudBackup = viewModel::deleteCloudBackup,
                            onParseRestoreJson = viewModel::parseRestoreJson,
                            onConfirmRestore = viewModel::confirmRestore,
                            onDismissRestore = viewModel::dismissRestorePreview,
                            onSignOut = {
                                signOut(credentialManager = credentialManager, scope = scope)
                            }
                        )
                    }
                }
            }
        }
    }
}
