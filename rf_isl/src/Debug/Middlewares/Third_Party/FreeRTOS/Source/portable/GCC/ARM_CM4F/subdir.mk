################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F/port.c 

OBJS += \
./Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F/port.o 

C_DEPS += \
./Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F/port.d 


# Each subdirectory must supply rules for building sources it contributes
Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F/%.o: ../Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: MCU GCC Compiler'
	@echo $(PWD)
	arm-none-eabi-gcc -mcpu=cortex-m4 -mthumb -mfloat-abi=hard -mfpu=fpv4-sp-d16 '-D__weak=__attribute__((weak))' '-D__packed="__attribute__((__packed__))"' -DUSE_HAL_DRIVER -DSTM32L476xx -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Drivers/STM32L4xx_HAL_Driver/Inc" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Drivers/STM32L4xx_HAL_Driver/Inc/Legacy" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Middlewares/Third_Party/FreeRTOS/Source/include" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Drivers/CMSIS/Device/ST/STM32L4xx/Include" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/Drivers/CMSIS/Include" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/src" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/src/User_Drivers" -I"/home/noitty/GitHub/balloon_fssexp/rf_isl/src/src/User_Drivers/galois_field_codes_utils" -Og -g3 -Wall -fmessage-length=0 -ffunction-sections -fdata-sections -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


