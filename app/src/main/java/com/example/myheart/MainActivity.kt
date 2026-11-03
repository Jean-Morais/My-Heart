package com.example.myheart

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myheart.data.sensor.ble.BleSensorDataSource
import com.example.myheart.data.sensor.ble.SharedPreferencesLastDeviceStore
import com.example.myheart.domain.usecase.AlertOnAbnormalReadingUseCase
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.domain.usecase.PrepareFamilyAlertUseCase
import com.example.myheart.ui.MyHeartApp
import com.example.myheart.ui.MyHeartViewModel
import com.example.myheart.ui.history.HistoryViewModel
import com.example.myheart.ui.profile.ProfileViewModel
import com.example.myheart.ui.theme.MyHeartTheme

class MainActivity : ComponentActivity() {

    // O banco e os repositórios vivem na Application; aqui só os pegamos.
    private val app get() = application as MyHeartApplication

    // Injeção de dependência manual: é aqui que se escolhe a fonte de dados do sensor
    // (BleSensorDataSource = Bluetooth real; FakeSensorDataSource = demonstração sem hardware).
    private val viewModel: MyHeartViewModel by viewModels {
        viewModelFactory {
            initializer {
                MyHeartViewModel(
                    sensor = BleSensorDataSource(
                        context = applicationContext,
                        lastDeviceStore = SharedPreferencesLastDeviceStore(applicationContext),
                    ),
                    analyzeReading = AnalyzeReadingUseCase(),
                    repository = app.readingRepository,
                    alertOnAbnormalReading = AlertOnAbnormalReadingUseCase(
                        profileRepository = app.profileRepository,
                        notifier = app.alertNotifier,
                    ),
                    prepareFamilyAlert = PrepareFamilyAlertUseCase(app.profileRepository),
                )
            }
        }
    }

    private val historyViewModel: HistoryViewModel by viewModels {
        viewModelFactory {
            initializer {
                HistoryViewModel(
                    repository = app.readingRepository,
                    analyzeReading = AnalyzeReadingUseCase(),
                )
            }
        }
    }

    private val profileViewModel: ProfileViewModel by viewModels {
        viewModelFactory {
            initializer {
                ProfileViewModel(repository = app.profileRepository)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O design é claro: ícones escuros nas barras do sistema, mesmo com o celular em modo escuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            MyHeartTheme {
                MyHeartApp(
                    viewModel = viewModel,
                    historyViewModel = historyViewModel,
                    profileViewModel = profileViewModel,
                )
            }
        }
    }
}
