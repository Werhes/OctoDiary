package org.bxkr.octodiary.data.gateway

import io.github.xxfast.kstore.KStore
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import okio.ByteString.Companion.toByteString
import org.bxkr.octodiary.data.StorageLatest
import org.bxkr.octodiary.data.datasource.remote.MesMosRemoteDataSource
import org.bxkr.octodiary.data.exception.callbackfailure.*
import org.bxkr.octodiary.data.model.api.mes.auth.IssueCallResponse
import org.bxkr.octodiary.data.model.api.mes.profile.Profile
import org.bxkr.octodiary.data.model.auth.MosRuInfo
import org.bxkr.octodiary.data.model.auth.accesscredentials.token.*
import org.bxkr.octodiary.data.toAuthStepFailure
import org.bxkr.octodiary.di.annotation.MainStorage
import org.bxkr.octodiary.domain.ExternalIntegration
import org.bxkr.octodiary.domain.gateway.AuthGateway
import org.bxkr.octodiary.domain.model.auth.*
import org.bxkr.octodiary.domain.model.diary.DiaryId
import org.bxkr.octodiary.domain.model.region.RegionCode
import org.bxkr.octodiary.domain.model.user.UserType
import org.koin.core.annotation.Single
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Single
class MesMosAuthGateway(
    @param:MainStorage private val kStore: KStore<StorageLatest>,
    private val mesMosRemoteDataSource: MesMosRemoteDataSource
) : AuthGateway {
    override val responsibleFor: DiaryId
        get() = DiaryId.MesMos

    private object MosRuConstants {
        // Единая ссылка: пользователь авторизуется на school.mos.ru, после чего сайт
        // автоматически переходит (через backUrl) на страницу с токеном.
        const val SCHOOL_LOGIN_URL =
            "https://school.mos.ru/?backUrl=https%3A%2F%2Fschool.mos.ru%2Fv2%2Ftoken%2Frefresh%3FroleId%3D1%26subsystem%3D2"
        const val TOKEN_REFRESH_URL = "https://school.mos.ru/v2/token/refresh?roleId=1&subsystem=2"
    }

    private object DeeplinkConstants {
        const val MOS_SCHEME = "dnevnik-mes"
        const val MOS_HOST = "oauth2redirect"
        const val MOS_CODE_PARAMETER_NAME = "code"
        const val OCTODIARY_SCHEME = "octodiary"
        const val TELEGRAM_HOST = "tgbot"
    }

    private suspend fun getGatewayStorage(): AuthGatewayStorage.MesMos? =
        kStore.get()?.authGatewayStorage as? AuthGatewayStorage.MesMos


    override suspend fun processAuthStep(credentials: Credentials): AuthStepResult =
        when (credentials) {
            is Credentials.FreshAuth -> initializeAuth(credentials.authMethod)
            is Credentials.AccessToken -> authorizeByToken(credentials.accessToken)
        }

    private suspend fun initializeAuth(authMethod: AuthMethod): AuthStepResult = when (authMethod) {
        is AuthMethod.InBrowser.MosRu -> initializeMosRuAuth(authMethod)
        is AuthMethod.WebView.MosRu -> initializeMosRuAuth(authMethod)
        is AuthMethod.Telegram -> proceedWithTelegramAuth()
        is AuthMethod.AccessToken -> AuthStepResult.ProceedWithAuthMethod(AuthMethodData.Proceed)
        else -> IllegalStateException("UnsupportedAuthMethod").toAuthStepFailure()
    }

    private suspend fun initializeMosRuAuth(method: AuthMethod): AuthStepResult =
        // МЭШ: сначала пользователь авторизуется на school.mos.ru, затем приложение
        // открывает страницу с токеном (token/refresh), откуда пользователь копирует токен
        // и вставляет его в приложение.
        AuthStepResult.ProceedWithAuthMethod(
            AuthMethodData.GoToUrl(
                url = MosRuConstants.SCHOOL_LOGIN_URL,
                isWebView = true,
                tokenUrl = MosRuConstants.TOKEN_REFRESH_URL
            )
        )

    private fun proceedWithTelegramAuth() = AuthStepResult.ProceedWithAuthMethod(
        AuthMethodData.GoToUrl(
            ExternalIntegration.getTelegramAuthLink(RegionCode.Moscow.code), false
        )
    )

    private fun authorizeByToken(accessToken: String): AuthStepResult {
        val mesToken = MesToken(accessToken)
        return if (mesToken.payload != null) {
            AuthStepResult.Success(
                AccessCredentials.MesMosAccessCredentials(mesToken, null, null)
            )
        } else IllegalStateException("Invalid MES JWT token").toAuthStepFailure()
    }

    override fun handleCallback(
        callbackLink: String, method: AuthMethod
    ): Flow<CallbackState> = flow {
        emit(CallbackState.Loading)
        when (method) {
            AuthMethod.InBrowser.MosRu, AuthMethod.WebView.MosRu -> handleMosRuCallback(callbackLink)
            AuthMethod.Telegram -> handleTelegramCallback(callbackLink)
            else -> throw CallbackHandlingFailureException(InvalidAuthMethodError())
        }
    }

    private suspend fun handleMosRuCallback(callbackLink: String) = try {
        val url = URLBuilder(callbackLink)
        if (url.protocol.name != DeeplinkConstants.MOS_SCHEME || url.host != DeeplinkConstants.MOS_HOST) throw CallbackHandlingFailureException(
            InvalidLinkFormatError()
        )
        val code = url.parameters[DeeplinkConstants.MOS_CODE_PARAMETER_NAME]
            ?: throw CallbackHandlingFailureException(InvalidLinkFormatError())
        val mosRuInfo = getGatewayStorage()?.mosRuInfo ?: throw CallbackHandlingFailureException(
            AuthGatewayDataNotFoundError()
        )
        handleMosRuCode(code, mosRuInfo)
    } catch (_: URLParserException) {
        throw CallbackHandlingFailureException(InvalidLinkFormatError())
    }

    private suspend fun handleTelegramCallback(callbackLink: String) {
        TODO()
    }

    private suspend fun handleMosRuCode(
        code: String, mosRuInfo: MosRuInfo
    ) {
        val handleCodeResult = mesMosRemoteDataSource.handleCode(code, mosRuInfo)
        val tokenExchange = handleCodeResult.getOrElse {
            throw CallbackHandlingFailureException(
                CodeHandlingError(it.message, it.stackTraceToString())
            )
        }
        val mosToMesResult = mesMosRemoteDataSource.mosToMes(tokenExchange.accessToken)
        val mesToken = mosToMesResult.getOrElse {
            throw CallbackHandlingFailureException(
                CodeHandlingError(it.message, it.stackTraceToString())
            )
        }
        kStore.update {
            it?.copy(
                accessCredentials = AccessCredentials.MesMosAccessCredentials(
                    mesToken, mosRuInfo, tokenExchange.refreshToken
                ),
                callbackAuthState = null
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun checkToken(token: String): Flow<TokenInfo> = flow<TokenInfo> {
        val jwtPayload =
            token.jwtPayloadTyped<MesPayload>() ?: token.jwtPayloadTyped<UchebnikPayload>()
        if (jwtPayload == null) return@flow emit(TokenInfo.Unsupported)
        if (jwtPayload is UchebnikPayload) {
            val uchebnikStandard = TokenInfo.Standard(
                furtherLoading = true,
                actualDiaryName = "Библиотека МЭШ",
                expiryTime = Instant.fromEpochSeconds(jwtPayload.expiryDate),
                issueTime = Instant.fromEpochSeconds(jwtPayload.issuedAt),
                userId = jwtPayload.mesPersonId
            )
            emit(uchebnikStandard)
            val mesToken = mesMosRemoteDataSource.toSchoolToken(UchebnikToken(token)).getOrElse {
                return@flow emit(
                    uchebnikStandard.copy(
                        furtherLoading = false, canLogIn = false
                    )
                )
            }
            checkMesToken(mesToken, "Библиотека МЭШ")
        } else if (jwtPayload is MesPayload) {
            checkMesToken(MesToken(token))
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun FlowCollector<TokenInfo>.checkMesToken(
        token: MesToken, originalDiaryName: String? = null
    ) {
        val standard = TokenInfo.Standard(
            furtherLoading = true,
            actualDiaryName = originalDiaryName ?: "МЭШ Москва",
            expiryTime = token.expirationDate,
            issueTime = token.issuedAt,
            userId = token.personId
        )
        emit(standard)
        val profile = mesMosRemoteDataSource.getProfile(token).getOrElse {
            return emit(
                standard.copy(
                    furtherLoading = false, cannotLoadFurther = true, canLogIn = false
                )
            )
        }
        val studentName =
            if (profile.profile.role == Profile.UserType.Student) profile.profile.fullName
            else profile.children.firstOrNull()?.fullName
        emit(
            TokenInfo.Extended(
                standard.copy(
                    furtherLoading = false, canLogIn = true
                ),
                userName = profile.profile.fullName,
                studentName = studentName,
                role = when (profile.profile.role) {
                    Profile.UserType.Student -> UserType.Student
                    Profile.UserType.Parent -> UserType.Parent
                    else -> null
                },
                schoolName = profile.children.firstOrNull()?.school?.name
            )
        )
    }
}