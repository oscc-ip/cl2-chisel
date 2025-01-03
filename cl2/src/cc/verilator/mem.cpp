#include <cassert>
#include <cstdint>
#include <cstdlib>
#include <cstdio>
#include <sim.h>
#include "VCl2Top__Dpi.h"


uint8_t mem[PMEM_SIZE] = {
  0x97, 0x02, 0x00, 0x00, // auipc t0,0
  0x23, 0x88, 0x02, 0x00, // sb zero, 16(t0)
  0x03, 0xc5, 0x02, 0x01, // lbu a0, 16(t0)
  0x73, 0x00, 0x10, 0x00  // ebreak
};

unsigned int load_img(char *path) {
  assert(mem);
  assert(path);
  FILE *img = fopen(path, "rb");
  assert(img);

  fseek(img, 0, SEEK_END);
  unsigned int size = ftell(img);
  if (size > PMEM_SIZE) {
    fclose(img);
    return 0;
  }
  fseek(img, 0, SEEK_SET);
  int ret = fread(mem, size, 1, img);
  if (ret != 1) {
    fclose(img);
    return 0;
  }

  fclose(img);
  return size;
}
extern void sim_exit();
//mmio devices ?
extern "C" unsigned int mem_read(uint32_t raddr) {
  // printf("The raddr is: %x\n", raddr);
  if(raddr < RESET_VECTOR || (raddr - RESET_VECTOR >= PMEM_SIZE)) {
    sim_exit();
    assert(0);
  }
  return *(uint32_t *)(mem + raddr - RESET_VECTOR);
}

extern "C" void mem_write(uint32_t waddr, uint32_t mask, uint32_t wdata) {
  uintptr_t addr = waddr;
  switch (mask) {
    case 0x1: *(volatile uint8_t*)addr = wdata;
    case 0x11: *(volatile uint16_t*)addr = wdata;
    case 0x1111: *(volatile uint32_t*)addr = wdata;
    default: assert(0);
  }


}
