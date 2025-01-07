#include "verilated.h"
#include "verilated_fst_c.h"
#include "VCl2Top___024root.h"
#include "VCl2Top.h"
#include "VCl2Top__Dpi.h"
#include <cassert>
#include <csignal>
#include <cstdint>
#include <cstdio>
#include <csignal>
#include <unistd.h>
#include <sim.h>

#define DIFFTEST
//We can use enviroment variable to do this.
#define WAVE_PATH "cl2.fst" //Note that this is a relative path

VerilatedContext *ctx = NULL;
VerilatedFstC *tfp = NULL;
VCl2Top *top = NULL;

state core_state = RUNNING;
core_context_t context;

#ifdef DIFFTEST
extern "C" int difftest_step();
extern "C" void difftest_init(char *ref_so_file, long img_size, int port);
#endif

// #define DEBUG_DIFF

//This macro just flip a 1-bit signal.
#define Flipped(signal) (~(signal ^ (-2)))
static bool dump_enable = true;
static uint32_t res = 0;

extern "C" void single_cycle() {
  for(int i = 0; i < 2; i++) {
  top->clock = Flipped(top->clock);
  top->eval();
  ctx->timeInc(1);
  if(dump_enable)
    tfp->dump(ctx->time());
  }
}

void sim_init(int argc, char **argv) {
  ctx = new VerilatedContext;
  assert(ctx);
  top = new VCl2Top{ctx};
  assert(top);
  tfp = new VerilatedFstC;
  assert(tfp);
  Verilated::traceEverOn(true);
  top->trace(tfp, 4);
  tfp->open(WAVE_PATH);

  extern unsigned int load_img(char *path);
  uint32_t size = 0;
  //A simple judge
  if(argc == 1)
    size = 16;
  else
    size = load_img(argv[1]);
  #ifdef DIFFTEST
  difftest_init(argv[2], size, 0);
  #endif

}
// When the program exits abnormally, this function will not be called.
// It should be possible to resolve this with a handler.
void sim_exit() {
  tfp->close();
}
void signal_handler(int signum) {
  if(signum == SIGABRT) {
    sim_exit();
  signal(SIGABRT,  SIG_DFL);
  raise(SIGABRT);
  }

}

void sim_stop(uint32_t pc) {
  // printf("ebreak pc: %#8x\n", pc);
  int a0 = GPR(10);
  if(a0 == 0)
    core_state = QUIT;
  else
    core_state = ABORT;
}

extern "C" void ctx_update(uint32_t pc, uint32_t wen, uint32_t rd, uint32_t v) {
  //printf("[DPI_C] PC is: %#8x\n", pc);
  context.pc = pc;
  if(rd != 0 && wen)
    context.gpr[rd] = v;
  int res = difftest_step();
  if(!res)
    core_state = ABORT;
  

}
#define MAX_CYCLES 100000
static uint32_t total_cycles = 0;
int sim_work() {

  //reset
  top->clock = 0;
  top->reset = 1;
  single_cycle();
  single_cycle();
  top->reset = 0;

  uint32_t res = 0;
  while(total_cycles <= MAX_CYCLES) {
    single_cycle();
    total_cycles++;
    if(core_state == ABORT) {
      res = -1;
      break;
    }
    if(core_state == QUIT) {
      res = 0;
      break;
    }

  }
  if(total_cycles == MAX_CYCLES)
    printf("max cycles !!!!\n");

  return res;
}



int main(int argc, char **argv) {
  signal(SIGABRT, signal_handler);
  sim_init(argc, argv);
  int res = sim_work();
  sim_exit();
  return res;
}
