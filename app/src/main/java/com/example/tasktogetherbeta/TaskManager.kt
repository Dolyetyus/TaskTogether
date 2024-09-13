package com.example.tasktogetherbeta

import android.app.AlertDialog
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskManager(private val context: Context) {
    private val auth: FirebaseAuth = Firebase.auth
    private val database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app")

    fun fetchTasks(onTasksFetched: (List<Task>) -> Unit, sortBy: String = "DueDate") {
        fetchTasksByCompletion(isCompleted = false) { tasks ->
            val sortedTasks = when (sortBy) {
                "TitleAsc" -> tasks.sortedBy { it.title }
                "TitleDesc" -> tasks.sortedByDescending { it.title }
                "DueDateAsc" -> tasks.sortedBy { parseDueDate(it.dueDate) }
                "DueDateDesc" -> tasks.sortedByDescending { parseDueDate(it.dueDate) }
                "PriorityAsc" -> tasks.sortedBy { getPriorityValue(it.priority) }
                "PriorityDesc" -> tasks.sortedByDescending { getPriorityValue(it.priority) }
                else -> tasks.sortedBy { parseDueDate(it.dueDate) }  // Default sorting by Due Date
            }
            onTasksFetched(sortedTasks)
        }
    }

    // Helper function to parse due date string to Date object
    private fun parseDueDate(dueDateString: String): Date? {
        return try {
            val dateFormatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            dateFormatter.parse(dueDateString)
        }
        catch (e: Exception) {
            null  // Return null if date format is incorrect
        }
    }

    // Helper function to convert priority strings to numeric values
    private fun getPriorityValue(priority: String): Int {
        return when (priority) {
            "High" -> 3
            "Medium" -> 2
            "Low" -> 1
            else -> 2  // In case priority is missing or invalid, then medium
        }
    }

    fun fetchCompletedTasks(onTasksFetched: (List<Task>) -> Unit) {
        fetchTasksByCompletion(isCompleted = true, onTasksFetched = onTasksFetched)
    }

    private fun fetchTasksByCompletion(isCompleted: Boolean, onTasksFetched: (List<Task>) -> Unit) {
        val currentUserUid = auth.currentUser?.uid ?: return
        val tasksRef = database.getReference("users/$currentUserUid/tasks")
        val allTasksRef = database.getReference("tasks")
        val usersRef = database.getReference("users")

        tasksRef.get().addOnSuccessListener { snapshot ->
            val tasks = mutableListOf<Task>()
            val taskIds = snapshot.children.mapNotNull { it.key }

            if (taskIds.isEmpty()) {
                onTasksFetched(tasks)
                return@addOnSuccessListener
            }

            var processedCount = 0 // Keep track of the completed tasks

            taskIds.forEach { taskId ->
                allTasksRef.child(taskId).get().addOnSuccessListener { taskSnapshot ->
                    val taskMap = taskSnapshot.value as? Map<*, *>
                    if (taskMap != null) {
                        val createdByUid = taskMap["createdBy"] as? String ?: ""

                        usersRef.child(createdByUid).child("username").get()
                            .addOnSuccessListener { usernameSnapshot ->
                                val username = usernameSnapshot.value as? String ?: "Unknown User"
                                val task = Task(
                                    taskId = taskId,
                                    title = taskMap["title"] as? String ?: "",
                                    description = taskMap["description"] as? String ?: "No description",
                                    dueDate = taskMap["dueDate"] as? String ?: "",
                                    priority = taskMap["priority"] as? String ?: "",
                                    createdBy = username,
                                    completedBy = taskMap["completedBy"] as? String ?: "",
                                    completedOn = taskMap["completedOn"] as? String ?: ""
                                )

                                // Add task based on the completion status
                                if (isCompleted && task.completedBy.isNotEmpty()) {
                                    tasks.add(task)
                                }
                                else if (!isCompleted && task.completedBy.isEmpty()) {
                                    tasks.add(task)
                                }

                                processedCount++
                                if (processedCount == taskIds.size) {
                                    val sortedTasks = if (isCompleted) {
                                        tasks.sortedBy { it.completedOn }
                                    }
                                    else {
                                        tasks.sortedBy { it.dueDate }
                                    }
                                    onTasksFetched(sortedTasks)
                                }
                            }
                            .addOnFailureListener {
                                processedCount++
                                if (processedCount == taskIds.size) {
                                    onTasksFetched(tasks.sortedBy { if (isCompleted) it.completedOn else it.dueDate })
                                }
                                CustomToast.show(context, "Failed to load username for task: $taskId")
                            }
                    }
                    else {
                        processedCount++
                        if (processedCount == taskIds.size) {
                            onTasksFetched(tasks.sortedBy { if (isCompleted) it.completedOn else it.dueDate })
                        }
                    }
                }.addOnFailureListener {
                    processedCount++
                    if (processedCount == taskIds.size) {
                        onTasksFetched(tasks.sortedBy { if (isCompleted) it.completedOn else it.dueDate })
                    }
                    CustomToast.show(context, "Failed to load task: $taskId")
                }
            }
        }.addOnFailureListener {
            CustomToast.show(context, "Failed to load tasks")
        }
    }

    fun completeTask(task: Task, onTaskCompleted: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Confirm Task Completion")
            .setMessage("You can always find completed tasks under 'Completed Tasks'.")
            .setPositiveButton("Confirm") { _, _ ->
                performTaskCompletion(task, onTaskCompleted)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performTaskCompletion(task: Task, onTaskCompleted: () -> Unit) {
        val currentUserUid = auth.currentUser?.uid ?: return
        val taskRef = database.getReference("tasks/${task.taskId}")

        val dateFormatter = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val currentDateTime = dateFormatter.format(Date())

        val updates = mapOf(
            "completedBy" to currentUserUid,
            "completedOn" to currentDateTime
        )

        taskRef.updateChildren(updates).addOnSuccessListener {
            CustomToast.show(context, "Task marked as completed")
            onTaskCompleted()
        }.addOnFailureListener {
            CustomToast.show(context, "Failed to mark task as completed")
        }
    }

    fun reactivateTask(task: Task, onTaskReactivated: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Confirm Task Reactivation")
            .setMessage("Are you sure you want to reactivate this task?")
            .setPositiveButton("Reactivate") { _, _ ->
                performTaskReactivation(task, onTaskReactivated)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performTaskReactivation(task: Task, onTaskReactivated: () -> Unit) {
        val taskRef = database.getReference("tasks/${task.taskId}")

        val updates = mapOf(
            "completedBy" to null,  // Remove completedBy field
            "completedOn" to null   // Remove completedOn field
        )

        taskRef.updateChildren(updates).addOnSuccessListener {
            CustomToast.show(context, "Task reactivated")
            onTaskReactivated()
        }.addOnFailureListener {
            CustomToast.show(context, "Failed to reactivate task")
        }
    }

    fun deleteTask(task: Task, onTaskDeleted: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this task? \nThis action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performTaskDeletion(task, onTaskDeleted)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performTaskDeletion(task: Task, onTaskDeleted: () -> Unit) {
        val currentUserUid = auth.currentUser?.uid ?: return
        val userTasksRef = database.getReference("users/$currentUserUid/tasks/${task.taskId}")
        val allTasksRef = database.getReference("tasks/${task.taskId}")

        val userRef = database.getReference("users/$currentUserUid")
        userRef.child("partner").get().addOnSuccessListener { partnerSnapshot ->
            val partnerUid = partnerSnapshot.getValue(String::class.java) ?: return@addOnSuccessListener

            val partnerTasksRef = database.getReference("users/$partnerUid/tasks/${task.taskId}")
            userTasksRef.removeValue().addOnSuccessListener {
                partnerTasksRef.removeValue().addOnSuccessListener {
                    allTasksRef.removeValue().addOnSuccessListener {
                        CustomToast.show(context, "Task deleted")
                        onTaskDeleted()
                    }.addOnFailureListener {
                        CustomToast.show(context, "Failed to delete task")
                    }
                }.addOnFailureListener {
                    CustomToast.show(context, "Failed to delete task from partner's task list")
                }
            }.addOnFailureListener {
                CustomToast.show(context, "Failed to delete task from user's task list")
            }
        }.addOnFailureListener {
            CustomToast.show(context, "Failed to fetch partner data")
        }
    }
}