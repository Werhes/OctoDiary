package org.bxkr.octodiary.ui.screen.diary.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import octodiary4.composeapp.generated.resources.Res
import octodiary4.composeapp.generated.resources.profile
import org.bxkr.octodiary.domain.model.student.Student
import org.bxkr.octodiary.domain.model.user.UserProfile
import org.bxkr.octodiary.presentation.viewmodel.diary.ProfileViewModel
import org.bxkr.octodiary.ui.component.AnimatedVisibilityFade
import org.bxkr.octodiary.ui.component.AnimatedVisibilityFadeNotNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.needToLoad) {
        if (uiState.needToLoad) {
            viewModel.loadProfile()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibilityFade(uiState.isLoading) { LoadingIndicator() }
        AnimatedVisibilityFadeNotNull(uiState.profile) { profile -> ProfileDescription(profile) }
    }
}

@Composable
fun ProfileTitle() {
    Text(stringResource(Res.string.profile))
}

@Composable
private fun ProfileDescription(profile: UserProfile) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        items(profile.students, key = { it.studentId }) { student ->
            StudentCard(student)
        }
    }
}

@Composable
private fun StudentCard(student: Student) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = listOfNotNull(student.lastName, student.firstName, student.middleName)
                    .joinToString(" "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = student.studentId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}