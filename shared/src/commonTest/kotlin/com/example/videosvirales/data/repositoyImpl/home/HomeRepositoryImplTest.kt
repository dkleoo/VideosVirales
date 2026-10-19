package com.example.videosvirales.data.repositoyImpl.home

import com.example.videosvirales.data.remote.home.HomeServices
import com.example.videosvirales.domain.repository.home.HomeRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HomeRepositoryImplTest {

    @Test
    fun returnsHomeInfoFromService() = runBlocking<Unit> {
        val repository: HomeRepository = HomeRepositoryImpl(StubHomeServices("Desktop"))

        val result = repository.getHomeInfo()

        assertIs<HomeResult.Success>(result)
        assertEquals("Desktop", result.info.platformName)
    }

    @Test
    fun returnsErrorWhenServiceFails() = runBlocking<Unit> {
        val repository: HomeRepository = HomeRepositoryImpl(StubHomeServices(null))

        val result = repository.getHomeInfo()

        assertIs<HomeResult.Error>(result)
    }
}

private class StubHomeServices(
    private val platformName: String?,
) : HomeServices {
    override fun getPlatformName(): String = platformName ?: error("Service failure")
}
