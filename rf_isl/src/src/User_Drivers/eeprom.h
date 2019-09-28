/***************************************************************************************************
*  File:        eeprom.h                                                                           *
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
#ifndef __EEPROM_H__
#define __EEPROM_H__

#include "stm32l4xx_hal.h"
#include "freertos_util.h"

#define EP_FLASH_START_ADDR     0x08000000
#define EP_FLASH_PAGE_SIZE      0x800
#define EP_FLASH_PAGE_START     128

/* Start address of eeprom memory */
#define EP_EEPROM_START_ADDR    (EP_FLASH_START_ADDR + (EP_FLASH_PAGE_SIZE * EP_FLASH_PAGE_START))

/* each data is stored in a different FLASH page */

#define EP_EEPROM_ID_SYSTEM_BOOT_COUNT      0
#define EP_EEPROM_ID_TX_FREQ      			1

typedef union __attribute__ ((__packed__)) SysBootCountEeprom {
    uint64_t raw[1];
    struct __attribute__ ((__packed__)) {
        uint8_t system_boot_count;
        /* padded to 64 bits thanks to UNION ;) */
    }fields;
}SysBootCountEeprom;

typedef union __attribute__ ((__packed__)) TransceiverConfiguration {
    uint64_t raw[1];
    struct __attribute__ ((__packed__)) {
        float freq; /* transceiver frequency */
    }fields;
    uint8_t   float_array[4];
}TransceiverConfiguration;

int write_eeprom(int id, uint64_t *data, size_t data_size);
int read_eeprom(int id, uint64_t *data, size_t data_size);

#endif /* USER_SRC_EEPROM_H_ */
