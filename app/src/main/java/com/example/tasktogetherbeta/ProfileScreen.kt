package com.example.tasktogetherbeta

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileScreen : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var profileGreetingText: TextView
    private lateinit var changeUsernameButton: Button
    private lateinit var usernameEditText: EditText
    private lateinit var confirmationButton: ImageButton
    private lateinit var deleteAccountButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_screen)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app").reference

        initViews()

        // Setup Toolbar and Drawer
        setupToolbarAndDrawer()

        // Load initial username and handle button actions
        fetchAndDisplayUsername()
        setupButtonListeners()
    }

    private fun initViews() {
        profileGreetingText = findViewById(R.id.profileGreetingText)
        changeUsernameButton = findViewById(R.id.changeUsernameButton)
        usernameEditText = findViewById(R.id.usernameEditText)
        confirmationButton = findViewById(R.id.confirmationButton)
        deleteAccountButton = findViewById(R.id.deleteAccountButton)

        // Hide username edit field and confirmation button initially
        usernameEditText.visibility = View.GONE
        confirmationButton.visibility = View.GONE
    }

    private fun setupToolbarAndDrawer() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close).apply {
            drawerArrowDrawable.color = resources.getColor(R.color.white) // Change text and drawer icon color
        }

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)
        updateNavHeader()
    }

    private fun setupButtonListeners() {
        changeUsernameButton.setOnClickListener { toggleUsernameEditTextVisibility() }
        confirmationButton.setOnClickListener { changeUsername() }
        deleteAccountButton.setOnClickListener { deleteAccount() }
    }

    private fun fetchAndDisplayUsername() {
        val currentUserUid = auth.currentUser?.uid ?: return
        database.child("users").child(currentUserUid).child("username").get().addOnSuccessListener { snapshot ->
            val username = snapshot.getValue(String::class.java) ?: return@addOnSuccessListener

            val greetingText = getString(R.string.profileDisplay, username)
            profileGreetingText.text = greetingText
        }
    }

    private fun toggleUsernameEditTextVisibility() {
        if (usernameEditText.visibility == View.GONE) {
            usernameEditText.visibility = View.VISIBLE
            confirmationButton.visibility = View.VISIBLE
            usernameEditText.alpha = 0f
            confirmationButton.alpha = 0f

            // Animate the fade-in
            usernameEditText.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(null)

            confirmationButton.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(null)

            changeUsernameButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_200))
        }
        else {
            // Animate the fade-out
            usernameEditText.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        usernameEditText.visibility = View.GONE
                    }
                })

            confirmationButton.animate()
                .alpha(0f)
                .setDuration(300)
                .setListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        confirmationButton.visibility = View.GONE
                    }
                })
            changeUsernameButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_primary_color))
        }
    }

    private fun changeUsername() {
        val newUsername = usernameEditText.text.toString().trim()
        val currentUserUid = auth.currentUser?.uid ?: return

        if (newUsername.isEmpty()) {
            CustomToast.show(this, "Username cannot be empty")
            return
        }

        // Retrieve the current username
        database.child("users").child(currentUserUid).child("username").get().addOnSuccessListener { snapshot ->
            val currentUsername = snapshot.getValue(String::class.java) ?: return@addOnSuccessListener

            if (newUsername == currentUsername) {
                CustomToast.show(this, "The new username cannot be the same as the current username.")
                return@addOnSuccessListener
            }

            if (!isValidUsername(newUsername)) {
                CustomToast.show(this, "Username must be lowercase letters, digits, and between 1-16 characters.")
                return@addOnSuccessListener
            }

            database.child("usernames").child(newUsername).get().addOnSuccessListener { usernameSnapshot ->
                if (usernameSnapshot.exists()) {
                    CustomToast.show(this, "Username already taken")
                }
                else {
                    // Update the username in the database
                    val userRef = database.child("users").child(currentUserUid)
                    userRef.child("username").setValue(newUsername).addOnSuccessListener {
                        database.child("usernames").child(newUsername).setValue(currentUserUid)

                        // Remove the old username from the usernames node
                        database.child("usernames").child(currentUsername).removeValue()

                        CustomToast.show(this, "Username updated successfully")

                        toggleUsernameEditTextVisibility()
                        fetchAndDisplayUsername()
                    }.addOnFailureListener {
                        CustomToast.show(this, "Failed to update username")
                    }
                }
            }.addOnFailureListener {
                CustomToast.show(this, "Failed to check username availability")
            }
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to retrieve current username")
        }
    }

    private fun isValidUsername(username: String): Boolean {
        val regex = "^[a-z0-9_]{1,16}\$".toRegex()
        return username.matches(regex)
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser ?: return
        val userUid = currentUser.uid

        AlertDialog.Builder(this).apply {
            setTitle("Delete Account")
            setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            setPositiveButton("Yes") { _, _ ->
                // Proceed with account deletion if the user confirms
                database.child("users").child(userUid).child("partner").get().addOnSuccessListener { partnerSnapshot ->
                    val partnerUid = partnerSnapshot.getValue(String::class.java)

                    if (!partnerUid.isNullOrEmpty()) {
                        // Set the partner's partner field to null
                        database.child("users").child(partnerUid).child("partner").setValue(null).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                proceedWithAccountDeletion(userUid, currentUser)
                            }
                            else {
                                CustomToast.show(this@ProfileScreen, "Failed to update partner status")
                            }
                        }
                    }
                    else {
                        proceedWithAccountDeletion(userUid, currentUser)
                    }
                }.addOnFailureListener {
                    CustomToast.show(this@ProfileScreen, "Failed to retrieve partner information")
                }
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            create()
            show()
        }
    }

    private fun proceedWithAccountDeletion(userUid: String, currentUser: FirebaseUser) {
        // First, retrieve the current username before deleting the user data
        database.child("users").child(userUid).child("username").get().addOnSuccessListener { usernameSnapshot ->
            val currentUsername = usernameSnapshot.getValue(String::class.java)

            // Retrieve all tasks of the current user
            database.child("users").child(userUid).child("tasks").get().addOnSuccessListener { taskSnapshot ->
                val userTasks = taskSnapshot.children.map { it.key.toString() }

                // Retrieve the partner UID
                database.child("users").child(userUid).child("partner").get().addOnSuccessListener { partnerSnapshot ->
                    val partnerUid = partnerSnapshot.getValue(String::class.java)

                    // Remove tasks and partner relationship if exists
                    if (!partnerUid.isNullOrEmpty()) {
                        val partnerRef = database.child("users").child(partnerUid)

                        // Get all tasks shared by the partner
                        partnerRef.child("tasks").get().addOnSuccessListener { partnerTaskSnapshot ->
                            val partnerTasks = partnerTaskSnapshot.children.map { it.key.toString() }
                            val sharedTasks = userTasks.intersect(partnerTasks.toSet())

                            // Remove tasks associated with both users
                            sharedTasks.forEach { taskId ->
                                database.child("tasks").child(taskId).removeValue()
                                database.child("users").child(userUid).child("tasks").child(taskId).removeValue()
                                database.child("users").child(partnerUid).child("tasks").child(taskId).removeValue()
                            }

                            // Remove the partner field from the partner's data
                            partnerRef.child("partner").removeValue()
                        }
                    }

                    // Remove the user's data
                    database.child("users").child(userUid).removeValue().addOnSuccessListener {
                        // Remove the username from the "usernames" node
                        if (!currentUsername.isNullOrEmpty()) {
                            database.child("usernames").child(currentUsername).removeValue().addOnSuccessListener {
                                // Proceed to delete the Firebase auth account
                                currentUser.delete().addOnSuccessListener {
                                    CustomToast.show(this@ProfileScreen, "Account deleted successfully")
                                    val intent = Intent(this@ProfileScreen, LoginScreen::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(intent)
                                    finish()
                                }.addOnFailureListener {
                                    CustomToast.show(this@ProfileScreen, "Failed to delete account")
                                }
                            }.addOnFailureListener {
                                CustomToast.show(this@ProfileScreen, "Failed to remove username")
                            }
                        }
                    }.addOnFailureListener {
                        CustomToast.show(this@ProfileScreen, "Failed to remove user data")
                    }
                }
            }.addOnFailureListener {
                CustomToast.show(this@ProfileScreen, "Failed to retrieve tasks")
            }
        }.addOnFailureListener {
            CustomToast.show(this@ProfileScreen, "Failed to retrieve username")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                Log.d("MainActivity", "Home selected")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.nav_profile -> {
                drawerLayout.closeDrawer(GravityCompat.START)
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        else {
            super.onBackPressed()
        }
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
}