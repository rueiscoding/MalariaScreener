package com.example.todo
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.todo.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fragmentHome = HomeFragment()
    private val fragmentList = ListFragment()
    private val fragmentCamera = CameraFragment()
    private var activeFragment: Fragment = fragmentHome // initial fragment is home page

    lateinit var bottomNavigationView: BottomNavigationView

    /*
    Called when the activity is first created.
    Initializes the fragments, which are managed by FragmentManager.
    Sets up the bottom navigation bar, which allows users to toggle between fragments.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        // initialize fragments and add them to fragment manager
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, fragmentList, "ListFragment")
            .hide(fragmentList)
            .commit()

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, fragmentCamera, "CameraFragment")
            .hide(fragmentCamera)
            .commit()

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, fragmentHome, "HomeFragment")
            .commit()

        // navigation item listener
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.homeFragment -> {
                    showFragment(fragmentHome)
                    true
                }
                R.id.cameraFragment -> {
                    showFragment(fragmentCamera)
                    true
                }
                R.id.listFragment -> {
                    showFragment(fragmentList)
                    true
                }
                else -> false
            }
        }

    }

    /*
    Function to show a given fragment.
    Hides the currently activeFragment,
    shows the selected fragment,
    and sets the "activeFragment" equal to "fragment"
     */
    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        activeFragment = fragment
    }

    /*
    Navigate to the Camera page
     */
    fun navigateToCamera(){
        showFragment(fragmentCamera)
        bottomNavigationView.selectedItemId = R.id.cameraFragment
    }

    /*
    Navigate to the List page and launch photolibrary
     */
    fun navigateToList(){
        showFragment(fragmentList)
        (fragmentList as ListFragment).launchPhotoLibrary()
        bottomNavigationView.selectedItemId = R.id.listFragment
    }


    // old functions
    fun showInfoFragment() {
        bottomNavigationView.visibility = View.GONE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, InfoFragment())
            .addToBackStack(null)
            .commit()
    }

    fun hideInfoFragment() {
        bottomNavigationView.visibility = View.VISIBLE
        supportFragmentManager.popBackStack()
    }
}