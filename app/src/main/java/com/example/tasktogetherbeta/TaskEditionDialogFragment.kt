package com.example.tasktogetherbeta

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskEditDialogFragment : DialogFragment() {

    interface TaskEditListener {
        fun onTaskEdited()
    }

    private var taskId: String? = null
    private var task: Task? = null
    private var editListener: TaskEditListener? = null

    private lateinit var taskTitleEditText: EditText
    private lateinit var taskDescriptionEditText: EditText
    private lateinit var taskDueDateEditText: EditText
    private lateinit var taskPrioritySpinner: Spinner
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    companion object {
        private const val ARG_TASK_ID = "task_id"
        private const val ARG_TASK = "task"

        fun newInstance(taskId: String, task: Task): TaskEditDialogFragment {
            val fragment = TaskEditDialogFragment()
            val args = Bundle()
            args.putString(ARG_TASK_ID, taskId)
            args.putParcelable(ARG_TASK, task)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        editListener = context as? TaskEditListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getString(ARG_TASK_ID)
        task = arguments?.getParcelable(ARG_TASK)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_task_editor, container, false)
    }

    override fun onResume() {
        super.onResume()
        val window = dialog?.window
        val width = (resources.displayMetrics.widthPixels * 0.94).toInt()
        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://tasktogether-ea9f1-default-rtdb.europe-west1.firebasedatabase.app")

        taskTitleEditText = view.findViewById(R.id.taskTitleEditText)
        taskDescriptionEditText = view.findViewById(R.id.taskDescriptionEditText)
        taskDueDateEditText = view.findViewById(R.id.taskDueDateEditText)
        taskPrioritySpinner = view.findViewById(R.id.taskPrioritySpinner) // Initialize Spinner
        confirmButton = view.findViewById(R.id.createTaskButton)
        cancelButton = view.findViewById(R.id.cancelTaskButton)

        val priorityOptions = listOf("High", "Medium", "Low")
        val adapter = CustomSpinnerAdapter(requireContext(), R.layout.spinner_item, priorityOptions)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        taskPrioritySpinner.adapter = adapter

        // Set existing task data to input fields
        task?.let {
            taskTitleEditText.setText(it.title)
            taskDescriptionEditText.setText(it.description)
            taskDueDateEditText.setText(it.dueDate)

            // Set priority based on the current task priority
            val priorityPosition = when (it.priority) {
                "High" -> 0
                "Medium" -> 1
                "Low" -> 2
                else -> 1 // Default to medium if not found
            }
            taskPrioritySpinner.setSelection(priorityPosition)
        }

        // Update button text to indicate editing
        confirmButton.text = getString(R.string.confirm)

        taskDueDateEditText.setOnClickListener {
            val currentDateTime = Calendar.getInstance()
            val startYear = currentDateTime.get(Calendar.YEAR)
            val startMonth = currentDateTime.get(Calendar.MONTH)
            val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
            val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
            val startMinute = currentDateTime.get(Calendar.MINUTE)

            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    TimePickerDialog(
                        requireContext(),
                        { _, hour, minute ->
                            val selectedDateTime = Calendar.getInstance()
                            selectedDateTime.set(year, month, day, hour, minute)
                            if (selectedDateTime.timeInMillis < currentDateTime.timeInMillis) {
                                CustomToast.show(context, "Due date must be later than current time")
                            }
                            else {
                                val formattedDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(selectedDateTime.time)
                                taskDueDateEditText.setText(formattedDateTime)
                            }
                        },
                        startHour, startMinute, true // Again use 24-hour format BECAUSE WTF IS A KILOMETEER RAAWRRWRWRWRWRRW
                    ).show()
                },
                startYear, startMonth, startDay
            ).show()
        }

        confirmButton.setOnClickListener {
            val title = taskTitleEditText.text.toString().trim()
            val description = taskDescriptionEditText.text.toString().trim()
            val dueDate = taskDueDateEditText.text.toString().trim()

            // Extract the first word from the priority string
            var priority = taskPrioritySpinner.selectedItem.toString()
            priority = if (priority.contains(" ")) priority.split(" ")[0] else priority

            if (title.isNotEmpty() && dueDate.isNotEmpty()) {
                editTask(title, description, dueDate, priority)
            }
            else {
                CustomToast.show(context, "Title and Due Date are required")
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun editTask(title: String, description: String, dueDate: String, priority: String) {
        if (taskId == null) return

        val updatedTask = mapOf(
            "title" to title,
            "description" to description,
            "dueDate" to dueDate,
            "priority" to priority
        )

        database.reference.child("tasks").child(taskId!!).updateChildren(updatedTask)
            .addOnCompleteListener { taskUpdate ->
                if (taskUpdate.isSuccessful) {
                    CustomToast.show(context, "Task updated successfully")
                    dismiss()
                    editListener?.onTaskEdited() // Refresh the task list
                }
                else {
                    CustomToast.show(context, "Failed to update task")
                }
            }
            .addOnFailureListener {
                CustomToast.show(context, "Failed to update task")
            }
    }
}