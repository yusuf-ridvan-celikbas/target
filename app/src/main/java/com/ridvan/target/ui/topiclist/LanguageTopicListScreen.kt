package com.ridvan.target.ui.topiclist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LanguageTopicListScreen(
    onTopicClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: LanguageTopicListViewModel = viewModel(),
) {
    val languageName by viewModel.languageName.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    TopicListContent(
        title = languageName,
        topics = topics,
        onAdd = viewModel::addTopic,
        onTopicClick = onTopicClick,
        onBack = onBack,
    )
}
