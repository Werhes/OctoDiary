package org.bxkr.octodiary.data.repository.diary

import kotlinx.datetime.LocalDateRange
import org.bxkr.octodiary.data.datasource.local.CacheLocalDataSource
import org.bxkr.octodiary.data.datasource.remote.MesLikeRemoteDataSource
import org.bxkr.octodiary.data.mapper.mes.toDomain
import org.bxkr.octodiary.data.model.auth.accesscredentials.token.MesToken
import org.bxkr.octodiary.domain.exception.diary.NotAuthorizedException
import org.bxkr.octodiary.domain.exception.diary.NotAuthorizedType
import org.bxkr.octodiary.domain.exception.diary.UnknownDiaryException
import org.bxkr.octodiary.domain.model.event.Event
import org.bxkr.octodiary.domain.model.group.Group
import org.bxkr.octodiary.domain.model.homework.HomeworkEntry
import org.bxkr.octodiary.domain.model.log.LogLevel
import org.bxkr.octodiary.domain.model.log.LogTemplate
import org.bxkr.octodiary.domain.model.organization.Organization
import org.bxkr.octodiary.domain.model.ranking.Ranking
import org.bxkr.octodiary.domain.model.user.UserProfile
import org.bxkr.octodiary.domain.repository.DiaryRepository
import org.bxkr.octodiary.domain.repository.Logger
import org.bxkr.octodiary.domain.repository.SessionRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
abstract class MesLikeRepositoryImpl(
    private val mesLikeRemoteDataSource: MesLikeRemoteDataSource,
    private val sessionRepository: SessionRepository,
    private val cacheLocalDataSource: CacheLocalDataSource,
    private val logger: Logger
) : DiaryRepository {
    private val cacheLifetime = 12.hours

    @OptIn(ExperimentalTime::class)
    protected fun isCacheExpired(cachedAtMillis: Long): Boolean {
        val cacheLifetimeMillis = cacheLifetime.inWholeMilliseconds
        return (Clock.System.now().toEpochMilliseconds() - cachedAtMillis) > cacheLifetimeMillis
    }

    private data class DiaryContext(val personIds: String, val studentId: Long)

    /**
     * Resolves the identifiers required by the family/mobile API from the account profile.
     * MES uses different identifiers than the JWT subject:
     *  - events expect the child's `contingent_guid` (a UUID) as `person_ids`;
     *  - homeworks expect the child's numeric `id` as `student_id`.
     * Falls back to the token's own person id when there are no children (e.g. a student account).
     */
    private suspend fun getDiaryContext(accessToken: MesToken): Result<DiaryContext> {
        val profile = mesLikeRemoteDataSource.getProfile(accessToken)
            .getOrElse { return Result.failure(it) }
        val child = profile.children.firstOrNull()
        return Result.success(
            DiaryContext(
                personIds = child?.contingentGuid ?: accessToken.personId,
                studentId = (child?.id ?: profile.profile.id).toLong()
            )
        )
    }

    abstract suspend fun getAccessToken(): MesToken?

    final override suspend fun getProfile(): Result<UserProfile> {
        val cacheEntry = cacheLocalDataSource.getProfile()
        if (cacheEntry != null && !isCacheExpired(cacheEntry.cachedAt)) {
            return Result.success(cacheEntry.profile)
        }
        val accessToken = getAccessToken() ?: return Result.failure(
            NotAuthorizedException(
                NotAuthorizedType.AccessCredentialsNotFound
            )
        )

        if (!accessToken.isAlive()) return Result.failure(
            NotAuthorizedException(
                NotAuthorizedType.ExpiredAccessCredentials
            )
        )

        val remoteResult = mesLikeRemoteDataSource.getProfile(accessToken)

        return remoteResult.fold(onSuccess = {
            logger.log(
                LogTemplate.successfullyLoaded(
                    loadedPartName = "profile", source = "$responsibleFor repository"
                )
            )
            val domainProfile = it.toDomain()
            cacheLocalDataSource.saveProfile(
                domainProfile, Clock.System.now().toEpochMilliseconds()
            )
            Result.success(domainProfile)
        }, onFailure = {
            logger.log(
                ("Couldn't load profile" +
                        "| - $responsibleFor repository" +
                        "| - exception name: ${it::class.simpleName}" +
                        "| - message: ${it.message}" +
                        "| - stack trace: ${it.stackTraceToString()}").trimMargin(),
                LogLevel.ERROR
            )
            Result.failure(it)
        })
    }

    final override suspend fun getSchedule(dateRange: LocalDateRange): Result<List<Event>> {
        val accessToken = getAccessToken() ?: return Result.failure(
            NotAuthorizedException(
                NotAuthorizedType.AccessCredentialsNotFound
            )
        )
        if (!accessToken.isAlive()) return Result.failure(
            NotAuthorizedException(
                NotAuthorizedType.ExpiredAccessCredentials
            )
        )

        val context = getDiaryContext(accessToken).getOrElse { return Result.failure(it) }

        val remoteResult = mesLikeRemoteDataSource.getEvents(
            accessToken,
            context.personIds,
            dateRange.start.toString(),
            dateRange.endInclusive.toString()
        )

        return remoteResult.fold(
            onSuccess = { response ->
                logger.log(
                    LogTemplate.successfullyLoaded(
                        loadedPartName = "schedule", source = "$responsibleFor repository"
                    )
                )
                Result.success(response.toDomain())
            },
            onFailure = {
                logger.log(
                    ("Couldn't load schedule" +
                        "| - $responsibleFor repository" +
                        "| - exception name: ${it::class.simpleName}" +
                        "| - message: ${it.message}").trimMargin(),
                    LogLevel.ERROR
                )
                Result.failure(it)
            }
        )
    }

    final override suspend fun getHomeworkEntries(
        dateRange: LocalDateRange
    ): Result<List<HomeworkEntry>> {
        val accessToken = getAccessToken() ?: return Result.failure(
            NotAuthorizedException(
                NotAuthorizedType.AccessCredentialsNotFound
            )
        )
        if (!accessToken.isAlive()) return Result.failure(
            NotAuthorizedException(
                NotAuthorizedType.ExpiredAccessCredentials
            )
        )
        val context = getDiaryContext(accessToken).getOrElse { return Result.failure(it) }

        val remoteResult = mesLikeRemoteDataSource.getHomeworks(
            accessToken,
            context.studentId,
            dateRange.start.toString(),
            dateRange.endInclusive.toString()
        )

        return remoteResult.fold(
            onSuccess = { response ->
                logger.log(
                    LogTemplate.successfullyLoaded(
                        loadedPartName = "homeworkEntries", source = "$responsibleFor repository"
                    )
                )
                Result.success(response.toDomain())
            },
            onFailure = {
                logger.log(
                    ("Couldn't load homeworks" +
                        "| - $responsibleFor repository" +
                        "| - exception name: ${it::class.simpleName}" +
                        "| - message: ${it.message}").trimMargin(),
                    LogLevel.ERROR
                )
                Result.failure(it)
            }
        )
    }

    final override suspend fun getOrganization(): Result<Organization> {
        TODO("Not yet implemented")
    }

    final override suspend fun getGroups(): Result<List<Group>> {
        TODO("Not yet implemented")
    }

    final override suspend fun getRanking(): Result<Ranking> {
        TODO("Not yet implemented")
    }

    final override suspend fun getEventDescription(eventId: String): Result<Event> {
        TODO("Not yet implemented")
    }
}