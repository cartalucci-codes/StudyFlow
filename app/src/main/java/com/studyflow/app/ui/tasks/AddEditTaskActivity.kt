package com.studyflow.app.ui.tasks

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.local.entity.Priority
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.databinding.ActivityAddEditTaskBinding
import com.studyflow.app.util.SessionManager
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Add/Edit Task screen. Saves instantly to the local Room database (offline-first)
 * and queues a background sync — see Planning & Design doc, Section 4.1.
 */
class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private lateinit var taskRepository: TaskRepository
    private lateinit var session: SessionManager
    private var selectedDueDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        val db = AppDatabase.getInstance(this)
        taskRepository = TaskRepository(db.taskDao(), RetrofitClient.api, session)

        val priorities = Priority.values().map { it.name }
        binding.spinnerPriority.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, priorities)

        binding.buttonPickDueDate.setOnClickListener { showDatePicker() }
        binding.buttonSaveTask.setOnClickListener { saveTask() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            selectedDueDate.set(year, month, day)
            binding.buttonPickDueDate.text = "${day}/${month + 1}/${year}"
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveTask() {
        val title = binding.editTitle.text.toString().trim()
        val subject = binding.editSubject.text.toString().trim()
        val userId = session.userId

        if (title.isEmpty()) {
            Toast.makeText(this, "Give the task a title", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null) {
            Toast.makeText(this, "You need to be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val priority = Priority.valueOf(priorities().getOrElse(binding.spinnerPriority.selectedItemPosition) { "Med" })

        lifecycleScope.launch {
            taskRepository.addTask(
                userId = userId,
                title = title,
                description = null,
                subject = subject.ifEmpty { null },
                dueDate = selectedDueDate.timeInMillis,
                priority = priority,
                reminderTime = null
            )
            Toast.makeText(this@AddEditTaskActivity, "Saved offline instantly, syncs when online", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun priorities() = Priority.values().map { it.name }
}
