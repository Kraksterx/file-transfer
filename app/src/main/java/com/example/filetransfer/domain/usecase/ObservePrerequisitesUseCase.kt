package com.example.filetransfer.domain.usecase

import com.example.filetransfer.domain.model.PrerequisiteState
import com.example.filetransfer.domain.repository.P2pRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePrerequisitesUseCase @Inject constructor(
    private val repository: P2pRepository
) {
    operator fun invoke(): Flow<PrerequisiteState> {
        return repository.prerequisitesState
    }
}
