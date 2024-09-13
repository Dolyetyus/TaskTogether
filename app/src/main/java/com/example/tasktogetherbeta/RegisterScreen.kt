package com.example.tasktogetherbeta

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class RegisterScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var usernamesRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide() // This looks ugly
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_screen)

        // Initialize Firebase Auth and Database references
        auth = Firebase.auth
        database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app")
        usersRef = database.getReference("users")
        usernamesRef = database.getReference("usernames")

        setupUI()
    }

    private fun setupUI() {
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val passwordConfirmText = findViewById<EditText>(R.id.confirmPasswordText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginScreenTextView = findViewById<TextView>(R.id.LoginTextView)

        // Handle registration
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = passwordConfirmText.text.toString().trim()

            if (isInputValid(username, email, password, confirmPassword)) {
                registerUser(username, email, password)
            }
        }

        // Redirect to login screen after registration
        loginScreenTextView.setOnClickListener {
            startActivity(Intent(this, LoginScreen::class.java))
        }
    }

    private fun isInputValid(username: String, email: String, password: String, confirmPassword: String): Boolean {
        return when {
            username.isEmpty() -> {
                CustomToast.show(this, "Username cannot be empty.")
                false
            }
            !isValidUsername(username) -> {
                CustomToast.show(this, "Invalid username. Only lowercase, digits, and max 16 characters allowed.")
                false
            }
            email.isEmpty() -> {
                CustomToast.show(this, "Email cannot be empty.")
                false
            }
            password.isEmpty() -> {
                CustomToast.show(this, "Password cannot be empty.")
                false
            }
            password != confirmPassword -> {
                CustomToast.show(this, "Passwords don't match.")
                false
            }
            else -> true
        }
    }

    private fun isValidUsername(username: String): Boolean {
        val regex = "^[a-z0-9_]{1,16}\$".toRegex()
        return username.matches(regex)
    }

    private fun registerUser(username: String, email: String, password: String) {
        val database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app")
        val usersRef = database.getReference("users")
        val usernamesRef = database.getReference("usernames")

        usernamesRef.child(username).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                CustomToast.show(this, "Username is already taken.")
            }
            else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid
                            if (userId != null) {
                                val userMap = hashMapOf("username" to username, "email" to email)

                                usersRef.child(userId).setValue(userMap).addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        usernamesRef.child(username).setValue(userId)
                                            .addOnCompleteListener { usernameTask ->
                                                if (usernameTask.isSuccessful) {
                                                    Log.d(TAG, "User saved to database successfully")
                                                    updateUI(user)
                                                }
                                                else {
                                                    Log.w(TAG, "Failed to save username mapping", usernameTask.exception)
                                                    CustomToast.show(this, "An error occurred while trying to save username mapping")
                                                }
                                            }
                                    }
                                    else {
                                        Log.w(TAG, "Failed to save user data", dbTask.exception)
                                        CustomToast.show(this, "Failed to save user data")
                                    }
                                }
                            }
                        }
                        else {
                            // Handle failure case and specific exceptions
                            task.exception?.let { exception ->
                                when (exception) {
                                    is FirebaseAuthUserCollisionException -> {
                                        // Email already in use
                                        CustomToast.show(this, "The email address is already in use")
                                    }
                                    else -> {
                                        Log.w(TAG, "createUserWithEmail:failure", exception)
                                        CustomToast.show(this, exception.localizedMessage)
                                    }
                                }
                            }
                        }
                    }
            }
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Error checking username", exception)
            CustomToast.show(this, "An error occurred while checking username availability")
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        user?.let {
            CustomToast.show(this, "Registration Successful!")
            startActivity(Intent(this, LoginScreen::class.java))
        }
    }

    companion object {
        private const val TAG = "RegisterScreen"
    }
}
