package mega.privacy.android.app.presentation.login

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.ActivityLoginBinding
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.toConstant
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmEmailFragment
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountComposeFragment
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.onboarding.TourFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.account.AccountBlockedDetail
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import timber.log.Timber
import javax.inject.Inject

/**
 * Login Activity.
 *
 * @property chatRequestHandler       [MegaChatRequestHandler]
 */
@AndroidEntryPoint
class LoginActivity : BaseActivity() {

    @Inject
    lateinit var chatRequestHandler: MegaChatRequestHandler

    private val viewModel by viewModels<LoginViewModel>()

    private lateinit var binding: ActivityLoginBinding

    private var cancelledConfirmationProcess = false

    //Fragments
    private var loginFragment: LoginFragment? = null

    private var visibleFragment = 0
    private var emailTemp: String? = null
    private var passwdTemp: String? = null
    private var firstNameTemp: String? = null
    private var lastNameTemp: String? = null

    /**
     * Flag to delay showing the splash screen.
     */
    private var keepShowingSplashScreen = true

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Timber.d("onBackPressed")
            retryConnectionsAndSignalPresence()

            when (visibleFragment) {
                Constants.CREATE_ACCOUNT_FRAGMENT -> showFragment(Constants.TOUR_FRAGMENT)
                Constants.TOUR_FRAGMENT, Constants.CONFIRM_EMAIL_FRAGMENT -> finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        visibleFragment = intent.getIntExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)

        if (visibleFragment == Constants.LOGIN_FRAGMENT) {
            loginFragment = LoginFragment()
        }

        showFragment(visibleFragment)
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        chatRequestHandler.setIsLoggingRunning(false)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            keepShowingSplashScreen
        }
        super.onCreate(savedInstanceState)

        if (intent.action == Intent.ACTION_MAIN
            && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
            && !viewModel.isConnected
        ) {
            // in case offline mode, go to ManagerActivity
            stopShowingSplashScreen()
            startActivity(Intent(this, ManagerActivity::class.java))
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
        chatRequestHandler.setIsLoggingRunning(true)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupSplashExitAnimation(splashScreen)
        setupObservers()
        lifecycleScope.launch {
            if (savedInstanceState != null) {
                Timber.d("Bundle is NOT NULL")
                visibleFragment =
                    savedInstanceState.getInt(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
            } else {
                visibleFragment =
                    intent?.getIntExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                        ?: Constants.LOGIN_FRAGMENT
                Timber.d("There is an intent! VisibleFragment: %s", visibleFragment)
            }

            viewModel.getEphemeral()?.let {
                visibleFragment = Constants.CONFIRM_EMAIL_FRAGMENT
                emailTemp = it.email
                passwdTemp = it.password
                firstNameTemp = it.firstName
                lastNameTemp = it.lastName
                resumeCreateAccount(it.session.orEmpty())
            } ?: run {
                if (!intent.hasExtra(Constants.VISIBLE_FRAGMENT) && savedInstanceState == null) {
                    val session = viewModel.getSession()
                    if (session.isNullOrEmpty()) {
                        visibleFragment = Constants.TOUR_FRAGMENT
                    }
                }
            }

            if (visibleFragment != Constants.LOGIN_FRAGMENT) {
                stopShowingSplashScreen()
            }
            val currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container_login)
            if (savedInstanceState == null || currentFragment == null) {
                // when savedInstanceState is different from null, the activity system automatically restores the last fragment
                // so we need to show the fragment again
                showFragment(visibleFragment)
            } else {
                loginFragment = currentFragment as? LoginFragment
            }

            // A fail-safe to avoid the splash screen to be shown forever
            // in case not called by expected fragments
            delay(1500)
            if (keepShowingSplashScreen) {
                stopShowingSplashScreen()
                Timber.w("Splash screen is being shown for too long")
            }
        }
    }

    /**
     * Disables the splash screen exit animation to prevent a visual "jump" of the app icon.
     *
     * Skipped on Android 13 (Tiramisu) for certain Chinese OEMs (e.g., OPPO, Realme, OnePlus),
     * or specific models (e.g., Galaxy A03 Core), as it may cause crashes.
     * See: https://issuetracker.google.com/issues/242118185
     */
    private fun setupSplashExitAnimation(splashScreen: SplashScreen) {
        val isAndroid13 = Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU
        val isAffectedBrand = Build.BRAND.lowercase() in setOf("oppo", "realme", "oneplus")
        val isAffectedModel = Build.MODEL.lowercase().contains("a03 core")

        if (isAndroid13 && (isAffectedBrand || isAffectedModel)) return

        splashScreen.setOnExitAnimationListener {
            it.remove()
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    /**
     * Stops showing the splash screen.
     */
    fun stopShowingSplashScreen() {
        keepShowingSplashScreen = false
    }

    private fun setupObservers() {
        collectFlow(viewModel.state, Lifecycle.State.RESUMED) { uiState ->
            with(uiState) {
                when {
                    isPendingToFinishActivity -> finish()
                    isPendingToShowFragment != null -> {
                        showFragment(isPendingToShowFragment.toConstant())
                        viewModel.isPendingToShowFragmentConsumed()
                    }

                    loginException != null -> {
                        if (loginException is LoginLoggedOutFromOtherLocation) {
                            showAlertLoggedOut()
                            viewModel.setLoginErrorConsumed()
                        }
                    }
                }
            }
        }

        collectFlow(viewModel.state.mapNotNull { it.isLoginNewDesignEnabled }) { isLoginNewDesignEnabled ->
            if (isLoginNewDesignEnabled) {
                restrictOrientation()
            }
        }
    }

    /**
     * Shows a snackbar.
     *
     * @param message Message to show.
     */
    fun showSnackbar(message: String) = showSnackbar(binding.relativeContainerLogin, message)

    /**
     * Show fragment
     *
     * @param fragmentType
     */
    fun showFragment(fragmentType: LoginFragmentType) {
        viewModel.setPendingFragmentToShow(fragmentType)
    }

    /**
     * Shows a fragment.
     *
     * @param visibleFragment The fragment to show.
     */
    private fun showFragment(visibleFragment: Int) {
        Timber.d("visibleFragment: %s", visibleFragment)
        this.visibleFragment = visibleFragment
        restrictOrientation()

        when (visibleFragment) {
            Constants.LOGIN_FRAGMENT -> {
                Timber.d("Show LOGIN_FRAGMENT")
                if (loginFragment == null) {
                    loginFragment = LoginFragment()
                }

                if (passwdTemp != null && emailTemp != null) {
                    viewModel.setTemporalCredentials(email = emailTemp, password = passwdTemp)
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, loginFragment ?: return)
                    .commitNowAllowingStateLoss()
            }

            Constants.CREATE_ACCOUNT_FRAGMENT -> {
                Timber.d("Show CREATE_ACCOUNT_FRAGMENT")
                val createActFragment =
                    CreateAccountComposeFragment()

                if (cancelledConfirmationProcess) {
                    cancelledConfirmationProcess = false
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, createActFragment)
                    .commitNowAllowingStateLoss()
            }

            Constants.TOUR_FRAGMENT -> {
                Timber.d("Show TOUR_FRAGMENT")
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_login, TourFragment())
                    .commitNowAllowingStateLoss()
            }

            Constants.CONFIRM_EMAIL_FRAGMENT -> {
                val confirmEmailFragment =
                    ConfirmEmailFragment.newInstance(emailTemp, firstNameTemp)

                with(supportFragmentManager) {
                    beginTransaction()
                        .replace(R.id.fragment_container_login, confirmEmailFragment)
                        .commitNowAllowingStateLoss()
                }
            }
        }
        if ((application as MegaApplication).isEsid) {
            showAlertLoggedOut()
        }
    }

    /**
     * Restrict to portrait mode always for mobile devices and tablets (already restricted via Manifest).
     * Allow the landscape mode only for tablets and only for TOUR_FRAGMENT.
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun restrictOrientation() {
        val isLoginNewDesignEnabled = viewModel.state.value.isLoginNewDesignEnabled == true
        requestedOrientation =
            if (isLoginNewDesignEnabled || visibleFragment == Constants.TOUR_FRAGMENT || visibleFragment == Constants.CREATE_ACCOUNT_FRAGMENT) {
                Timber.d("Tour/create account screen landscape mode allowed")
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            } else {
                Timber.d("Other screens landscape mode restricted")
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
    }

    override fun shouldSetStatusBarTextColor() = false

    /**
     * Shows a warning informing the account has been logged out.
     */
    private fun showAlertLoggedOut() {
        Timber.d("showAlertLoggedOut")
        (application as MegaApplication).isEsid = false

        if (!isFinishing) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_alert_logged_out))
                .setMessage(getString(R.string.error_server_expired_session))
                .setPositiveButton(getString(R.string.general_ok), null)
                .show()
        }
    }

    public override fun onResume() {
        Timber.d("onResume")
        super.onResume()
        Util.setAppFontSize(this)

        if (intent == null) return

        if (intent?.action != null) {
            when (intent.action) {
                Constants.ACTION_CANCEL_CAM_SYNC -> showCancelCUWarning()
            }
        }
    }

    private fun showCancelCUWarning() {
        Timber.d("ACTION_CANCEL_CAM_SYNC")
        val title = getString(R.string.cam_sync_syncing)
        val text = getString(R.string.cam_sync_cancel_sync)

        Util.getCustomAlertBuilder(this, title, text, null)
            .setPositiveButton(getString(R.string.general_yes)) { _, _ ->
                viewModel.stopCameraUploads()
            }.setNegativeButton(getString(R.string.general_no), null)
            .show()
    }

    /**
     * Sets the received string as temporal email.
     *
     * @param emailTemp The temporal email.
     */
    fun setTemporalEmail(emailTemp: String) {
        this.emailTemp = emailTemp
        viewModel.setTemporalEmail(emailTemp)
    }

    private fun resumeCreateAccount(session: String) {
        lifecycleScope.launch {
            runCatching {
                Timber.d("resume create account")
                viewModel.resumeCreateAccount(session)
            }.onFailure {
                Timber.e(it)
                if (it !is CancellationException) {
                    cancelConfirmationAccount()
                }
            }
        }
    }

    /**
     * Cancels the account confirmation.
     */
    fun cancelConfirmationAccount() {
        Timber.d("cancelConfirmationAccount")
        viewModel.clearEphemeral()
        viewModel.clearUserCredentials()
        cancelledConfirmationProcess = true
        passwdTemp = null
        emailTemp = null
        viewModel.setTourAsPendingFragment()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        Timber.d("onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putInt(Constants.VISIBLE_FRAGMENT, visibleFragment)
    }

    /**
     * Sets temporal data for account creation.
     *
     * @param email    Email.
     * @param name     First name.
     * @param lastName Last name.
     * @param password Password.
     */
    fun setTemporalDataForAccountCreation(
        email: String,
        name: String,
        lastName: String,
        password: String,
    ) {
        setTemporalEmail(email)
        firstNameTemp = name
        lastNameTemp = lastName
        passwdTemp = password
        viewModel.setIsWaitingForConfirmAccount()
    }

    fun showAccountBlockedDialog(accountBlockedDetail: AccountBlockedDetail) {
        if (visibleFragment == Constants.LOGIN_FRAGMENT) {
            loginFragment?.showAccountBlockedDialog(accountBlockedDetail)
        }
    }

    companion object {

        /**
         * Flag for knowing if it was already in the login page.
         */
        @JvmField
        var isBackFromLoginPage = false

        /**
         * Intent extra for knowing if the user is logged in.
         */
        const val EXTRA_IS_LOGGED_IN = "isLoggedIn"
    }
}
