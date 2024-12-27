#include <cassert>
#include <cstdint>
#include <cstdlib>
#include <cstdio>
#include "VCl2Core__Dpi.h"

#define PMEM_BASE 0x80000000U
#define PMEM_SIZE 0x8000000U

static uint8_t mem[PMEM_SIZE] = {
  0x97, 0x02, 0x00, 0x00, // auipc t0,0
  0x23, 0x88, 0x02, 0x00, // sb zero, 16(t0)
  0x03, 0xc5, 0x02, 0x01, // lbu a0, 16(t0)
  0x73, 0x00, 0x10, 0x00  // ebreak
};

extern "C" unsigned int mem_read(uint32_t raddr, uint32_t mask) {
  printf("The raddr is: %x\n", raddr);
   assert(raddr >= PMEM_BASE && (raddr - PMEM_SIZE >= 0));
   return *(uint32_t *)(mem + raddr - PMEM_BASE);
}

extern "C" void mem_write(uint32_t waddr, uint32_t mask, uint32_t wdata) {

}
