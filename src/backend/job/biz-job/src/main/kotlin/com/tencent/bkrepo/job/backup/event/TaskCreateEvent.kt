package com.tencent.bkrepo.job.backup.event

import org.springframework.context.ApplicationEvent

class TaskCreateEvent(
    val context: TaskCreateContext
) : ApplicationEvent(context)
