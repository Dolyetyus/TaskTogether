package com.example.tasktogetherbeta

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class LoginScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    public override fun onStart() {
        super.onStart()

        // Check if device is connected to the internet
        if (!isConnectedToInternet()) {
            AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("Internet connection is required to use TaskTogether. Please connect and try again.")
                .setCancelable(false)
                .setNegativeButton("Close App") { _, _ ->
                    finish()
                }
                .show()
            return
        }

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    // Utility method to check for internet connectivity
    private fun isConnectedToInternet(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()

        enableEdgeToEdge()
        setContentView(R.layout.activity_login_screen)

        auth = Firebase.auth

        setupWindowInsets()

        // Initialize UI elements and their listeners
        setupUI()
    }

    private fun setupWindowInsets() {
        // Adjust view to fit system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        // Get references to UI elements
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signUpTextView = findViewById<TextView>(R.id.signUpTextView)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgotPasswordTextView)

        // Set onClickListener for login button
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                signIn(username, password)
            }
            else {
                CustomToast.show(this, "Please enter username and password")
            }
        }

        // Navigate to registration screen if user clicks on "Sign Up"
        signUpTextView.setOnClickListener {
            startActivity(Intent(this, RegisterScreen::class.java))
        }

        // Handle "Forgot Password" click
        forgotPasswordTextView.setOnClickListener {
            CustomToast.show(this, "This functionality is not available yet")
        }
    }

    private fun signIn(username: String, password: String) {
        val database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app")
        val usernamesRef = database.getReference("usernames")
        val usersRef = database.getReference("users")

        // Retrieve the UID associated with the username
        usernamesRef.child(username).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val userId = snapshot.getValue(String::class.java)
                if (userId != null) {
                    // Retrieve the email associated with the UID
                    usersRef.child(userId).child("email").get().addOnSuccessListener { emailSnapshot ->
                        val email = emailSnapshot.getValue(String::class.java)

                        if (email != null) {
                            // Authenticate the user with email and password
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        Log.d(TAG, "signInWithEmail:success")
                                        val user = auth.currentUser
                                        updateUI(user)
                                    }
                                    else {
                                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                                        CustomToast.show(this, "Your username or password is wrong")
                                        updateUI(null)
                                    }
                                }
                        }
                        else {
                            CustomToast.show(this, "Failed to retrieve credentials")
                        }
                    }.addOnFailureListener { exception ->
                        Log.w(TAG, "Error retrieving email", exception)
                        CustomToast.show(this, "Failed to retrieve credentials")
                    }
                }
                else {
                    CustomToast.show(this, "User data not found")
                }
            }
            else {
                CustomToast.show(this, "Username not found")
            }
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Error retrieving UID", exception)
            CustomToast.show(this, "Failed to retrieve user ID")
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            CustomToast.show(this, "Login Successful!")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        private const val TAG = "LoginScreen"
    }
}
