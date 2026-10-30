# Native part of the Android build: libSDL2.so + libmain.so (recompiled game + runtime).
# Configured by build.ps1 with the NDK toolchain; sources come from ../GTStaticRecomp.
cmake_minimum_required(VERSION 3.20)
project(GTStaticRecompAndroid C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 11)

set(GT_ROOT "${CMAKE_SOURCE_DIR}/../GTStaticRecomp" CACHE PATH "GTStaticRecomp checkout (runtime sources)")
set(GEN_DIR "${CMAKE_SOURCE_DIR}/build/gen" CACHE PATH "Output of gen/recomp.py")
if(NOT EXISTS "${GEN_DIR}/func_table.c")
  message(FATAL_ERROR "Run gen/recomp.py first (see build.ps1); expected ${GEN_DIR}/func_table.c")
endif()

# ---- SDL2 (shared: SDLActivity loads libSDL2.so, then libmain.so) -----------
set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
set(SDL_TEST OFF CACHE BOOL "" FORCE)
include(FetchContent)
FetchContent_Declare(SDL2 GIT_REPOSITORY https://github.com/libsdl-org/SDL.git GIT_TAG release-2.30.9 GIT_SHALLOW TRUE)
FetchContent_MakeAvailable(SDL2)

# 16 KB page alignment (required on devices with 16 KB pages)
add_link_options(-Wl,-z,max-page-size=16384)

# 32-bit ARM (Android TVs): ARM instead of Thumb-2 code, ARMv7VE for the hardware divide
# instruction, scheduled for the Cortex-A55 most current TV chips use
if(ANDROID_ABI STREQUAL "armeabi-v7a")
  add_compile_options(-marm -march=armv7ve -mtune=cortex-a55)
endif()

# Save states restore the game's native stack in a later process (runtime/state.cpp), so the
# frames on it must not depend on per-process secrets: no stack-protector canaries (a restored
# frame would carry the old process's canary and abort on return) and no signed return
# addresses (arm64 pointer authentication keys are per process too).
set(NO_STACK_SECRETS -fno-stack-protector)
if(ANDROID_ABI STREQUAL "arm64-v8a")
  list(APPEND NO_STACK_SECRETS -mbranch-protection=none)
endif()

# ---- generated code ---------------------------------------------------------
file(GLOB GEN_SOURCES "${GEN_DIR}/funcs_*.c" "${GEN_DIR}/func_table.c")
add_library(gt_gen STATIC ${GEN_SOURCES})
target_include_directories(gt_gen PUBLIC "${GT_ROOT}/runtime" "${GEN_DIR}")
target_compile_options(gt_gen PRIVATE -O2 -fwrapv -fno-strict-aliasing -w ${NO_STACK_SECRETS})

# ---- runtime: libmain.so (SDL calls SDL_main in it) --------------------------
set(RT "${GT_ROOT}/runtime")
add_library(main SHARED
  ${RT}/main.cpp ${RT}/cpu.cpp ${RT}/hw.cpp ${RT}/bios.cpp ${RT}/gte.cpp ${RT}/gpu.cpp
  ${RT}/spu.cpp ${RT}/cdrom.cpp ${RT}/sio.cpp ${RT}/mdec.cpp ${RT}/crash.cpp ${RT}/hooks.cpp
  ${RT}/pgxp.cpp ${RT}/state.cpp ${RT}/jmp.c)
target_include_directories(main PRIVATE "${RT}")
target_compile_options(main PRIVATE -O2 -fwrapv -fno-strict-aliasing -Wall -Wno-unused-variable -Wno-unused-function ${NO_STACK_SECRETS})
target_link_libraries(main PRIVATE gt_gen SDL2::SDL2 android log)
