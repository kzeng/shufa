package com.example.shufa.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shufa.data.PostRepository
import com.example.shufa.model.CalligraphyPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val posts: List<CalligraphyPost> = emptyList(),
    val isLoading: Boolean = true
)

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getFavoritePosts().collect { posts ->
                _uiState.value = FavoritesUiState(
                    posts = posts,
                    isLoading = false
                )
            }
        }
    }

    fun removeFavorite(postId: String) {
        viewModelScope.launch {
            repository.setFavorite(postId, false)
        }
    }
}
