/***************************************************************************************************
*  File:        eeprom.c                                                                           *
*  Authors:     Joan Francesc Muñoz Martin <JFM>                                                   *
*                                                                                                  *
*  Creation:    20-02-2018                                                                         *
*  Description: EEPROM emulation over STM32 flash memory                                           *
*                                                                                                  *
*  This file is part of a project developed by Nano-Satellite and Payload Laboratory (NanoSat Lab) *
*  at Technical University of Catalonia - UPC BarcelonaTech.                                       *
*                                                                                                  *
* ------------------------------------------------------------------------------------------------ *
*  Changelog:                                                                                      *
*  v#   Date            Author  Description                                                        *
*  0.1  20-02-2018      <JFM>   <First version>                                                    *
***************************************************************************************************/
#include "eeprom.h"

/**
 * @brief This function writes a set of data bytes ("data_size" bytes)
 * into an Eeprom memory. These bytes are retrieved from the "data" buffer
 * and stored in the Eeprom memory with "id" identifier.
 */
int write_eeprom(int id, uint64_t *data, size_t data_size)
{
    /* write a eeprom structure */
    FLASH_EraseInitTypeDef erase_cmd;
    uint32_t page_error;
    uint32_t double_word_size = data_size/sizeof(uint64_t);
    int i;

    if(id != EP_EEPROM_ID_SYSTEM_BOOT_COUNT &&
       id != EP_EEPROM_ID_TX_FREQ) {
        return -1;
    }

    taskENTER_CRITICAL();
    HAL_FLASH_Unlock();
    __HAL_FLASH_CLEAR_FLAG(FLASH_FLAG_EOP | FLASH_FLAG_OPERR | FLASH_FLAG_WRPERR |
                               FLASH_FLAG_PGAERR | FLASH_FLAG_PGSERR);

    erase_cmd.TypeErase = FLASH_TYPEERASE_PAGES;
    erase_cmd.Banks = FLASH_BANK_1;
    erase_cmd.Page = EP_FLASH_PAGE_START + id;
    erase_cmd.NbPages = 1;
    HAL_FLASHEx_Erase(&erase_cmd, &page_error);
    /* flash write */
    for(i = 0; i < double_word_size; i++) {
        HAL_FLASH_Program(  FLASH_TYPEPROGRAM_DOUBLEWORD,
                            (uint32_t) EP_EEPROM_START_ADDR + id * EP_FLASH_PAGE_SIZE + i * sizeof(uint64_t),
                            (uint64_t) data[i]);
    }
    HAL_FLASH_Lock();
    taskEXIT_CRITICAL();

    return 0;
}

/**
 * @brief This function reads the Eeprom memory installed in the RF ISL Module.
 * In particular it is able to store "data_size" bytes into the "data" buffer.
 * The input "id" parameter identify the Eeprom identifier.
 */
int read_eeprom(int id, uint64_t *data, size_t data_size)
{
    int i;
    uint32_t double_word_size = data_size/sizeof(uint64_t);
    const volatile uint64_t *aux;

    if(id != EP_EEPROM_ID_SYSTEM_BOOT_COUNT &&
       id != EP_EEPROM_ID_TX_FREQ) {
        return -1;
    }

    for(i = 0; i < double_word_size; i++) {
        aux = (const volatile uint64_t *) (EP_EEPROM_START_ADDR + id*EP_FLASH_PAGE_SIZE + i*sizeof(uint64_t));
        data[i] = *aux;
    }
    return 0;
}
