package app.dapk.st.home

import android.os.Bundle
import androidx.activity.compose.setContent
import app.dapk.st.core.DapkActivity
import app.dapk.st.core.module
import app.dapk.st.core.viewModel
import app.dapk.st.directory.DirectoryModule
import app.dapk.st.login.LoginModule
import app.dapk.st.profile.ProfileModule

class MainActivity : DapkActivity() {

    private val directoryViewModel by viewModel { module<DirectoryModule>().directoryViewModel() }
    private val loginViewModel by viewModel { module<LoginModule>().loginViewModel() }
    private val profileViewModel by viewModel { module<ProfileModule>().profileViewModel() }
    private val homeViewModel by viewModel { module<HomeModule>().homeViewModel(directoryViewModel, loginViewModel, profileViewModel) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeScreen(homeViewModel)
        }
    }
}

