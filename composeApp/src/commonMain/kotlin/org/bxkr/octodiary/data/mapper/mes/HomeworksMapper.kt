package org.bxkr.octodiary.data.mapper.mes

import kotlinx.datetime.LocalDate
import org.bxkr.octodiary.data.model.api.mes.homeworks.HomeworkItem
import org.bxkr.octodiary.data.model.api.mes.homeworks.HomeworksResponse
import org.bxkr.octodiary.domain.model.homework.Homework
import org.bxkr.octodiary.domain.model.homework.HomeworkEntry
import org.bxkr.octodiary.domain.model.subject.Subject

fun HomeworksResponse.toDomain(): List<HomeworkEntry> = payload.map { item -> item.toDomain() }

private fun HomeworkItem.toDomain(): HomeworkEntry {
    val subject = Subject(id = subjectId.toString(), name = subjectName)
    return HomeworkEntry(
        subject = subject,
        deadline = parseIsoDate(date),
        homeworks = listOf(
            Homework(
                homeworkId = "$subjectId-$date",
                text = homework,
                isDone = isDone
            )
        )
    )
}

private fun parseIsoDate(value: String): LocalDate = try {
    LocalDate.parse(value)
} catch (e: Exception) {
    LocalDate.fromEpochDays(0)
}