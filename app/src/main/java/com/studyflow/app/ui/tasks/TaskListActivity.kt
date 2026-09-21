package com.studyflow.app.ui.tasks

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.studyflow.app.data.local.AppDatabase
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.databinding.ActivityTaskListBinding
import com.studyflow.app.util.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Calendar / Tasks screen: scrollable list of upcoming tasks (see Planning & Design doc,
 * Section 4.1). Tapping the FAB opens Add/Edit Task. A full month-grid calendar view can be
 * layered on top of this list later without changing the data layer.
 */
class TaskListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager(this)
        val userId = session.userId

        binding.recyclerTasks.layoutManager = LinearLayoutManager(this)
        val adapter = TaskAdapter()
        binding.recyclerTasks.adapter = adapter

        if (userId != null) {
            AppDatabase.getInstance(this).taskDao().observeTasksForUser(userId)
                .observe(this) { tasks -> adapter.submitList(tasks) }
        }

        binding.fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }
    }
}

private class TaskAdapter : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    private var items: List<TaskEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    fun submitList(newItems: List<TaskEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    class TaskViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = items[position]
        holder.text.text = "${task.title}  •  ${task.priority}  •  ${dateFormat.format(Date(task.dueDate))}"
    }

    override fun getItemCount(): Int = items.size
}
