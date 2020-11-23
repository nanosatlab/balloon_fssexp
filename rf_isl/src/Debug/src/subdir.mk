################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
S_SRCS += \
../src/startup_stm32l476xx.s 

C_SRCS += \
../src/main.c \
../src/stm32l4xx_hal_msp.c \
../src/stm32l4xx_hal_timebase_TIM.c \
../src/stm32l4xx_it.c \
../src/system_stm32l4xx.c 

OBJS += \
./src/main.o \
./src/startup_stm32l476xx.o \
./src/stm32l4xx_hal_msp.o \
./src/stm32l4xx_hal_timebase_TIM.o \
./src/stm32l4xx_it.o \
./src/system_stm32l4xx.o 

C_DEPS += \
./src/main.d \
./src/stm32l4xx_hal_msp.d \
./src/stm32l4xx_hal_timebase_TIM.d \
./src/stm32l4xx_it.d \
./src/system_stm32l4xx.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: MCU GCC Compiler'
	@echo $(PWD)
	arm-none-eabi-gcc -mcpu=cortex-m4 -mthumb -mfloat-abi=hard -mfpu=fpv4-sp-d16 '-D__weak=__attribute__((weak))' '-D__packed="__attribute__((__packed__))"' -DUSE_HAL_DRIVER -DSTM32L476xx -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Drivers/STM32L4xx_HAL_Driver/Inc" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Drivers/STM32L4xx_HAL_Driver/Inc/Legacy" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Middlewares/Third_Party/FreeRTOS/Source/include" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Drivers/CMSIS/Device/ST/STM32L4xx/Include" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Drivers/CMSIS/Include" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/src" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/src/User_Drivers" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/src/User_Drivers/galois_field_codes_utils" -Og -g3 -Wall -fmessage-length=0 -ffunction-sections -fdata-sections -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '

src/%.o: ../src/%.s
	@echo 'Building file: $<'
	@echo 'Invoking: MCU GCC Assembler'
	@echo $(PWD)
	arm-none-eabi-as -mcpu=cortex-m4 -mthumb -mfloat-abi=hard -mfpu=fpv4-sp-d16 -g -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


