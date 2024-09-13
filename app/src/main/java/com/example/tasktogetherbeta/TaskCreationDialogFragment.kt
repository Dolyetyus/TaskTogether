package com.example.tasktogetherbeta

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.icu.util.Calendar
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
import java.util.Locale

open class TaskCreationDialogFragment : DialogFragment() {

    interface TaskCreationListener {
        fun onTaskCreated()
    }

    private lateinit var taskTitleEditText: EditText
    private lateinit var taskDescriptionEditText: EditText
    private lateinit var taskDueDateEditText: EditText
    private lateinit var taskPrioritySpinner: Spinner
    private lateinit var createTaskButton: Button
    private lateinit var cancelTaskButton: Button

    lateinit var auth: FirebaseAuth
    lateinit var database: FirebaseDatabase
    private var listener: TaskCreationListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? TaskCreationListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_task_creation, container, false)
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
        taskPrioritySpinner = view.findViewById(R.id.taskPrioritySpinner)
        createTaskButton = view.findViewById(R.id.createTaskButton)
        cancelTaskButton = view.findViewById(R.id.cancelTaskButton)

        val priorityOptions = listOf("High", "Medium", "Low")
        val adapter = CustomSpinnerAdapter(requireContext(), R.layout.spinner_item, priorityOptions)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        taskPrioritySpinner.adapter = adapter

        taskPrioritySpinner.setSelection(1) // Show medium as the default

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
                                // Format and set the selected date and time to the EditText
                                val formattedDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(selectedDateTime.time)
                                taskDueDateEditText.setText(formattedDateTime)
                            }
                        },
                        startHour, startMinute, true // Use 24-hour format BECAUSE WTF IS A KILOMETEER RAAWRRWRWRWRWRRW
                    ).show()
                },
                startYear, startMonth, startDay
            ).show()
        }

        createTaskButton.setOnClickListener {
            val title = taskTitleEditText.text.toString().trim()
            val description = taskDescriptionEditText.text.toString().trim()
            val dueDate = taskDueDateEditText.text.toString().trim()

            // Extract the first word from the priority string
            var priority = taskPrioritySpinner.selectedItem.toString()
            priority = if (priority.contains(" ")) priority.split(" ")[0] else priority

            if (title.isNotEmpty() && dueDate.isNotEmpty()) {
                createTask(title, description, dueDate, priority)
            }
            else {
                CustomToast.show(context, "Title and Due Date are required")
            }
        }

        cancelTaskButton.setOnClickListener {
            dismiss()
        }
    }

    private fun createTask(title: String, description: String, dueDate: String, priority: String) {
        val currentUserUid = auth.currentUser?.uid ?: return

        database.reference.child("users").child(currentUserUid).get()
            .addOnSuccessListener { dataSnapshot ->
                val partnerUid = dataSnapshot.child("partner").getValue(String::class.java)

                if (partnerUid.isNullOrEmpty()) {
                    CustomToast.show(context, "You cannot create a task without a partner") //very edge case
                    return@addOnSuccessListener
                }

                val taskId =
                    database.reference.child("tasks").push().key ?: return@addOnSuccessListener

                val task = mapOf(
                    "title" to title,
                    "description" to description,
                    "dueDate" to dueDate,
                    "priority" to priority,
                    "createdBy" to currentUserUid,
                    "completedBy" to null,
                    "completedOn" to null,
                    "partners" to listOf(currentUserUid, partnerUid)
                )

                database.reference.child("tasks").child(taskId).setValue(task)
                    .addOnCompleteListener { taskCreation ->
                        if (taskCreation.isSuccessful) {
                            database.reference.child("users").child(currentUserUid).child("tasks").child(taskId).setValue(true)
                            database.reference.child("users").child(partnerUid).child("tasks").child(taskId).setValue(true)

                            CustomToast.show(context, "Task created successfully")
                            dismiss() // Close the dialog after creation
                            // Notify the activity to refresh the task list
                            listener?.onTaskCreated()
                        }
                        else {
                            CustomToast.show(context, "Failed to create task")
                        }
                    }
                    .addOnFailureListener {
                        CustomToast.show(context, "Failed to fetch partner data")
                    }
            }
    }
}