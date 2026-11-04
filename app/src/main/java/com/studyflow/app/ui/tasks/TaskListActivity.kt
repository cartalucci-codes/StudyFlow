package com.studyflow.app.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.studyflow.app.R
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.remote.RetrofitClient
import com.studyflow.app.data.repository.SyncScheduler
import com.studyflow.app.data.repository.TaskRepository
import com.studyflow.app.databinding.ActivityTaskListBinding
import com.studyflow.app.util.Gamification
import com.studyflow.app.util.NetworkLiveData
import com.studyflow.app.util.SessionManager
import com.studyflow.app.util.applySystemBarInsets
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/**
 * Calendar / Tasks screen (Planning & Design doc, Section 4.1): a month calendar to pick a day,
 * the tasks due on that day (or all upcoming tasks), and a button to add a new one.
 */
class TaskListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskListBinding
    private lateinit var repository: TaskRepository
    private lateinit var adapter: TaskAdapter

    private var allTasks: List<TaskEntity> = emptyList()
    private var selectedDay: Long? = null // Gamification.dayIndex of the chosen day; null = show all
    private var selectedMillis: Long = System.currentTimeMillis() // noon on the chosen day (for display)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        val session = SessionManager(this)
        val userId = session.userId ?: run { finish(); return }
        val db = AppDatabase.getInstance(this)
        repository = TaskRepository(db.taskDao(), db.studySessionDao(), RetrofitClient.api, session)

        adapter = TaskAdapter(
            onClick = { task ->
                startActivity(Intent(this, AddEditTaskActivity::class.java).putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.taskId))
            },
            onToggleDone = { task, done ->
                lifecycleScope.launch {
                    repository.setDone(task, done)
                    SyncScheduler.enqueue(this@TaskListActivity)
                    if (done) Toast.makeText(this@TaskListActivity, getString(R.string.msg_task_done_xp, Gamification.XP_PER_TASK), Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.recyclerTasks.layoutManager = LinearLayoutManager(this)
        binding.recyclerTasks.adapter = adapter

        repository.observeTasks(userId).observe(this) { tasks ->
            allTasks = tasks
            render()
        }
        NetworkLiveData(this).observe(this) { online ->
            binding.textOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            val cal = Calendar.getInstance().apply { set(year, month, day, 12, 0, 0) }
            selectedMillis = cal.timeInMillis
            selectedDay = Gamification.dayIndex(selectedMillis)
            render()
        }
        binding.buttonShowAll.setOnClickListener {
            selectedDay = null
            render()
        }
        binding.fabAddTask.setOnClickListener { startActivity(Intent(this, AddEditTaskActivity::class.java)) }
    }

    private fun render() {
        val day = selectedDay
        val shown = if (day == null) allTasks else allTasks.filter { Gamification.dayIndex(it.dueDate) == day }
        adapter.submitList(shown)

        binding.textSelectedDay.text = if (day == null) {
            getString(R.string.calendar_showing_all)
        } else {
            getString(R.string.calendar_showing_day, DateFormat.getDateInstance(DateFormat.LONG).format(Date(selectedMillis)))
        }
        binding.textEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
    }
}
