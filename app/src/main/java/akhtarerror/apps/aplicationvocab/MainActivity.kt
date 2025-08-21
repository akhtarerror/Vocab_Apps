package akhtarerror.apps.aplicationvocab

import akhtarerror.apps.aplicationvocab.ui.history.HistoryFragment
import akhtarerror.apps.aplicationvocab.ui.home.HomeFragment
import akhtarerror.apps.aplicationvocab.ui.test.TestFragment
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import akhtarerror.apps.aplicationvocab.vocab.viewmodel.VocabViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var vocabViewModel: VocabViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize ViewModel early
        vocabViewModel = ViewModelProvider(this)[VocabViewModel::class.java]

        initViews()
        setupWindowInsets()
        setupToolbar()
        setupBottomNavigation()

        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            toolbar.title = "Vocabulary App"
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupWindowInsets() {
        // Apply window insets to main container
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Apply top padding for status bar
            v.setPadding(
                systemBars.left,
                0, // Don't apply top padding here, let AppBarLayout handle it
                systemBars.right,
                0  // Don't apply bottom padding here
            )

            insets
        }

        // Handle insets for bottom navigation specifically
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Apply bottom margin to bottomNavigation to avoid overlap with system navigation
            view.setPadding(0, 0, 0, navigationBars.bottom)

            insets
        }

        // Handle insets for fragment container
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragmentContainer)) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Apply bottom padding to fragment container to account for system navigation
            view.setPadding(0, 0, 0, navigationBars.bottom)

            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    toolbar.title = "Vocabulary App"
                    true
                }
                R.id.nav_test -> {
                    loadFragment(TestFragment())
                    toolbar.title = "Test"
                    true
                }
                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    toolbar.title = "History"
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Optional: Provide access to ViewModel for fragments if needed
    fun getVocabViewModel(): VocabViewModel = vocabViewModel
}