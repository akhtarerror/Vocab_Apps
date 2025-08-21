package akhtarerror.apps.aplicationvocab

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Handle window insets properly
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Only apply top padding to avoid affecting bottom navigation
            v.setPadding(systemBars.left, 0, systemBars.right, 0)

            // Apply bottom padding to bottom navigation
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)

            insets
        }

        initViews()
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
}