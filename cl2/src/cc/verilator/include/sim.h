#ifndef _SIM_H_
#define _SIM_H_

#include <cstdint>
typedef struct core_context {
  uint32_t gpr[32];
  uint32_t csr[16];
  uint32_t pc;
} core_context_t;

typedef enum state  {
  RUNNING,
  STOP,
  END,
  ABORT,
  QUIT
} state;

#define RESET_VECTOR 0x80000000U
#define PMEM_SIZE 0x8000000U

#define __EXPORT __atrribute__((visibility("default")))
enum {DIFFTEST_TO_DUT, DIFFTEST_TO_REF};
#define DIFF_MEMCPY (void (*)(unsigned int, void *, size_t, bool))
#define DIFF_REGCPY (void (*)(void *, bool))
#define DIFF_EXEC (void (*)(uint64_t))
#define DIFF_INIT (void (*)(int))
#endif
