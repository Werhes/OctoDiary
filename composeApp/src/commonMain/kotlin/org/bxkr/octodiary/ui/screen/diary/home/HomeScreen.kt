package org.bxkr.octodiary.ui.screen.diary.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import octodiary4.composeapp.generated.resources.Res
import octodiary4.composeapp.generated.resources.dashboard
import org.bxkr.octodiary.domain.model.user.UserProfile
import org.bxkr.octodiary.presentation.viewmodel.diary.ProfileViewModel
import org.bxkr.octodiary.ui.component.AnimatedVisibilityFade
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val profileUiState by profileViewModel.uiState.collectAsState()

    LaunchedEffect(profileUiState.needToLoad) {
        if (profileUiState.needToLoad) profileViewModel.loadProfile()
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "OctoDiary",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        AnimatedVisibilityFade(profileUiState.isLoading) {
            LoadingIndicator(
                Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            )
        }
        profileUiState.profile?.let { profile ->
            ProfileCard(profile)
        }
    }
}

@Composable
private fun ProfileCard(profile: UserProfile) {
    Card(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            profile.students.forEach { student ->
                Text(
                    text = listOfNotNull(student.lastName, student.firstName, student.middleName)
                        .joinToString(" "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun HomeTitle() {
    Text(stringResource(Res.string.dashboard))
}