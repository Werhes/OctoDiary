package org.bxkr.octodiary.data.mapper.mes

import kotlinx.datetime.LocalDateTime
import org.bxkr.octodiary.data.model.api.mes.events.EventHomework
import org.bxkr.octodiary.data.model.api.mes.events.EventItem
import org.bxkr.octodiary.data.model.api.mes.events.EventMark
import org.bxkr.octodiary.data.model.api.mes.events.EventsResponse
import org.bxkr.octodiary.domain.model.event.Event
import org.bxkr.octodiary.domain.model.event.EventAdditionalInformation
import org.bxkr.octodiary.domain.model.event.EventType
import org.bxkr.octodiary.domain.model.homework.Homework
import org.bxkr.octodiary.domain.model.mark.Mark
import org.bxkr.octodiary.domain.model.mark.MarkComponent
import org.bxkr.octodiary.domain.model.subject.Subject

fun EventsResponse.toDomain(): List<Event> = response.map { it.toDomain() }

private fun EventItem.toDomain(): Event {
    val subject = Subject(
        id = subjectId?.toString() ?: id.toString(),
        name = subjectName ?: lessonName ?: title ?: ""
    )
    return Event(
        eventId = id.toString(),
        eventName = subjectName ?: lessonName ?: title ?: "",
        type = EventType.MainPlan,
        timeStart = parseDateTime(startAt),
        timeEnd = parseDateTime(finishAt),
        isAllDay = isAllDay,
        subject = subject,
        homework = homework?.toDomain(id),
        marks = marks.map { it.toDomain(subject) },
        additionalInformation = description?.let { EventAdditionalInformation(description = it) }
    )
}

private fun EventHomework.toDomain(eventId: Long): Homework = Homework(
    homeworkId = eventId.toString(),
    text = descriptions.joinToString("\n").ifBlank { null }
)

private fun EventMark.toDomain(subject: Subject): Mark = Mark(
    subject = subject,
    components = listOf(
        MarkComponent.IntegerMark(value = value.toIntOrNull() ?: 0, weight = weight)
    ),
    workType = controlFormName
)

private fun parseDateTime(value: String?): LocalDateTime? {
    if (value.isNullOrBlank()) return null
    // Timestamps may include a UTC offset or Z suffix; treat the wall-clock part as local time.
    val cleaned = value.substringBefore('+').substringBefore('Z')
    return try {
        LocalDateTime.parse(cleaned)
    } catch (e: Exception) {
        null
    }
}