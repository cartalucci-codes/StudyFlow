package com.studyflow.app.ui.tasks

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyflow.app.R
import com.studyflow.app.data.local.entity.Priority
import com.studyflow.app.data.local.entity.SyncStatus
import com.studyflow.app.data.local.entity.TaskEntity
import com.studyflow.app.data.local.entity.TaskStatus
import com.studyflow.app.databinding.ItemTaskBinding
import java.text.DateFormat
import java.util.Date

/** Shows tasks on both the Home ("My Day") and Calendar screens. */
class TaskAdapter(
    private val onClick: (TaskEntity) -> Unit,
    private val onToggleDone: (TaskEntity, Boolean) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = getItem(position)
        val b = holder.binding
        val context = b.root.context
        val done = task.status == TaskStatus.Done
        val overdue = !done && task.dueDate < System.currentTimeMillis()

        b.textTitle.text = task.title
        b.textTitle.paintFlags =
            if (done) b.textTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else b.textTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        val due = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(task.dueDate))
        b.textSubtitle.text = listOfNotNull(task.subject?.takeIf { it.isNotBlank() }, due).joinToString("  •  ")
        b.textSubtitle.setTextColor(
            if (overdue) ContextCompat.getColor(context, R.color.overdue)
            else ContextCompat.getColor(context, R.color.text_secondary)
        )

        b.textPriority.text = context.getString(
            when (task.priority) {
                Priority.Low -> R.string.priority_low
                Priority.Med -> R.string.priority_med
                Priority.High -> R.string.priority_high
            }
        )
        // ⏳ = saved on the phone, waiting to sync   ☁ = safely on the server
        b.textSync.text = if (task.syncStatus == SyncStatus.SYNCED) "☁" else "⏳"

        // Detach the listener first so recycled rows don't fire it while we set the state.
        b.checkDone.setOnCheckedChangeListener(null)
        b.checkDone.isChecked = done
        b.checkDone.setOnCheckedChangeListener { _, checked -> onToggleDone(task, checked) }
        b.root.setOnClickListener { onClick(task) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TaskEntity>() {
            override fun areItemsTheSame(old: TaskEntity, new: TaskEntity) = old.taskId == new.taskId
            override fun areContentsTheSame(old: TaskEntity, new: TaskEntity) = old == new
        }
    }
}
