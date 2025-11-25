package com.example.recorder.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: RecordingRepository
) : ViewModel() {
    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings: StateFlow<List<Recording>> = _recordings

    init {
        viewModelScope.launch {
            repository.observeRecordings().collectLatest { list -> _recordings.value = list }
        }
    }
}
