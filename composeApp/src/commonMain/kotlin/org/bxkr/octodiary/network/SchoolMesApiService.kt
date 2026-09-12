package org.bxkr.octodiary.network

import org.bxkr.octodiary.data.model.api.mes.events.EventsResponse
import org.bxkr.octodiary.data.model.api.mes.homeworks.HomeworksResponse
import org.bxkr.octodiary.data.model.api.mes.profile.ProfileResponse
import org.bxkr.octodiary.data.model.auth.accesscredentials.token.MesToken

interface SchoolMesApiService {
    suspend fun getProfile(accessToken: MesToken): Result<ProfileResponse>

    suspend fun getHomeworks(
        accessToken: MesToken,
        studentId: Long,
        from: String,
        to: String
    ): Result<HomeworksResponse>

    suspend fun getEvents(
        accessToken: MesToken,
        personId: String,
        from: String,
        to: String
    ): Result<EventsResponse>
}