package com.itkacher.raspberry.pico.registry

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform