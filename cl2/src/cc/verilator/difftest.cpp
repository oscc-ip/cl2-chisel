#include <cassert>
#include <complex>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <dlfcn.h>
#include <sim.h>


void (*ref_difftest_memcpy)(unsigned int addr, void *buf, size_t n,
                            bool direction) = NULL;
void (*ref_difftest_regcpy)(void *dut, bool direction) = NULL;
void (*ref_difftest_exec)(uint64_t n) = NULL;

extern uint8_t mem[];
extern core_context_t context;
extern "C" void difftest_init(char *ref_so_file, long img_size, int port) {
  void *handle;
  handle = dlopen(ref_so_file, RTLD_LAZY);
  assert(handle);

  ref_difftest_memcpy = DIFF_MEMCPY dlsym(handle, "difftest_memcpy");
  assert(ref_difftest_memcpy);
  ref_difftest_regcpy = DIFF_REGCPY dlsym(handle, "difftest_regcpy");
  assert(ref_difftest_regcpy);
  ref_difftest_exec = DIFF_EXEC dlsym(handle, "difftest_exec");
  void (*ref_difftest_init)(int) = DIFF_INIT dlsym(handle, "difftest_init");
  assert(ref_difftest_init);

  ref_difftest_init(port);
  ref_difftest_memcpy(RESET_VECTOR, mem, img_size, DIFFTEST_TO_REF);
  context.pc = RESET_VECTOR;
  ref_difftest_regcpy((void*)&context, DIFFTEST_TO_REF);
}

// This function is so ugly !!!
extern "C" bool difftest_step() {
  printf("[Difftest] this is difftest\n");
  core_context_t tmp_context = {};
  ref_difftest_regcpy((void*)&tmp_context, DIFFTEST_TO_DUT);
  if(tmp_context.pc != context.pc) {
    printf("[difftest] ref PC: %#8x, dut PC: %#8x\n", tmp_context.pc, context.pc);
    return false;
  }
  ref_difftest_exec(1);
  ref_difftest_regcpy((void*)&tmp_context, DIFFTEST_TO_DUT);
  bool flag = true;
  for(int i = 0; i < 32; i++) {
    uint32_t dut = context.gpr[i];
    uint32_t ref = tmp_context.gpr[i];
    if(dut != ref) {
      printf("[difftest] register %d, dut v:%#8x, ref v:%#8x\n", i, dut, ref);
      flag = false;
    }
  }
  return flag;
}
