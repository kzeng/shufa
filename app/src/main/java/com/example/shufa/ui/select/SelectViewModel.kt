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
            val posts = repository.getPosts()
            _uiState.value = SelectUiState(
                posts = posts,
                isLoading = false
            )
        }
    }

    fun filterByStyle(style: CalligraphyStyle?) {
        _uiState.value = _uiState.value.copy(selectedStyle = style)
    }

    fun getFilteredPosts(): List<CalligraphyPost> {
        val state = _uiState.value
        return if (state.selectedStyle != null) {
            state.posts.filter { it.style == state.selectedStyle }
        } else {
            state.posts
        }
    }
}
