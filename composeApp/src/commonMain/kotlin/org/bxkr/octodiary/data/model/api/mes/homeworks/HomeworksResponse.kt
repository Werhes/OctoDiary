package org.bxkr.octodiary.data.model.api.mes.homeworks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeworksResponse(
    @SerialName("payload") val payload: List<HomeworkItem> = emptyList()
)

@Serializable
data class HomeworkItem(
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("subject_name") val subjectName: String = "",
    @SerialName("homework") val homework: String? = null,
    @SerialName("date") val date: String,
    @SerialName("is_done") val isDone: Boolean? = null
)