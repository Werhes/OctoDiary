package org.bxkr.octodiary.data.model.api.mes.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventsResponse(
    @SerialName("response") val response: List<EventItem> = emptyList()
)

@Serializable
data class EventItem(
    @SerialName("id") val id: Long = 0,
    @SerialName("lesson_name") val lessonName: String? = null,
    @SerialName("subject_id") val subjectId: Long? = null,
    @SerialName("subject_name") val subjectName: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("start_at") val startAt: String? = null,
    @SerialName("finish_at") val finishAt: String? = null,
    @SerialName("is_all_day") val isAllDay: Boolean? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("homework") val homework: EventHomework? = null,
    @SerialName("marks") val marks: List<EventMark>? = emptyList()
)

@Serializable
data class EventHomework(
    @SerialName("descriptions") val descriptions: List<String> = emptyList()
)

@Serializable
data class EventMark(
    @SerialName("id") val id: Long = 0,
    @SerialName("value") val value: String = "",
    @SerialName("weight") val weight: Int = 1,
    @SerialName("control_form_name") val controlFormName: String? = null,
    @SerialName("is_exam") val isExam: Boolean = false
)