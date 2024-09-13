package com.example.tasktogetherbeta

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class CompletedTasksScreen : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var navView: NavigationView
    private lateinit var completedTasksRecyclerView: RecyclerView
    private lateinit var taskManager: TaskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completed_tasks_screen)

        auth = Firebase.auth
        taskManager = TaskManager(this)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.white)    //Change text and drawer icon colour
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        updateNavHeader()

        completedTasksRecyclerView = findViewById(R.id.completedTasksRecyclerView)
        completedTasksRecyclerView.layoutManager = LinearLayoutManager(this)

        completedTasksRecyclerView.adapter = TaskAdapter(emptyList(), { task ->
            taskManager.deleteTask(task) { refreshCompletedTasks() } }, { task ->
            taskManager.completeTask(task) { refreshCompletedTasks() } }, { task ->
            taskManager.reactivateTask(task) { refreshCompletedTasks() }
        })

        refreshCompletedTasks()
    }

    private fun refreshCompletedTasks() {
        val noCompletedTasksText = findViewById<TextView>(R.id.noCompletedTasksNotice)
        val completedTasksScreenText = findViewById<TextView>(R.id.completedTasksScreenText)

        taskManager.fetchCompletedTasks { tasks ->
            completedTasksRecyclerView.adapter = TaskAdapter(tasks, { task ->
                taskManager.deleteTask(task) { refreshCompletedTasks() } }, { task ->
                taskManager.completeTask(task) { refreshCompletedTasks() } }, { task ->
                taskManager.reactivateTask(task) { refreshCompletedTasks() }
            })
            noCompletedTasksText.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
            completedTasksScreenText.visibility = if (tasks.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCompletedTasks()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                Log.d("CompletedTasksScreen", "Home selected")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Finish this activity so it's removed from the back stack
            }
            R.id.nav_profile -> {
                Log.d("CompletedTasksScreen", "Profile selected")
                val intent = Intent(this, ProfileScreen::class.java)
                startActivity(intent)
            }
            R.id.nav_partner -> {
                Log.d("CompletedTasksScreen", "Partner Status selected")
                val intent = Intent(this, PartnerStatusScreen::class.java)
                startActivity(intent)
            }
            R.id.nav_completed -> {
                drawerLayout.closeDrawer(GravityCompat.START) // Do nothing, we are already here
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