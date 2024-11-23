package com.example.tasktogetherbeta

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class TaskAdapter(
    var tasks: List<Task>,
    private val onDeleteTask: (Task) -> Unit,
    private val onCompleteTask: (Task) -> Unit,
    private val onReactivateTask: (Task) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TASK = 0
        private const val TYPE_COMPLETED_TASK = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_COMPLETED_TASK -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_completed_task, parent, false)
                CompletedTaskViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
                TaskViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val task = tasks[position]
        when (holder) {
            is TaskViewHolder -> holder.bind(task)
            is CompletedTaskViewHolder -> holder.bind(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    override fun getItemViewType(position: Int): Int {
        return if (tasks[position].completedBy.isNotEmpty()) {
            TYPE_COMPLETED_TASK
        }
        else {
            TYPE_TASK
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val priorityIcon: ImageView = itemView.findViewById(R.id.priorityIcon)
        private val taskTitleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)
        private val taskDetailsLayout: LinearLayout = itemView.findViewById(R.id.taskDetailsLayout)
        private val taskDescriptionTextView: TextView = itemView.findViewById(R.id.taskDescriptionTextView)
        private val taskDueDateTextView: TextView = itemView.findViewById(R.id.taskDueDateTextView)
        private val taskCreatedByTextView: TextView = itemView.findViewById(R.id.taskCreatedByTextView)
        private val priorityTextView: TextView = itemView.findViewById(R.id.priorityTextView)
        private val completeTaskButton: Button = itemView.findViewById(R.id.completeTaskButton)
        private val editTaskButton: Button = itemView.findViewById(R.id.editTaskButton)
        private val deleteTaskButton: Button = itemView.findViewById(R.id.deleteTaskButton)

        fun bind(task: Task) {
            taskTitleTextView.text = task.title
            taskDescriptionTextView.text = task.description?.takeIf { it.isNotEmpty() } ?: "No description"
            taskDueDateTextView.text = "Due Date: ${task.dueDate}"
            taskCreatedByTextView.text = "Created By: ${task.createdBy}"

            when (task.priority) {
                "High" -> {
                    priorityIcon.setImageResource(R.drawable.baseline_priority_high_24)
                    priorityTextView.text = itemView.context.getString(R.string.priority_level, "High")
                }
                "Medium" -> {
                    priorityIcon.setImageResource(R.drawable.baseline_priority_medium_24)
                    priorityTextView.text = itemView.context.getString(R.string.priority_level, "Medium")
                }
                "Low" -> {
                    priorityIcon.setImageResource(R.drawable.baseline_priority_low_24)
                    priorityTextView.text = itemView.context.getString(R.string.priority_level, "Low")
                }
                else -> { // Default priority is medium just in case
                    priorityIcon.setImageResource(R.drawable.baseline_priority_medium_24)
                    priorityTextView.text = itemView.context.getString(R.string.priority_level, "Unknown")
                }
            }

            // Visibility handling for TaskViewHolder
            taskDetailsLayout.visibility = View.GONE
            completeTaskButton.visibility = View.VISIBLE
            editTaskButton.visibility = View.VISIBLE
            deleteTaskButton.visibility = View.VISIBLE

            itemView.setOnClickListener {
                if (taskDetailsLayout.visibility == View.VISIBLE) {
                    collapseTaskDetails()
                }
                else {
                    expandTaskDetails()
                }
            }

            completeTaskButton.setOnClickListener { onCompleteTask(task) }
            editTaskButton.setOnClickListener {
                val activity = itemView.context as? AppCompatActivity ?: return@setOnClickListener
                val fragmentManager = activity.supportFragmentManager
                task.taskId?.let { it1 -> TaskEditDialogFragment.newInstance(it1, task) }
                    ?.show(fragmentManager, "TaskEditDialogFragment")
            }
            deleteTaskButton.setOnClickListener { onDeleteTask(task) }
        }

        private fun expandTaskDetails() {
            taskDetailsLayout.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val targetHeight = taskDetailsLayout.measuredHeight

            taskDetailsLayout.layoutParams.height = 0
            taskDetailsLayout.visibility = View.VISIBLE

            val animation = ValueAnimator.ofInt(0, targetHeight)
            animation.duration = 300
            animation.interpolator = AccelerateDecelerateInterpolator()
            animation.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                taskDetailsLayout.layoutParams.height = value
                taskDetailsLayout.requestLayout()
            }
            animation.start()
        }

        private fun collapseTaskDetails() {
            val initialHeight = taskDetailsLayout.measuredHeight

            val animation = ValueAnimator.ofInt(initialHeight, 0)
            animation.duration = 300
            animation.interpolator = AccelerateDecelerateInterpolator()
            animation.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                taskDetailsLayout.layoutParams.height = value
                taskDetailsLayout.requestLayout()
            }
            animation.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    taskDetailsLayout.visibility = View.GONE
                }
            })
            animation.start()
        }
    }

    inner class CompletedTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskTitleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)
        private val taskDetailsLayout: LinearLayout = itemView.findViewById(R.id.taskDetailsLayout)
        private val taskDescriptionTextView: TextView = itemView.findViewById(R.id.taskDescriptionTextView)
        private val taskDueDateTextView: TextView = itemView.findViewById(R.id.taskDueDateTextView)
        private val taskCompletedByTextView: TextView = itemView.findViewById(R.id.taskCompletedByTextView)
        private val completedDateTextView: TextView = itemView.findViewById(R.id.completedDateTextView)
        private val lateTextView: TextView = itemView.findViewById(R.id.lateTextView)
        private val reactivateTaskButton: Button = itemView.findViewById(R.id.reactivateTaskButton)
        private val deleteTaskButton: Button = itemView.findViewById(R.id.deleteTaskButton)

        @SuppressLint("SetTextI18n")
        fun bind(task: Task) {
            taskTitleTextView.text = task.title
            taskDescriptionTextView.text = task.description?.takeIf { it.isNotEmpty() } ?: "No description"
            taskDueDateTextView.text = "Due Date: ${task.dueDate}"
            completedDateTextView.text = "Completed On: ${task.completedOn}"

            fetchUsername(task.completedBy) { username ->
                taskCompletedByTextView.text = "Completed By: ${username ?: "Unknown"}"
            }

            if (isTaskLate(task.dueDate, task.completedOn)) {
                lateTextView.visibility = View.VISIBLE
            }
            else {
                lateTextView.visibility = View.GONE
            }

            // Visibility handling for CompletedTaskViewHolder
            taskDetailsLayout.visibility = View.GONE
            reactivateTaskButton.visibility = View.VISIBLE
            deleteTaskButton.visibility = View.VISIBLE

            itemView.setOnClickListener {
                if (taskDetailsLayout.visibility == View.VISIBLE) {
                    collapseTaskDetails()
                }
                else {
                    expandTaskDetails()
                }
            }

            reactivateTaskButton.setOnClickListener { onReactivateTask(task) }
            deleteTaskButton.setOnClickListener { onDeleteTask(task) }
        }

        private fun isTaskLate(dueDate: String?, completedOn: String?): Boolean {
            if (dueDate.isNullOrBlank() || completedOn.isNullOrBlank()) return false

            return try {
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

                // Parse the due date and completed date strings
                val dueDateTime = LocalDateTime.parse(dueDate.trim(), formatter)
                val completedDateTime = LocalDateTime.parse(completedOn.trim(), formatter)

                // Check if the task was completed after the due date
                completedDateTime.isAfter(dueDateTime)
            }
            catch (e: DateTimeParseException) {
                e.printStackTrace()
                false // if any error happens, not late
            }
        }

        private fun expandTaskDetails() {
            taskDetailsLayout.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val targetHeight = taskDetailsLayout.measuredHeight

            taskDetailsLayout.layoutParams.height = 0
            taskDetailsLayout.visibility = View.VISIBLE

            val animation = ValueAnimator.ofInt(0, targetHeight)
            animation.duration = 300
            animation.interpolator = AccelerateDecelerateInterpolator()
            animation.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                taskDetailsLayout.layoutParams.height = value
                taskDetailsLayout.requestLayout()
            }
            animation.start()
        }

        private fun collapseTaskDetails() {
            val initialHeight = taskDetailsLayout.measuredHeight

            val animation = ValueAnimator.ofInt(initialHeight, 0)
            animation.duration = 300
            animation.interpolator = AccelerateDecelerateInterpolator()
            animation.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                taskDetailsLayout.layoutParams.height = value
                taskDetailsLayout.requestLayout()
            }
            animation.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    taskDetailsLayout.visibility = View.GONE
                }
            })
            animation.start()
        }

        private fun fetchUsername(userId: String, callback: (String?) -> Unit) {
            val database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app")
            val userRef = database.getReference("users").child(userId).child("username")

            userRef.get().addOnSuccessListener { snapshot ->
                val username = snapshot.getValue(String::class.java)
                callback(username)
            }.addOnFailureListener {
                callback(null)
            }
        }
    }
}
