package com.example.tasktogetherbeta

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class PartnerStatusScreen : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var findYourPartnerText: TextView
    private lateinit var partnerSearchBar: EditText
    private lateinit var addPartnerButton: Button
    private lateinit var currentPartnerDisplay: TextView
    private lateinit var removePartnerButton: Button
    private lateinit var receivedInvitationText: TextView
    private lateinit var acceptInvitationButton: ImageButton
    private lateinit var rejectInvitationButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_partner_status_screen)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app").reference

        initViews()
        setupToolbarAndDrawer()
        setupButtonListeners()

        handleIncomingRequest()
    }

    private fun initViews() {
        findYourPartnerText = findViewById(R.id.findYourPartnerText)
        partnerSearchBar = findViewById(R.id.partnerSearchBar)
        addPartnerButton = findViewById(R.id.addPartnerButton)
        currentPartnerDisplay = findViewById(R.id.currentPartnerDisplay)
        removePartnerButton = findViewById(R.id.removePartnerButton)
        receivedInvitationText = findViewById(R.id.receivedInvitationText)
        acceptInvitationButton = findViewById(R.id.acceptInvitationButton)
        rejectInvitationButton = findViewById(R.id.rejectInvitationButton)

        // Set initial visibility
        setInitialVisibility()
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
            drawerArrowDrawable.color = resources.getColor(R.color.white) //Change text and drawer icon colour
        }

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)
        updateNavHeader()
    }

    private fun setInitialVisibility() {
        findYourPartnerText.visibility = View.VISIBLE
        partnerSearchBar.visibility = View.VISIBLE
        addPartnerButton.visibility = View.VISIBLE

        currentPartnerDisplay.visibility = View.GONE
        removePartnerButton.visibility = View.GONE
        receivedInvitationText.visibility = View.GONE
        acceptInvitationButton.visibility = View.GONE
        rejectInvitationButton.visibility = View.GONE
    }

    private fun setupButtonListeners() {
        addPartnerButton.setOnClickListener {
            val enteredUsername = partnerSearchBar.text.toString()
            hideKeyboard()
            sendPartnerRequest(enteredUsername)
        }

        removePartnerButton.setOnClickListener {
            val currentUserUid = auth.currentUser?.uid ?: return@setOnClickListener
            removePartner(currentUserUid)
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(partnerSearchBar.windowToken, 0)
    }

    private fun handleIncomingRequest() {
        val currentUserUid = auth.currentUser?.uid ?: return
        val userRef = database.child("users").child(currentUserUid)

        // Check if the user has an incoming request
        userRef.child("incomingRequest").get().addOnSuccessListener { snapshot ->
            val incomingRequestUid = snapshot.value as String?
            if (incomingRequestUid.isNullOrEmpty()) {
                checkPartnerStatus(userRef)
            }
            else {
                displayIncomingRequest(incomingRequestUid, userRef)
            }
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to check request status")
        }
    }

    private fun checkPartnerStatus(userRef: DatabaseReference) {
        // No incoming request, proceed to check if the user has a partner
        userRef.child("partner").get().addOnSuccessListener { partnerSnapshot ->
            val partnerUid = partnerSnapshot.value as String?
            if (partnerUid.isNullOrEmpty()) {
                setInitialVisibility()
            }
            else {
                displayCurrentPartner(partnerUid)
            }
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to check partner status")
        }
    }

    private fun displayIncomingRequest(incomingRequestUid: String, userRef: DatabaseReference) {
        // Retrieve the username of the incoming request's user
        val partnerRef = database.child("users").child(incomingRequestUid)
        partnerRef.child("username").get().addOnSuccessListener { usernameSnapshot ->
            val partnerUsername = usernameSnapshot.value as String?
            if (!partnerUsername.isNullOrEmpty()) {
                val requestText = getString(R.string.request_text, partnerUsername)
                receivedInvitationText.text = requestText

                receivedInvitationText.visibility = View.VISIBLE
                acceptInvitationButton.visibility = View.VISIBLE
                rejectInvitationButton.visibility = View.VISIBLE

                findYourPartnerText.visibility = View.GONE
                partnerSearchBar.visibility = View.GONE
                addPartnerButton.visibility = View.GONE
                currentPartnerDisplay.visibility = View.GONE
                removePartnerButton.visibility = View.GONE

                acceptInvitationButton.setOnClickListener {
                    acceptRequest(incomingRequestUid, userRef, partnerRef)
                }

                rejectInvitationButton.setOnClickListener {
                    rejectRequest(userRef)
                }
            }
            else {
                CustomToast.show(this, "Failed to retrieve partner's username")
            }
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to retrieve partner information")
        }
    }

    private fun acceptRequest(incomingRequestUid: String, userRef: DatabaseReference, partnerRef: DatabaseReference) {
        val currentUserUid = auth.currentUser?.uid ?: return

        partnerRef.child("partner").setValue(currentUserUid).addOnSuccessListener {
            userRef.child("partner").setValue(incomingRequestUid).addOnSuccessListener {
                // Clear the incoming request field and refresh UI
                userRef.child("incomingRequest").removeValue()
                CustomToast.show(this, "Partnership request has been accepted.")
                recreate()
            }.addOnFailureListener {
                CustomToast.show(this, "Failed to set partner for the current user")
            }
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to set partner for the requesting user")
        }
    }

    private fun rejectRequest(userRef: DatabaseReference) {
        userRef.child("incomingRequest").removeValue().addOnSuccessListener {
            CustomToast.show(this, "Partnership request has been rejected.")
            recreate()
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to remove incoming request")
        }
    }

    private fun displayCurrentPartner(partnerUid: String) {
        val partnerRef = database.child("users").child(partnerUid)
        partnerRef.child("username").get().addOnSuccessListener { usernameSnapshot ->
            val partnerUsername = usernameSnapshot.value as String?
            if (!partnerUsername.isNullOrEmpty()) {
                currentPartnerDisplay.text = getString(R.string.current_partner_display, partnerUsername)
            }

            findYourPartnerText.visibility = View.GONE
            partnerSearchBar.visibility = View.GONE
            addPartnerButton.visibility = View.GONE

            currentPartnerDisplay.visibility = View.VISIBLE
            removePartnerButton.visibility = View.VISIBLE
            receivedInvitationText.visibility = View.GONE
            acceptInvitationButton.visibility = View.GONE
            rejectInvitationButton.visibility = View.GONE
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to retrieve partner's username")
        }
    }

    private fun sendPartnerRequest(username: String) {
        val currentUserUid = auth.currentUser?.uid ?: return
        val usernamesRef = database.child("usernames")
        val usersRef = database.child("users")

        // Step 1: Search for the userID of the target user given the username
        usernamesRef.child(username).get().addOnSuccessListener { snapshot ->
            val targetUserUid = snapshot.value as String?
            if (!targetUserUid.isNullOrEmpty()) {
                if (targetUserUid == currentUserUid) {
                    CustomToast.show(this, "You cannot send a request to yourself")
                    return@addOnSuccessListener
                }

                // Step 2: Check if the target user already has an incoming request or a partner
                val targetUserRef = usersRef.child(targetUserUid)
                targetUserRef.get().addOnSuccessListener { targetUserSnapshot ->
                    val incomingRequest = targetUserSnapshot.child("incomingRequest").value as String?
                    val partnerUid = targetUserSnapshot.child("partner").value as String?

                    when {
                        !incomingRequest.isNullOrEmpty() -> {
                            CustomToast.show(this, "This user already has a pending partnership request")
                        }
                        !partnerUid.isNullOrEmpty() -> {
                            CustomToast.show(this, "This user already has a partner")
                        }
                        // Step 3: Send the partner request by updating the target user's "incomingRequest" field
                        else -> {
                            targetUserRef.child("incomingRequest").setValue(currentUserUid).addOnSuccessListener {
                                CustomToast.show(this, "Partner request sent to $username")
                            }.addOnFailureListener {
                                CustomToast.show(this, "Failed to send partner request")
                            }
                        }
                    }
                }.addOnFailureListener {
                    CustomToast.show(this, "Failed to retrieve user data")
                }
            }
            else {
                CustomToast.show(this, "Username not found")
            }
        }.addOnFailureListener {
            CustomToast.show(this, "Failed to search for user")
        }
    }

    private fun removePartner(currentUserUid: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Partner")
            .setMessage("Are you sure you want to remove your partner? All your current tasks will be deleted.")
            .setPositiveButton("Yes") { dialog, _ ->
                val userRef = database.child("users").child(currentUserUid)

                userRef.child("partner").get().addOnSuccessListener { snapshot ->
                    val partnerUid = snapshot.value as String?
                    if (!partnerUid.isNullOrEmpty()) {
                        removeSharedTasks(currentUserUid, partnerUid)
                    }
                    else {
                        CustomToast.show(this, "You do not have a partner")
                    }
                }.addOnFailureListener {
                    CustomToast.show(this, "Failed to retrieve partner information")
                }

                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun removeSharedTasks(currentUserUid: String, partnerUid: String) {
        val userRef = database.child("users").child(currentUserUid)
        val partnerRef = database.child("users").child(partnerUid)

        // Get all tasks of the current user
        userRef.child("tasks").get().addOnSuccessListener { taskSnapshot ->
            val userTasks = taskSnapshot.children.map { it.key.toString() }

            // Get all tasks of the partner
            partnerRef.child("tasks").get().addOnSuccessListener { partnerTaskSnapshot ->
                val partnerTasks = partnerTaskSnapshot.children.map { it.key.toString() }
                val sharedTasks = userTasks.intersect(partnerTasks.toSet())

                // Remove tasks associated with both users
                sharedTasks.forEach { taskId ->
                    database.child("tasks").child(taskId).removeValue()
                    database.child("users").child(currentUserUid).child("tasks").child(taskId).removeValue()
                    database.child("users").child(partnerUid).child("tasks").child(taskId).removeValue()
                }

                // Remove the partner field from both users
                userRef.child("partner").removeValue()
                partnerRef.child("partner").removeValue().addOnCompleteListener { partnerTask ->
                    if (partnerTask.isSuccessful) {
                        CustomToast.show(this, "Partner removed successfully")
                        updateUIAfterPartnerRemoval()
                    }
                    else {
                        CustomToast.show(this, "Failed to remove your info from partner's data")
                    }
                }
            }.addOnFailureListener {
                CustomToast.show(this, "Failed to retrieve tasks")
            }
        }
    }

    private fun updateUIAfterPartnerRemoval() {
        val findYourPartnerText = findViewById<TextView>(R.id.findYourPartnerText)
        val partnerSearchBar = findViewById<EditText>(R.id.partnerSearchBar)
        val addPartnerButton = findViewById<Button>(R.id.addPartnerButton)

        val currentPartnerDisplay = findViewById<TextView>(R.id.currentPartnerDisplay)
        val removePartnerButton = findViewById<Button>(R.id.removePartnerButton)

        findYourPartnerText.visibility = View.VISIBLE
        partnerSearchBar.visibility = View.VISIBLE
        addPartnerButton.visibility = View.VISIBLE

        currentPartnerDisplay.visibility = View.GONE
        removePartnerButton.visibility = View.GONE
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                Log.d("MainActivity", "Home selected")
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Finish this activity so it's removed from the back stack
            }
            R.id.nav_profile -> {
                Log.d("MainActivity", "Profile selected")
                val intent = Intent(this, ProfileScreen::class.java)
                startActivity(intent)
            }
            R.id.nav_partner -> {
                drawerLayout.closeDrawer(GravityCompat.START)
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