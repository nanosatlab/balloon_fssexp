################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/User_Drivers/cc1101_routine.c \
../src/User_Drivers/command_parser.c \
../src/User_Drivers/eeprom.c \
../src/User_Drivers/freertos_routines.c \
../src/User_Drivers/housekeeping.c \
../src/User_Drivers/link_layer.c \
../src/User_Drivers/of_reed-solomon_gf_2_m_api.c \
../src/User_Drivers/rs_work.c \
../src/User_Drivers/simple_link.c \
../src/User_Drivers/utils.c 

OBJS += \
./src/User_Drivers/cc1101_routine.o \
./src/User_Drivers/command_parser.o \
./src/User_Drivers/eeprom.o \
./src/User_Drivers/freertos_routines.o \
./src/User_Drivers/housekeeping.o \
./src/User_Drivers/link_layer.o \
./src/User_Drivers/of_reed-solomon_gf_2_m_api.o \
./src/User_Drivers/rs_work.o \
./src/User_Drivers/simple_link.o \
./src/User_Drivers/utils.o 

C_DEPS += \
./src/User_Drivers/cc1101_routine.d \
./src/User_Drivers/command_parser.d \
./src/User_Drivers/eeprom.d \
./src/User_Drivers/freertos_routines.d \
./src/User_Drivers/housekeeping.d \
./src/User_Drivers/link_layer.d \
./src/User_Drivers/of_reed-solomon_gf_2_m_api.d \
./src/User_Drivers/rs_work.d \
./src/User_Drivers/simple_link.d \
./src/User_Drivers/utils.d 


# Each subdirectory must supply rules for building sources it contributes
src/User_Drivers/%.o: ../src/User_Drivers/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: MCU GCC Compiler'
	@echo $(PWD)
	arm-none-eabi-gcc -mcpu=cortex-m4 -mthumb -mfloat-abi=hard -mfpu=fpv4-sp-d16 '-D__weak=__attribute__((weak))' '-D__packed="__attribute__((__packed__))"' -DUSE_HAL_DRIVER -DSTM32L476xx -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/Drivers/STM32L4xx_HAL_Driver/Inc" -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/Drivers/STM32L4xx_HAL_Driver/Inc/Legacy" -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/Middlewares/Third_Party/FreeRTOS/Source/include" -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F" -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/Drivers/CMSIS/Device/ST/STM32L4xx/Include" -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/Drivers/CMSIS/Include" -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/src" -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/src/User_Drivers" -I"/home/noitty/GitHub/fsscat_fssexp/rf_isl/src/src/User_Drivers/galois_field_codes_utils" -Og -g3 -Wall -fmessage-length=0 -ffunction-sections -fdata-sections -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


