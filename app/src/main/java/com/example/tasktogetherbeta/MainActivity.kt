package com.example.tasktogetherbeta

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    TaskCreationDialogFragment.TaskCreationListener, TaskEditDialogFragment.TaskEditListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var navView: NavigationView
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskManager: TaskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_screen)

        auth = Firebase.auth
        taskManager = TaskManager(this)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize the notification manager: NotificationManagerUtil(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        initializeDrawer()
        initializeSpinner()
        initializeSearchView()
        initializeRecyclerView()
        initializeButtons()
        checkPartnerStatus()
        refreshTasks()
    }

    private fun initializeDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
        updateNavHeader()
    }

    private fun initializeSpinner() {
        val sortingSpinner: Spinner = findViewById(R.id.sortSpinner)
        val sortOptions = listOf(
            "Due Date (Asc)", "Due Date (Desc)",
            "Title (A-Z)", "Title (Z-A)",
            "Priority (High)", "Priority (Low)"
        )
        val adapter = CustomSpinnerAdapter(this, R.layout.spinner_item, sortOptions)
        sortingSpinner.adapter = adapter

        sortingSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                refreshTasks() // Refresh tasks based on selected sorting option
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }
    }

    private fun initializeSearchView() {
        val searchView: SearchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false // Not needed here so just false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { searchTasksByTitle(it) }
                return true
            }
        })
    }

    private fun initializeRecyclerView() {
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)

        val taskAdapter = TaskAdapter(emptyList(), { task ->
            taskManager.deleteTask(task) { refreshTasks() } }, { task ->
            taskManager.completeTask(task) { refreshTasks() } }, { task ->
            taskManager.reactivateTask(task) { refreshTasks() }
        })

        tasksRecyclerView.adapter = taskAdapter
    }

    private fun initializeButtons() {
        val addPartnerButton = findViewById<Button>(R.id.addPartnerButton)
        val createTaskButton = findViewById<Button>(R.id.createTaskButton)

        addPartnerButton.setOnClickListener {
            val intent = Intent(this, PartnerStatusScreen::class.java)
            startActivity(intent)
        }

        createTaskButton.setOnClickListener {
            val taskCreationDialog = TaskCreationDialogFragment()
            taskCreationDialog.show(supportFragmentManager, TASK_CREATION_DIALOG_TAG)
        }
    }

    // Handle partner status
    private fun checkPartnerStatus() {
        val addPartnerButton = findViewById<Button>(R.id.addPartnerButton)
        val createTaskButton = findViewById<Button>(R.id.createTaskButton)
        val partnerMissingText = findViewById<TextView>(R.id.partnerMissingNotice)

        checkPartnerStatus(addPartnerButton, createTaskButton, partnerMissingText)
    }

    companion object {
        private const val TASK_CREATION_DIALOG_TAG = "TaskCreationDialog"
    }

    override fun onTaskCreated() {
        refreshTasks()
    }

    override fun onTaskEdited() {
        refreshTasks()
    }

    private fun refreshTasks() {
        val noTasksYetText = findViewById<TextView>(R.id.noTasksYetNotice)
        val sortingSpinner: Spinner = findViewById(R.id.sortSpinner)
        val spinnerThing: LinearLayout = findViewById(R.id.spinnerThing)

        // Map the spinner position to sorting logic
        val sortBy = when (sortingSpinner.selectedItemPosition) {
            0 -> "DueDateAsc"
            1 -> "DueDateDesc"
            2 -> "TitleAsc"
            3 -> "TitleDesc"
            4 -> "PriorityDesc"
            5 -> "PriorityAsc"
            else -> "DueDateAsc"   // Default sorting
        }

        // Fetch tasks based on the selected sorting method
        taskManager.fetchTasks({ tasks ->
            tasksRecyclerView.adapter = TaskAdapter(tasks, { task ->
                taskManager.deleteTask(task) { refreshTasks() } }, { task ->
                taskManager.completeTask(task) { refreshTasks() } }, { task ->
                taskManager.reactivateTask(task) { refreshTasks() }
            })

            noTasksYetText.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            spinnerThing.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
        }, sortBy)
    }

    private fun searchTasksByTitle(query: String) {
        taskManager.fetchTasks({ tasks ->
            // Filter tasks based on the query string
            val filteredTasks = tasks.filter { it.title.contains(query, ignoreCase = true) }

            // Update the RecyclerView with the filtered tasks
            tasksRecyclerView.adapter = TaskAdapter(filteredTasks, { task ->
                taskManager.deleteTask(task) { refreshTasks() } }, { task ->
                taskManager.completeTask(task) { refreshTasks() } }, { task ->
                taskManager.reactivateTask(task) { refreshTasks() }
            })
        }, sortBy = "DueDateAsc")
    }

    override fun onResume() {
        super.onResume()

        val addPartnerButton = findViewById<Button>(R.id.addPartnerButton)
        val createTaskButton = findViewById<Button>(R.id.createTaskButton)
        val partnerMissingText = findViewById<TextView>(R.id.partnerMissingNotice)

        // Re-check the partner status every time the activity is resumed
        checkPartnerStatus(addPartnerButton, createTaskButton, partnerMissingText)
    }

    private fun checkPartnerStatus(addPartnerButton: Button, createTaskButton: Button, partnerMissingText: TextView) {
        val currentUserUid = auth.currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app")
        val userRef = database.getReference("users/$currentUserUid")

        userRef.child("partner").get().addOnSuccessListener { snapshot ->
            val partnerUid = snapshot.value as String?

            if (partnerUid.isNullOrEmpty()) {
                addPartnerButton.visibility = View.VISIBLE
                partnerMissingText.visibility = View.VISIBLE
                createTaskButton.visibility = View.GONE
            }
            else {
                addPartnerButton.visibility = View.GONE
                partnerMissingText.visibility = View.GONE
                createTaskButton.visibility = View.VISIBLE
            }
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to check partner status")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                Log.d("MainActivity", "Home selected")
            }
            R.id.nav_profile -> {
                Log.d("MainActivity", "Profile selected")
                val intent = Intent(this, ProfileScreen::class.java)
                startActivity(intent)
            }
            R.id.nav_partner -> {
                Log.d("MainActivity", "Partner Status selected")
                val intent = Intent(this, PartnerStatusScreen::class.java)
                startActivity(intent)
            }
            R.id.nav_completed -> {
                Log.d("MainActivity", "Completed Tasks selected")
                val intent = Intent(this, CompletedTasksScreen::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                auth.signOut()
                val intent = Intent(this, LoginScreen::class.java)
                startActivity(intent)
                finish()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun updateNavHeader() {
        val headerView = navView.getHeaderView(0)
        val usernameDisplay = headerView.findViewById<TextView>(R.id.usernameDisplay)
        val emailDisplay = headerView.findViewById<TextView>(R.id.emailDisplay)

        val currentUser = auth.currentUser
        currentUser?.let {
            val userId = currentUser.uid
            val database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app")
            val usersRef = database.getReference("users").child(userId)

            // Retrieve the user's data
            usersRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val username = snapshot.child("username").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)

                    usernameDisplay.text = username ?: "Username not found"
                    emailDisplay.text = email ?: "Email not found"
                }
                else {
                    CustomToast.show(this, "User data not found")
                }
            }.addOnFailureListener {
                CustomToast.show(this, "Failed to retrieve user data")
            }
        }

        val profileIntent = Intent(baseContext, ProfileScreen::class.java)
        usernameDisplay.setOnClickListener {
            startActivity(profileIntent)
        }
        emailDisplay.setOnClickListener {
            startActivity(profileIntent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        else {
            super.onBackPressed()
        }
    }
}