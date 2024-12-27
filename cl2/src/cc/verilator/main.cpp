#include "verilated.h"
#include "verilated_fst_c.h"
#include "VCl2Core.h"
#include <cassert>
#include <csignal>
#include <cstdint>
#include <cstdio>
#include <signal.h>
#include <unistd.h>

//We can use enviroment variable to do this.
#define WAVE_PATH "./build/wave/cl2.fst" //Note that this is a relative path

VerilatedContext *ctx = NULL;
VerilatedFstC *tfp = NULL;
VCl2Core *top = NULL;

//This macro just flip a 1-bit signal.
#define Flipped(signal) (~(signal ^ (-2)))
bool dump_enable = true;

extern "C" void single_cycle() {
  for(int i = 0; i < 2; i++) {
  top->clock = Flipped(top->clock);
  top->eval();
  ctx->timeInc(1);
  if(dump_enable)
    tfp->dump(ctx->time());
  }
}

void sim_init() {
  ctx = new VerilatedContext;
  assert(ctx);
  top = new VCl2Core{ctx};
  assert(top);
  tfp = new VerilatedFstC;
  assert(tfp);
  Verilated::traceEverOn(true);
  top->trace(tfp, 4);
  tfp->open(WAVE_PATH);

}
// When the program exits abnormally, this function will not be called.
// It should be possible to resolve this with a handler.
void sim_exit() {
  tfp->close();
}

void signal_handler(int signum) {
  if(signum == SIGABRT)
    sim_exit();
  signal(SIGABRT,  SIG_DFL);
  raise(SIGABRT);

}
int dummy_work() {
  printf("Hello, world!\n");
  return 0;
}

#define MAX_CYCLES 100
static uint32_t total_cycles = 0;
int sim_work() {

  //reset
  top->clock = 0;
  top->reset = 1;
  single_cycle();
  single_cycle();
  top->reset = 0;

  while(total_cycles <= MAX_CYCLES) {
    single_cycle();
    total_cycles++;
  }

  return 0;
}



int main(int argc, char **argv) {
  sim_init();
  signal(SIGABRT, signal_handler);
  Verilated::commandArgs(argc, argv);
  // int res = dummy_work();
  int res = sim_work();
  sim_exit();
  return res;
}
