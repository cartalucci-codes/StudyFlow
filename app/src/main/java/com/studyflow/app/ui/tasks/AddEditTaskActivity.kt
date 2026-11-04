package com.studyflow.app.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyflow.app.R
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.local.entity.Priority
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.SyncScheduler
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.databinding.ActivityAddEditTaskBinding
import com.studyflow.app.util.SessionManager
import com.studyflow.app.util.Validators
import com.studyflow.app.util.applySystemBarInsets
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar

/**
 * Add / Edit Task screen (Planning & Design doc, Section 4.1). Saves instantly to the local Room
 * database (offline-first) and queues a background sync. Pass [EXTRA_TASK_ID] to edit an existing task.
 */
class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private lateinit var repository: TaskRepository
    private lateinit var session: SessionManager

    private var existing: TaskEntity? = null
    private val due: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        session = SessionManager(this)
        val db = AppDatabase.getInstance(this)
        repository = TaskRepository(db.taskDao(), db.studySessionDao(), RetrofitClient.api, session)

        binding.radioMed.isChecked = true
        updateDueButtons()
        binding.buttonPickDate.setOnClickListener { pickDate() }
        binding.buttonPickTime.setOnClickListener { pickTime() }
        binding.buttonSaveTask.setOnClickListener { saveTask() }
        binding.buttonDeleteTask.setOnClickListener { confirmDelete() }

        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        if (taskId != null) {
            binding.textScreenTitle.setText(R.string.title_edit_task)
            lifecycleScope.launch {
                val task = repository.getTask(taskId) ?: return@launch
                existing = task
                binding.inputTitle.setText(task.title)
                binding.inputSubject.setText(task.subject)
                binding.inputDescription.setText(task.description)
                due.timeInMillis = task.dueDate
                binding.switchReminder.isChecked = task.reminderTime != null
                when (task.priority) {
                    Priority.Low -> binding.radioLow.isChecked = true
                    Priority.Med -> binding.radioMed.isChecked = true
                    Priority.High -> binding.radioHigh.isChecked = true
                }
                binding.buttonDeleteTask.visibility = View.VISIBLE
                updateDueButtons()
            }
        }
    }

    private fun updateDueButtons() {
        binding.buttonPickDate.text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(due.time)
        binding.buttonPickTime.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(due.time)
    }

    private fun pickDate() {
        DatePickerDialog(this, { _, year, month, day ->
            due.set(year, month, day)
            updateDueButtons()
        }, due.get(Calendar.YEAR), due.get(Calendar.MONTH), due.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime() {
        TimePickerDialog(this, { _, hour, minute ->
            due.set(Calendar.HOUR_OF_DAY, hour)
            due.set(Calendar.MINUTE, minute)
            updateDueButtons()
        }, due.get(Calendar.HOUR_OF_DAY), due.get(Calendar.MINUTE), android.text.format.DateFormat.is24HourFormat(this)).show()
    }

    private fun saveTask() {
        val title = binding.inputTitle.text.toString()
        val userId = session.userId

        // Invalid input is explained next to the field instead of crashing or silently failing.
        binding.layoutTitle.error = if (Validators.isValidTitle(title)) null else getString(R.string.error_title_required)
        if (binding.layoutTitle.error != null) return
        if (userId == null) {
            Toast.makeText(this, R.string.msg_not_logged_in, Toast.LENGTH_SHORT).show()
            return
        }

        val priority = when (binding.radioGroupPriority.checkedRadioButtonId) {
            R.id.radioLow -> Priority.Low
            R.id.radioHigh -> Priority.High
            else -> Priority.Med
        }
        val reminder = if (binding.switchReminder.isChecked) due.timeInMillis - 60 * 60 * 1000L else null
        val subject = binding.inputSubject.text.toString().trim().ifEmpty { null }
        val description = binding.inputDescription.text.toString().trim().ifEmpty { null }

        val task = (existing ?: TaskEntity(userId = userId, title = title, dueDate = due.timeInMillis)).copy(
            title = title.trim(),
            subject = subject,
            description = description,
            dueDate = due.timeInMillis,
            priority = priority,
            reminderTime = reminder
        )

        lifecycleScope.launch {
            repository.saveTask(task)
            SyncScheduler.enqueue(this@AddEditTaskActivity) // syncs now if online, or as soon as we reconnect
            Toast.makeText(this@AddEditTaskActivity, R.string.msg_task_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmDelete() {
        val task = existing ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_task_title)
            .setMessage(task.title)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteTask(task)
                    SyncScheduler.enqueue(this@AddEditTaskActivity)
                    Toast.makeText(this@AddEditTaskActivity, R.string.msg_task_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }
}
