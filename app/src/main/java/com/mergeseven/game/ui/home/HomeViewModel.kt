package com.mergeseven.game.ui.home

import androidx.lifecycle.ViewModel
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.data.repository.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = userDataRepository.userProfile
}
