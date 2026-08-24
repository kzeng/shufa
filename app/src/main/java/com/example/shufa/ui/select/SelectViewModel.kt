package com.example.shufa.ui.select

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shufa.data.PostRepository
import com.example.shufa.model.CalligraphyPost
import com.example.shufa.model.CalligraphyStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SelectUiState(
    val posts: List<CalligraphyPost> = emptyList(),
    val selectedStyle: CalligraphyStyle? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class SelectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    private val _uiState = MutableStateFlow(SelectUiState())
    val uiState: StateFlow<SelectUiState> = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            try {
                val posts = repository.getPosts()
                _uiState.value = SelectUiState(
                    posts = posts,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = SelectUiState(
                    posts = emptyList(),
                    isLoading = false
                )
            }
        }
    }

    fun filterByStyle(style: CalligraphyStyle?) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredPosts(): List<CalligraphyPost> {
        val state = _uiState.value
        return state.posts.filter { post ->
            val matchesStyle = state.selectedStyle == null || post.style == state.selectedStyle
            val matchesSearch = state.searchQuery.isEmpty() ||
                post.title.contains(state.searchQuery, ignoreCase = true) ||
                post.author.contains(state.searchQuery, ignoreCase = true) ||
                post.dynasty.contains(state.searchQuery, ignoreCase = true) ||
                post.style.label.contains(state.searchQuery, ignoreCase = true)
            matchesStyle && matchesSearch
        }
    }
}
