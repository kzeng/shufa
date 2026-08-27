package com.example.shufa.ui.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shufa.data.PostRepository
import com.example.shufa.model.CalligraphyPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewUiState(
    val post: CalligraphyPost? = null,
    val isLoading: Boolean = true
)

class ViewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    private val _uiState = MutableStateFlow(ViewUiState())
    val uiState: StateFlow<ViewUiState> = _uiState.asStateFlow()

    fun loadPost(postId: String) {
        viewModelScope.launch {
            _uiState.value = ViewUiState(isLoading = true)
            val post = repository.getPostById(postId)
            _uiState.value = ViewUiState(
                post = post,
                isLoading = false
            )
        }
    }

    fun toggleFavorite() {
        val post = _uiState.value.post ?: return
        val newValue = !post.isFavorite
        _uiState.value = _uiState.value.copy(
            post = post.copy(isFavorite = newValue)
        )
        viewModelScope.launch {
            repository.setFavorite(post.id, newValue)
        }
    }
}
